package net.joseplay.core.storage.impls;

import net.joseplay.core.Core;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CouplesImpl implements CouplesRepository {

    private final Core core;
    private final Map<UUID, Couple> coupleMap = new ConcurrentHashMap<>();

    public CouplesImpl(Core core) {
        this.core = core;
    }

    private void addToCache(Couple couple) {
        coupleMap.put(couple.getId(), couple);
    }

    private Couple getFromCache(UUID uuid) {
        return coupleMap.get(uuid);
    }

    private boolean hasInCache(UUID uuid) {
        return coupleMap.containsKey(uuid);
    }

    @Override
    public Couple create(UUID partner1, UUID partner2) throws Exception {

        UUID[] partners = normalize(partner1, partner2);

        partner1 = partners[0];
        partner2 = partners[1];

        Optional<Couple> existing = find(partner1, partner2);

        if (existing.isPresent()) {
            return existing.get();
        }

        UUID coupleId = UUID.randomUUID();
        Instant now = Instant.now();

        String sql = """
            INSERT INTO couples
            (id, partner1UUID, partner2UUID, anniversary, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

        try {
            core.dataBase.executeUpdate(
                    sql,
                    coupleId.toString(),
                    partner1.toString(),
                    partner2.toString(),
                    now.toString(),
                    now.toString()
            );
        } catch (Exception exception) {
            /*
             * Another server may have created the couple
             * between find() and INSERT.
             */
            return find(partner1, partner2)
                    .orElseThrow(() -> exception);
        }

        Couple couple = new Couple(
                coupleId,
                partner1,
                partner2,
                now,
                now,
                core.featureRegistry,
                core.featureRepository
        );

        addToCache(couple);

        return couple;
    }

    @Override
    public Optional<Couple> find(UUID partner1, UUID partner2) {

        UUID[] partners = normalize(partner1, partner2);

        String sql = """
            SELECT id, partner1UUID, partner2UUID, anniversary, created_at
            FROM couples
            WHERE partner1UUID = ?
            AND partner2UUID = ?
            LIMIT 1
            """;

        Optional<Couple> couple = core.dataBase.executeQuery(
                sql,
                resultSet -> {

                    if (!resultSet.next()) {
                        return Optional.empty();
                    }

                    UUID id = UUID.fromString(
                            resultSet.getString("id")
                    );

                    Couple cached = getFromCache(id);

                    if (cached != null) {
                        return Optional.of(cached);
                    }

                    return Optional.of(new Couple(
                            id,
                            UUID.fromString(
                                    resultSet.getString("partner1UUID")
                            ),
                            UUID.fromString(
                                    resultSet.getString("partner2UUID")
                            ),
                            Instant.parse(
                                    resultSet.getString("anniversary")
                            ),
                            Instant.parse(
                                    resultSet.getString("created_at")
                            ),
                            core.featureRegistry,
                            core.featureRepository
                    ));
                },
                partners[0].toString(),
                partners[1].toString()
        );

        couple.ifPresent(this::addToCache);

        return couple;
    }

    private UUID[] normalize(UUID partner1, UUID partner2) {
        if (partner1.toString().compareTo(partner2.toString()) <= 0) {
            return new UUID[]{partner1, partner2};
        }

        return new UUID[]{partner2, partner1};
    }
}
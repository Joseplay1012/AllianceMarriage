package net.joseplay.core.storage.impls;

import net.joseplay.core.Core;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import org.bukkit.Bukkit;

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
            core.getDataBase().executeUpdate(
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
                core.getFeatureRegistry(),
                core.getFeatureRepository()
        );

        couple.features().load();

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

        Optional<Couple> couple = core.getDataBase().executeQuery(
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
                            core.getFeatureRegistry(),
                            core.getFeatureRepository()
                    ));
                },
                partners[0].toString(),
                partners[1].toString()
        );

        couple.ifPresent(c -> {
            addToCache(c);

            c.features().load();
        });

        return couple;
    }

    @Override
    public Optional<Couple> findById(UUID id) {
        String sql = """
                SELECT partner1UUID, partner2UUID, anniversary, created_at
                FROM couples
                WHERE id = ?
                LIMIT 1
                """;

        Optional<Couple> op = core.getDataBase().executeQuery(
                sql,
                resultSet -> {
                    if (!resultSet.next()) return Optional.empty();


                    UUID partner1 = UUID.fromString(resultSet.getString("partner1UUID"));
                    UUID partner2 = UUID.fromString(resultSet.getString("partner2UUID"));
                    Instant anniversary = Instant.parse(resultSet.getString("anniversary"));
                    Instant createdAt = Instant.parse(resultSet.getString("created_at"));


                    return Optional.of(new Couple(
                            id,
                            partner1,
                            partner2,
                            anniversary,
                            createdAt,
                            core.getFeatureRegistry(),
                            core.getFeatureRepository()
                    ));
                },
                id.toString()
        );

        op.ifPresent(c -> {
            addToCache(c);

            c.features().load();
        });

        return op;
    }

    @Override
    public Optional<UUID> findPartner(UUID playerUUID) {
        String sql = """
                SELECT partnerUUID
                FROM marriages
                WHERE playerUUID = ?
                """;

        return core.getDataBase().executeQuery(
                sql,
                resultSet -> {
                    if (!resultSet.next()) return Optional.empty();

                    return Optional.of(UUID.fromString(resultSet.getString("partnerUUID")));
                },
                playerUUID.toString()
        );
    }

    @Override
    public Optional<Boolean> maryPlayer(UUID playerUUID, UUID partnerUUID) {

        if (findPartner(playerUUID).isPresent()){
            return Optional.of(Boolean.FALSE);
        }

        if (findPartner(partnerUUID).isPresent()){
            return Optional.of(Boolean.FALSE);
        }

        String sql = core.getDataBase().mysql ? """
                INSERT INTO marriages
                (playerUUID, partnerUUID)
                VALUES(?, ?)
                ON DUPLICATE UPDATE
                partnerUUID = VALUES(partnerUUID)
                """ :
                """
                INSERT INTO marriages
                (playerUUID, partnerUUID)
                VALUES(?, ?)
                ON CONFLICT(playerUUID)
                DO UPDATE SET
                partnerUUID = excluded.partnerUUID
                """;

        Bukkit.getScheduler().runTaskAsynchronously(core.getPlugin(),() -> {
            core.getDataBase().executeUpdate(
                    sql,
                    playerUUID.toString(),
                    partnerUUID.toString()
            );

            core.getDataBase().executeUpdate(
                    sql,
                    partnerUUID.toString(),
                    playerUUID.toString()
            );

            try {
                create(playerUUID, partnerUUID);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return Optional.of(Boolean.TRUE);
    }

    @Override
    public Optional<Boolean> divocePlayer(UUID playerUUID) {

        UUID partner = findPartner(playerUUID).orElse(null);

        if (partner == null){
            return Optional.of(Boolean.FALSE);
        }

        String sql = """
                DELETE FROM marriages
                WHERE playerUUID = ?
                AND partnerUUID = ?
                """;

        Bukkit.getScheduler().runTaskAsynchronously(core.getPlugin(), () -> {
            core.getDataBase().executeUpdate(
                    sql,
                    playerUUID.toString(),
                    partner.toString()
            );

            core.getDataBase().executeUpdate(
                    sql,
                    partner.toString(),
                    playerUUID.toString()
            );
        });

        return Optional.of(Boolean.TRUE);
    }

    private UUID[] normalize(UUID partner1, UUID partner2) {
        if (partner1.toString().compareTo(partner2.toString()) <= 0) {
            return new UUID[]{partner1, partner2};
        }

        return new UUID[]{partner2, partner1};
    }
}
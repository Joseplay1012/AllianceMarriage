package net.joseplay.core.storage.impls;

import net.joseplay.core.Core;
import net.joseplay.core.contexts.MarryContextResult;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class CouplesImpl implements CouplesRepository {
    private final Map<UUID, Couple> coupleMap = new ConcurrentHashMap<>();

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
    public CompletableFuture<Couple> create(UUID partner1, UUID partner2) {
        UUID[] partners = normalize(partner1, partner2);

        partner1 = partners[0];
        partner2 = partners[1];

        UUID finalPartner1 = partner1;
        UUID finalPartner2 = partner2;

        return find(finalPartner1, finalPartner2)
                .thenCompose(existing -> {
                    if (existing.isPresent()) {
                        return CompletableFuture.completedFuture(existing.get());
                    }

                    UUID coupleId = UUID.randomUUID();
                    Instant now = Instant.now();

                    String sql = """
                            INSERT INTO couples
                            (id, partner1UUID, partner2UUID, anniversary, created_at)
                            VALUES (?, ?, ?, ?, ?)
                            """;

                    return Core.getInstance().getInstance().getDataBase().executeUpdateAsync(
                            sql,
                            coupleId.toString(),
                            finalPartner1.toString(),
                            finalPartner2.toString(),
                            now.toString(),
                            now.toString()
                    ).thenCompose(ignored -> {
                        Couple couple = new Couple(
                                coupleId,
                                finalPartner1,
                                finalPartner2,
                                now,
                                now,
                                Core.getInstance().getFeatureRegistry(),
                                Core.getInstance().getFeatureRepository()
                        );

                        return couple.features()
                                .load()
                                .thenApply(ignoredLoad -> {
                                    addToCache(couple);
                                    return couple;
                                });
                    }).exceptionallyCompose(exception ->
                            find(finalPartner1, finalPartner2)
                                    .thenCompose(found -> {
                                        if (found.isPresent()) {
                                            return CompletableFuture.completedFuture(
                                                    found.get()
                                            );
                                        }

                                        CompletableFuture<Couple> failed =
                                                new CompletableFuture<>();

                                        failed.completeExceptionally(exception);
                                        return failed;
                                    })
                    );
                });
    }

    @Override
    public CompletableFuture<Optional<Couple>> find(UUID partner1, UUID partner2) {
        UUID[] partners = normalize(partner1, partner2);

        String sql = """
            SELECT id, partner1UUID, partner2UUID, anniversary, created_at
            FROM couples
            WHERE partner1UUID = ?
            AND partner2UUID = ?
            LIMIT 1
            """;

        return Core.getInstance().getDataBase().<Optional<Couple>>executeQueryAsync(
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

                    Couple couple = new Couple(
                            id,
                            UUID.fromString(resultSet.getString("partner1UUID")),
                            UUID.fromString(resultSet.getString("partner2UUID")),
                            Instant.parse(resultSet.getString("anniversary")),
                            Instant.parse(resultSet.getString("created_at")),
                            Core.getInstance().getFeatureRegistry(),
                            Core.getInstance().getFeatureRepository()
                    );

                    addToCache(couple);

                    return Optional.of(couple);
                },
                partners[0].toString(),
                partners[1].toString()
        ).thenCompose(optional -> {
            if (optional.isEmpty()){
                return CompletableFuture.completedFuture(Optional.empty());
            }

            Couple couple = optional.get();

            return  couple.features()
                    .load()
                    .thenApply(ignore -> Optional.of(couple));

        });
    }

    @Override
    public CompletableFuture<Optional<Couple>> findById(UUID id) {
        String sql = """
                SELECT partner1UUID, partner2UUID, anniversary, created_at
                FROM couples
                WHERE id = ?
                LIMIT 1
                """;

        return Core.getInstance().getDataBase().executeQueryAsync(
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
                            Core.getInstance().getFeatureRegistry(),
                            Core.getInstance().getFeatureRepository()
                    ));
                },
                id.toString()
        );
    }

    @Override
    public CompletableFuture<Optional<UUID>> findPartner(UUID playerUUID) {
        String sql = """
                SELECT partnerUUID
                FROM marriages
                WHERE playerUUID = ?
                """;

        return Core.getInstance().getDataBase().executeQueryAsync(
                sql,
                resultSet -> {
                    if (!resultSet.next()) return Optional.empty();

                    return Optional.of(UUID.fromString(resultSet.getString("partnerUUID")));
                },
                playerUUID.toString()
        );
    }

    @Override
    public CompletableFuture<MarryContextResult> marryPlayer(
            UUID playerUUID,
            UUID partnerUUID
    ) {
        return findPartner(playerUUID)
                .thenCompose(playerMarriage -> {
                    if (playerMarriage.isPresent()) {
                        return CompletableFuture.completedFuture(
                                MarryContextResult.error(
                                        playerUUID,
                                        partnerUUID,
                                        MarryContextResult.MarryErrorType.ALREADY_MARRIED,
                                        "Player is already married."
                                )
                        );
                    }

                    return findPartner(partnerUUID).thenCompose(partnerMarriage -> {
                        if (partnerMarriage.isPresent()) {
                            return CompletableFuture.completedFuture(
                                    MarryContextResult.error(
                                            playerUUID,
                                            partnerUUID,
                                            MarryContextResult.MarryErrorType.PARTNER_ALREADY_MARRIED,
                                            "Partner is already married."
                                    )
                            );
                        }

                        return createMarriage(playerUUID, partnerUUID);
                    });
                });
    }

    @Override
    public CompletableFuture<MarryContextResult> divorcePlayer(UUID playerUUID) {
        return findPartner(playerUUID)
                .thenCompose(partner -> {
                    if (partner.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                MarryContextResult.error(
                                        playerUUID,
                                        null,
                                        MarryContextResult.MarryErrorType.NOT_MARRIED,
                                        "Player is not married."
                                )
                        );
                    }

                    return divorce(playerUUID, partner.get());
                });
    }

    private CompletableFuture<MarryContextResult> createMarriage(
            UUID playerUUID,
            UUID partnerUUID
    ) {
        String sql = Core.getInstance().getDataBase().mysql ? """
            INSERT INTO marriages
            (playerUUID, partnerUUID)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
            partnerUUID = VALUES(partnerUUID)
            """ : """
            INSERT INTO marriages
            (playerUUID, partnerUUID)
            VALUES (?, ?)
            ON CONFLICT(playerUUID)
            DO UPDATE SET
            partnerUUID = excluded.partnerUUID
            """;

        return Core.getInstance().getDataBase().executeUpdateAsync(
                sql,
                playerUUID.toString(),
                partnerUUID.toString()
        ).thenCompose(ignored ->
                Core.getInstance().getDataBase().executeUpdateAsync(
                        sql,
                        partnerUUID.toString(),
                        playerUUID.toString()
                )
        ).thenCompose(ignored ->
                create(playerUUID, partnerUUID)
        ).thenApply(couple -> MarryContextResult.marry(playerUUID, partnerUUID));
    }

    private CompletableFuture<MarryContextResult> divorce(
            UUID playerUUID,
            UUID partnerUUID
    ) {
        String sql = """
            DELETE FROM marriages
            WHERE playerUUID = ?
            AND partnerUUID = ?
            """;

        return Core.getInstance().getDataBase()
                .executeUpdateAsync(
                        sql,
                        playerUUID.toString(),
                        partnerUUID.toString()
                )
                .thenCompose(ignored ->
                        Core.getInstance().getDataBase()
                                .executeUpdateAsync(
                                        sql,
                                        partnerUUID.toString(),
                                        playerUUID.toString()
                                )
                )
                .thenApply(ignored ->
                        MarryContextResult.divorce(playerUUID, partnerUUID)
                );
    }

    private UUID[] normalize(UUID partner1, UUID partner2) {
        if (partner1.toString().compareTo(partner2.toString()) <= 0) {
            return new UUID[]{partner1, partner2};
        }

        return new UUID[]{partner2, partner1};
    }
}
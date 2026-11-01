package net.joseplay.core.storage.impls;

import net.joseplay.core.Core;
import net.joseplay.core.FeatureRegistry;
import net.joseplay.core.contexts.MarryContextResult;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import net.joseplay.core.storage.Database;
import net.joseplay.core.storage.FeatureRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CouplesImpl implements CouplesRepository {
    private final Database database;
    private final FeatureRegistry featureRegistry;
    private final FeatureRepository featureRepository;

    private final Map<UUID, Couple> coupleMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerCoupleMap = new ConcurrentHashMap<>();

    public CouplesImpl(Database database, FeatureRegistry featureRegistry, FeatureRepository featureRepository) {
        this.database = Objects.requireNonNull(database, "database");
        this.featureRegistry = Objects.requireNonNull(featureRegistry, "featureRegistry");
        this.featureRepository = Objects.requireNonNull(featureRepository, "featureRepository");
    }

    public CouplesImpl() {
        this(
                Core.getInstance().getDataBase(),
                Core.getInstance().getFeatureRegistry(),
                Core.getInstance().getFeatureRepository()
        );
    }

    @Override
    public void addToCache(Couple couple) {
        coupleMap.put(couple.getId(), couple);
        playerCoupleMap.put(couple.getPartner1(), couple.getId());
        playerCoupleMap.put(couple.getPartner2(), couple.getId());
    }

    @Override
    public void removeCouple(UUID coupleId) {
        Couple couple = coupleMap.remove(coupleId);
        if (couple == null) {
            return;
        }

        playerCoupleMap.remove(couple.getPartner1());
        playerCoupleMap.remove(couple.getPartner2());
    }

    @Override
    public Couple getCoupleFromCache(UUID uuid) {
        UUID coupleId = playerCoupleMap.get(uuid);
        return coupleId != null ? coupleMap.get(coupleId) : null;
    }

    @Override
    public Couple getFromCache(UUID uuid) {
        return coupleMap.get(uuid);
    }

    @Override
    public boolean hasInCache(UUID uuid) {
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

                    return database.executeUpdateAsync(
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
                                featureRegistry,
                                featureRepository
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

        return database.<Optional<Couple>>executeQueryAsync(
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
                            featureRegistry,
                            featureRepository
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

            return couple.features()
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

        return database.executeQueryAsync(
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
                            featureRegistry,
                            featureRepository
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

        return database.executeQueryAsync(
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
        if (playerUUID.equals(partnerUUID)) {
            return CompletableFuture.completedFuture(
                    MarryContextResult.error(
                            playerUUID,
                            partnerUUID,
                            MarryContextResult.MarryErrorType.NOT_MARRIED,
                            "Player cannot marry themselves."
                    )
            );
        }

        return database.executeTransactionAsync(connection -> {
            // Step 1: Check availability of both players atomically inside transaction
            String checkSql = "SELECT playerUUID FROM marriages WHERE playerUUID = ? OR playerUUID = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, playerUUID.toString());
                checkStmt.setString(2, partnerUUID.toString());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    while (rs.next()) {
                        String marriedPlayer = rs.getString("playerUUID");
                        if (marriedPlayer.equals(playerUUID.toString())) {
                            return MarryContextResult.error(
                                    playerUUID,
                                    partnerUUID,
                                    MarryContextResult.MarryErrorType.ALREADY_MARRIED,
                                    "Player is already married."
                            );
                        } else {
                            return MarryContextResult.error(
                                    playerUUID,
                                    partnerUUID,
                                    MarryContextResult.MarryErrorType.PARTNER_ALREADY_MARRIED,
                                    "Partner is already married."
                            );
                        }
                    }
                }
            }

            // Step 2: Insert into marriages (NO ON CONFLICT/DUPLICATE UPDATE!)
            String insertMarriageSql = "INSERT INTO marriages (playerUUID, partnerUUID) VALUES (?, ?)";
            try (PreparedStatement mStmt = connection.prepareStatement(insertMarriageSql)) {
                mStmt.setString(1, playerUUID.toString());
                mStmt.setString(2, partnerUUID.toString());
                mStmt.executeUpdate();

                mStmt.setString(1, partnerUUID.toString());
                mStmt.setString(2, playerUUID.toString());
                mStmt.executeUpdate();
            }

            // Step 3: Insert into couples
            UUID[] partners = normalize(playerUUID, partnerUUID);
            UUID coupleId = UUID.randomUUID();
            Instant now = Instant.now();

            String insertCoupleSql = """
                    INSERT INTO couples
                    (id, partner1UUID, partner2UUID, anniversary, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement cStmt = connection.prepareStatement(insertCoupleSql)) {
                cStmt.setString(1, coupleId.toString());
                cStmt.setString(2, partners[0].toString());
                cStmt.setString(3, partners[1].toString());
                cStmt.setString(4, now.toString());
                cStmt.setString(5, now.toString());
                cStmt.executeUpdate();
            }

            Couple couple = new Couple(
                    coupleId,
                    partners[0],
                    partners[1],
                    now,
                    now,
                    featureRegistry,
                    featureRepository
            );
            addToCache(couple);

            return MarryContextResult.marry(playerUUID, partnerUUID);
        }).exceptionally(ex -> MarryContextResult.error(
                playerUUID,
                partnerUUID,
                MarryContextResult.MarryErrorType.ALREADY_MARRIED,
                "Failed to marry: " + ex.getMessage()
        ));
    }

    @Override
    public CompletableFuture<MarryContextResult> divorcePlayer(UUID playerUUID) {
        return database.executeTransactionAsync(connection -> {
            String findPartnerSql = "SELECT partnerUUID FROM marriages WHERE playerUUID = ?";
            UUID partnerUUID;
            try (PreparedStatement stmt = connection.prepareStatement(findPartnerSql)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return MarryContextResult.error(
                                playerUUID,
                                null,
                                MarryContextResult.MarryErrorType.NOT_MARRIED,
                                "Player is not married."
                        );
                    }
                    partnerUUID = UUID.fromString(rs.getString("partnerUUID"));
                }
            }

            String deleteMarriagesSql = "DELETE FROM marriages WHERE playerUUID = ? OR playerUUID = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteMarriagesSql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, partnerUUID.toString());
                stmt.executeUpdate();
            }

            return MarryContextResult.divorce(playerUUID, partnerUUID);
        }).thenApply(result -> {
            if (!result.isError() && result.getPartner2UUID() != null) {
                find(playerUUID, result.getPartner2UUID()).thenAccept(optCouple -> {
                    optCouple.ifPresent(c -> removeCouple(c.getId()));
                });
            }
            return result;
        });
    }

    private UUID[] normalize(UUID partner1, UUID partner2) {
        if (partner1.toString().compareTo(partner2.toString()) <= 0) {
            return new UUID[]{partner1, partner2};
        }

        return new UUID[]{partner2, partner1};
    }
}

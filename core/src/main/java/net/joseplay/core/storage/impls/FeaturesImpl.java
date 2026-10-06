package net.joseplay.core.storage.impls;

import net.joseplay.core.Core;
import net.joseplay.core.feature.StoredFeature;
import net.joseplay.core.storage.Database;
import net.joseplay.core.storage.FeatureRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FeaturesImpl implements FeatureRepository {
    private final Database dataBase;
    private final JavaPlugin plugin;
    private static final Set<String> NUMBER_TYPES = Set.of(
            "INTEGER",
            "DOUBLE",
            "LONG"
    );

    public FeaturesImpl(Database dataBase, JavaPlugin plugin) {
        this.dataBase = dataBase;
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> set(String coupleId, String feature, String type, String value) {
        String sql = dataBase.mysql ? """
                INSERT INTO couples_features
                        (couple_id, feature, type, value)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                        type = VALUES(type),
                        value = VALUES(value)
                """ :
                """
                                INSERT INTO couples_features
                                (couple_id, feature, type, value)
                                VALUES (?, ?, ?, ?)
                                ON CONFLICT(couple_id, feature)
                                DO UPDATE SET
                                type = excluded.type,
                                value = excluded.value
                        """;

        return dataBase.executeUpdateAsync(
                sql,
                coupleId,
                feature,
                type,
                value
        );
    }

    @Override
    public CompletableFuture<String> increment(
            String coupleId,
            String feature,
            String type,
            String amount
    ) {
        String normalizedType = type.toUpperCase(Locale.ROOT);

        String sqlType = switch (normalizedType) {
            case "INTEGER", "LONG" -> dataBase.mysql ? "SIGNED" : "INTEGER";
            case "DOUBLE" -> dataBase.mysql ? "DOUBLE" : "REAL";
            default -> throw new IllegalArgumentException(
                    "Unsupported numeric feature type: " + type
            );
        };

        if (!NUMBER_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException(
                    "Feature type '" + type + "' does not support increment."
            );
        }

        validateNumber(normalizedType, amount);

        String sql = dataBase.mysql ? """
                INSERT INTO couples_features
                (couple_id, feature, type, value)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                type = VALUES(type),
                value = CAST(couples_features.value AS %s)
                      + CAST(VALUES(value) AS %s)
                """.formatted(sqlType, sqlType) : """
                INSERT INTO couples_features
                (couple_id, feature, type, value)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(couple_id, feature)
                DO UPDATE SET
                type = excluded.type,
                value = CAST(couples_features.value AS %s)
                      + CAST(excluded.value AS %s)
                """.formatted(sqlType, sqlType);

        return dataBase.executeUpdateAsync(
                sql,
                coupleId,
                feature,
                normalizedType,
                amount
        ).thenCompose(ignored ->
                dataBase.executeQueryAsync(
                        """
                                SELECT value
                                FROM couples_features
                                WHERE couple_id = ?
                                AND feature = ?
                                """,
                        resultSet -> {
                            if (!resultSet.next()) {
                                throw new IllegalStateException(
                                        "Feature '" + feature
                                                + "' was not found after increment."
                                );
                            }

                            return resultSet.getString("value");
                        },
                        coupleId,
                        feature
                )
        );
    }

    @Override
    public CompletableFuture<Void> delete(String coupleId, String feature) {
        String sql = """
                DELETE FROM couples_features
                WHERE couple_id = ?
                AND feature = ?
                """;
        return CompletableFuture.runAsync(() -> {
            dataBase.executeUpdate(
                    sql,
                    coupleId,
                    feature
            );
        });
    }

    @Override
    public CompletableFuture<List<StoredFeature>> load(String coupleId) {
        String sql = """
                SELECT feature, type, value
                FROM couples_features
                WHERE couple_id = ?
                """;

        return dataBase.executeQueryAsync(
                sql,
                resultSet -> {
                    List<StoredFeature> features = new ArrayList<>();
                    while (resultSet.next()) {
                        StoredFeature storedFeature = new StoredFeature(
                                resultSet.getString("feature"),
                                resultSet.getString("type"),
                                resultSet.getString("value")
                        );

                        features.add(storedFeature);
                    }

                    return features;
                },
                coupleId
        );
    }


    private void validateNumber(String type, String value) {
        try {
            switch (type) {
                case "INTEGER" -> Integer.parseInt(value);
                case "LONG" -> Long.parseLong(value);
                case "DOUBLE" -> {
                    double number = Double.parseDouble(value);

                    if (!Double.isFinite(number)) {
                        throw new NumberFormatException(
                                "Value must be finite."
                        );
                    }
                }
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid " + type + " value: " + value,
                    exception
            );
        }
    }
}

package net.joseplay.core.storage.impls;

import net.joseplay.core.feature.StoredFeature;
import net.joseplay.core.storage.Database;
import net.joseplay.core.storage.FeatureRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class FeaturesImpl implements FeatureRepository {
    private final Database dataBase;
    private final JavaPlugin plugin;
    private final List<String> allowNumbers = List.of(
            "INTEGER",
            "DOUBLE",
            "LONG"
    );

    public FeaturesImpl(Database dataBase, JavaPlugin plugin) {
        this.dataBase = dataBase;
        this.plugin = plugin;
    }

    @Override
    public void set(String coupleId, String feature, String type, String value) {
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

        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> dataBase.executeUpdate(
                sql,
                coupleId,
                feature,
                type,
                value
        ));
    }

    @Override
    public void increment(String coupleId, String feature, String type, String value) {

        if (!allowNumbers.contains(type.toUpperCase())) return;

        String sql = dataBase.mysql ? """
                INSERT INTO couples_features
                                  (couple_id, feature, type, value)
                                  VALUES (?, ?, ?, ?)
                                  ON DUPLICATE KEY UPDATE
                                  type = VALUES(type),
                                  value = couples_features.value + VALUES(value)
                """ :
                """
                        INSERT INTO couples_features
                        (couple_id, feature, type, value)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(couple_id, feature)
                        DO UPDATE SET
                        type = excluded.type,
                        value = couples_features.value + excluded.value
                """;

        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> dataBase.executeUpdate(
                sql,
                coupleId,
                feature,
                type,
                value
        ));
    }

    @Override
    public void delete(String coupleId, String feature) {
        String sql = """
                DELETE FROM couples_features
                WHERE couple_id = ?
                AND feature = ?
                """;

        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> dataBase.executeUpdate(
                sql,
                coupleId,
                feature
        ));
    }

    @Override
    public List<StoredFeature> load(String coupleId) {
        String sql = """
                SELECT feature, type, value
                FROM couples_features
                WHERE couple_id = ?
                """;

        return dataBase.executeQuery(
                sql,
                resultSet -> {
                    List<StoredFeature> sList = new ArrayList<>();
                    while (resultSet.next()){
                        StoredFeature storedFeature = new StoredFeature(
                                resultSet.getString("feature"),
                                resultSet.getString("type"),
                                resultSet.getString("value")
                        );

                        sList.add(storedFeature);
                    }

                    return sList;
                },
                coupleId
        );
    }
}

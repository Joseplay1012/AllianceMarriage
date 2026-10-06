package net.joseplay.test.storage.impls;

import net.joseplay.test.feature.StoredFeature;
import net.joseplay.test.storage.Database;
import net.joseplay.test.storage.FeatureRepository;

import java.util.ArrayList;
import java.util.List;

public class FeaturesImpl implements FeatureRepository {
    private final Database dataBase;

    public FeaturesImpl(Database dataBase) {
        this.dataBase = dataBase;
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

        dataBase.executeUpdate(
                sql,
                coupleId,
                feature,
                type,
                value
        );
    }

    @Override
    public void delete(String coupleId, String feature) {
        String sql = """
                DELETE FROM couples_features
                WHERE couple_id = ?
                AND feature = ?
                """;
        dataBase.executeUpdate(
                sql,
                coupleId,
                feature
        );
    }

    @Override
    public List<StoredFeature> load(String coupleId) {
        String sql = """
                SELECT feature, type, value
                FROM couples_features
                WHERE couple_id = ?
                """;

        List<StoredFeature> storedFeatures = dataBase.executeQuery(
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

        return storedFeatures;
    }
}

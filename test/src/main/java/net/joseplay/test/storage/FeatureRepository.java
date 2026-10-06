package net.joseplay.test.storage;

import net.joseplay.test.feature.StoredFeature;

import java.util.List;

public interface FeatureRepository {

    void set(
            String coupleId,
            String feature,
            String type,
            String value
    );

    void delete(
            String coupleId,
            String feature
    );

    List<StoredFeature> load(String coupleId);
}

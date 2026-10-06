package net.joseplay.core.storage;

import net.joseplay.core.feature.StoredFeature;

import java.util.List;

public interface FeatureRepository {

    void set(
            String coupleId,
            String feature,
            String type,
            String value
    );

    void increment(
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

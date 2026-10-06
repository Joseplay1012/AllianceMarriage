package net.joseplay.test.feature;

import net.joseplay.test.feature.Feature;

import java.util.*;

public final class FeatureRegistry {

    private final Map<String, Feature<?>> features = new HashMap<>();

    public void register(Feature<?> feature) {
        Objects.requireNonNull(feature, "feature");

        String id = feature.getId();

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Feature ID cannot be empty");
        }

        if (features.putIfAbsent(id, feature) != null) {
            throw new IllegalArgumentException(
                    "Feature already registered: " + id
            );
        }
    }

    public void unregister(String id) {
        features.remove(id);
    }

    public boolean isRegistered(String id) {
        return features.containsKey(id);
    }

    public Feature<?> get(String id) {
        return features.get(id);
    }

    public Collection<Feature<?>> all() {
        return Collections.unmodifiableCollection(features.values());
    }
}

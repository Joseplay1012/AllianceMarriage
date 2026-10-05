package net.joseplay.core.feature;

import net.joseplay.core.storage.FeatureRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class FeatureManager {

    private final FeatureRegistry registry;
    private final FeatureRepository repository;
    private final String coupleId;

    private final Map<String, Object> values = new HashMap<>();

    public FeatureManager(
            FeatureRegistry registry,
            FeatureRepository repository,
            String coupleId
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.coupleId = Objects.requireNonNull(coupleId, "coupleId");
    }

    public <T> T get(String id) {
        Feature<T> feature = getFeature(id);

        Object value = values.get(id);

        if (value == null) {
            return feature.getDefaultValue();
        }

        return feature.cast(value);
    }

    public <T> void set(String id, T value) {
        Feature<T> feature = getFeature(id);

        if (value == null) {
            remove(id);
            return;
        }

        feature.validate(value);

        repository.set(
                coupleId,
                feature.getId(),
                feature.getType(),
                feature.serialize(value)
        );

        values.put(id, value);
    }

    public void remove(String id) {
        getFeature(id);

        repository.delete(coupleId, id);
        values.remove(id);
    }

    public boolean has(String id) {
        return values.containsKey(id);
    }

    public void load() {
        values.clear();

        for (StoredFeature stored : repository.load(coupleId)) {
            Feature<?> feature = registry.get(stored.feature());

            if (feature == null) {
                continue;
            }

            Object value = feature.deserialize(stored.value());

            if (value != null) {
                values.put(stored.feature(), value);
            }
        }
    }

    public Map<String, Object> values() {
        return Collections.unmodifiableMap(values);
    }

    @SuppressWarnings("unchecked")
    private <T> Feature<T> getFeature(String id) {
        Feature<?> feature = registry.get(id);

        if (feature == null) {
            throw new IllegalArgumentException(
                    "Unknown feature: " + id
            );
        }

        return (Feature<T>) feature;
    }
}

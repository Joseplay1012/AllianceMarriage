package net.joseplay.core.feature;

import net.joseplay.core.FeatureRegistry;
import net.joseplay.core.storage.FeatureRepository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class FeatureManager {

    private final FeatureRegistry registry;
    private final FeatureRepository repository;
    private final String coupleId;

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<?>> featureChains = new ConcurrentHashMap<>();

    public FeatureManager(
            FeatureRegistry registry,
            FeatureRepository repository,
            String coupleId
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.coupleId = Objects.requireNonNull(coupleId, "coupleId");
    }

    private <R> CompletableFuture<R> chainFeatureOp(String featureId, Supplier<CompletableFuture<R>> op) {
        CompletableFuture<R> resultFuture = new CompletableFuture<>();
        featureChains.compute(featureId, (key, prev) -> {
            CompletableFuture<?> base = (prev == null) ? CompletableFuture.completedFuture(null) : prev;
            CompletableFuture<R> next = base.handle((v, err) -> null)
                    .thenCompose(v -> op.get());
            next.whenComplete((res, ex) -> {
                if (ex != null) {
                    resultFuture.completeExceptionally(ex);
                } else {
                    resultFuture.complete(res);
                }
            });
            return next;
        });
        return resultFuture;
    }

    /**
     * Gets the feature value. If the couple does not have a value stored yet,
     * returns the feature's default value.
     */
    public <T> T get(String id) {
        Feature<T> feature = getFeature(id);

        Object value = values.get(id);

        if (value == null) {
            return feature.getDefaultValue();
        }

        return feature.cast(value);
    }

    /**
     * Persists and updates the feature value in cache.
     * If the value is null, delegates to {@link #remove(String)}.
     */
    public <T> CompletableFuture<Void> set(String id, T value) {
        Feature<T> feature = getFeature(id);

        if (value == null) {
            return remove(id);
        }

        feature.validate(value);

        return chainFeatureOp(id, () ->
                repository.set(
                        coupleId,
                        feature.getId(),
                        feature.getType(),
                        feature.serialize(value)
                ).thenRun(() -> values.put(id, value))
        );
    }

    /**
     * Deletes the feature from persistence and cache.
     */
    public CompletableFuture<Void> remove(String id) {
        getFeature(id);

        return chainFeatureOp(id, () ->
                repository.delete(coupleId, id).thenRun(() -> values.remove(id))
        );
    }

    /**
     * Atomically increments the feature value in the database and updates cache.
     * Consecutive operations on the same feature are chained to prevent lost updates.
     */
    public <T> CompletableFuture<T> increment(String id, T value) {
        Feature<T> feature = getFeature(id);

        feature.validate(value);

        return chainFeatureOp(id, () ->
                repository.increment(
                        coupleId,
                        feature.getId(),
                        feature.getType(),
                        feature.serialize(value)
                ).thenApply(result -> {
                    T newValue = feature.deserialize(result);
                    values.put(id, newValue);
                    return newValue;
                })
        );
    }

    /**
     * Conditionally decrements the feature in the database only if the current value >= amount.
     * Returns true if debited, false if insufficient balance.
     */
    public CompletableFuture<Boolean> decrementIfSufficient(String id, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (amount == 0) {
            return CompletableFuture.completedFuture(true);
        }
        getFeature(id);

        return chainFeatureOp(id, () ->
                repository.decrementIfSufficient(coupleId, id, amount)
                        .thenApply(success -> {
                            if (success) {
                                values.compute(id, (k, oldVal) -> {
                                    int current = (oldVal instanceof Integer i) ? i : 0;
                                    return current - amount;
                                });
                            }
                            return success;
                        })
        );
    }

    /**
     * Checks if this feature is registered in the FeatureRegistry.
     */
    public boolean isRegistered(String id) {
        return registry.isRegistered(id);
    }

    /**
     * Checks if this feature currently has an explicit value stored in memory for this couple.
     */
    public boolean hasStored(String id) {
        return values.containsKey(id);
    }

    /**
     * Checks if this feature currently has an explicit value stored in memory for this couple.
     * To check if a feature is known to the registry, use {@link #isRegistered(String)}.
     */
    public boolean has(String id) {
        return hasStored(id);
    }

    /**
     * Loads features from the repository into cache.
     * In-flight modifications are preserved and not overwritten.
     */
    public CompletableFuture<Void> load() {
        return repository.load(coupleId).thenAccept(features -> {
            for (StoredFeature stored : features) {
                Feature<?> feature = registry.get(stored.feature());

                if (feature == null) {
                    continue;
                }

                Object value = feature.deserialize(stored.value());

                if (value != null) {
                    values.putIfAbsent(stored.feature(), value);
                }
            }
        });
    }

    /**
     * Returns an unmodifiable snapshot copy of current stored values.
     */
    public Map<String, Object> values() {
        return Map.copyOf(values);
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

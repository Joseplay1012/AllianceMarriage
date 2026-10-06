package net.joseplay.core.storage;

import net.joseplay.core.feature.StoredFeature;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FeatureRepository {

    CompletableFuture<Void> set(
            String coupleId,
            String feature,
            String type,
            String value
    );

    CompletableFuture<String> increment(String coupleId, String feature, String type, String amount);

    CompletableFuture<Void> delete(
            String coupleId,
            String feature
    );

    CompletableFuture<List<StoredFeature>> load(String coupleId);
}

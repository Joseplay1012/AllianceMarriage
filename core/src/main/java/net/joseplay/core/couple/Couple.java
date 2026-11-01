package net.joseplay.core.couple;

import net.joseplay.core.feature.FeatureManager;
import net.joseplay.core.FeatureRegistry;
import net.joseplay.core.storage.FeatureRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class Couple {

    private final UUID id;
    private final UUID partner1;
    private final UUID partner2;

    private final Instant anniversary;
    private final Instant createdAt;

    private final FeatureManager features;

    public Couple(
            UUID id,
            UUID partner1,
            UUID partner2,
            Instant anniversary,
            Instant createdAt,
            FeatureRegistry featureRegistry,
            FeatureRepository featureRepository
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.partner1 = Objects.requireNonNull(partner1, "partner1");
        this.partner2 = Objects.requireNonNull(partner2, "partner2");
        this.anniversary = anniversary;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");

        this.features = new FeatureManager(
                featureRegistry,
                featureRepository,
                id.toString()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getPartner1() {
        return partner1;
    }

    public UUID getPartner2() {
        return partner2;
    }

    public Instant getAnniversary() {
        return anniversary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public FeatureManager features() {
        return features;
    }

    public CompletableFuture<Integer> addHeartsWithChance(int points, double chance) {
        if (points <= 0) {
            return CompletableFuture.completedFuture(0);
        }

        if (features == null || !features.isRegistered("hearts")) {
            return CompletableFuture.completedFuture(0);
        }

        if (ThreadLocalRandom.current().nextDouble() >= chance / 100.0) {
            return CompletableFuture.completedFuture(0);
        }

        return features.increment("hearts", points);
    }

    public boolean isPartner(UUID target){
        return partner1.equals(target)
                || partner2.equals(target);
    }
}

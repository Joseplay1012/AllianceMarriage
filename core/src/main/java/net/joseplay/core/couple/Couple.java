package net.joseplay.core.couple;

import net.joseplay.core.feature.FeatureManager;
import net.joseplay.core.FeatureRegistry;
import net.joseplay.core.storage.FeatureRepository;

import java.time.Instant;
import java.util.UUID;

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
        this.id = id;
        this.partner1 = partner1;
        this.partner2 = partner2;
        this.anniversary = anniversary;
        this.createdAt = createdAt;

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
}

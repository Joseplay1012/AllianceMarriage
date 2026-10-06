package net.joseplay.core.storage;

import net.joseplay.core.couple.Couple;

import java.util.Optional;
import java.util.UUID;

public interface CouplesRepository {
    Couple create(UUID partner1, UUID partner2) throws Exception;

    Optional<Couple> find(UUID partner1, UUID partner2);

    Optional<Couple> findById(UUID id);

    /**
     * find partnet of player
     */
    Optional<UUID> findPartner(UUID playerUUID);

    Optional<Boolean> maryPlayer(UUID playerUUID, UUID partnerUUID);

    Optional<Boolean> divocePlayer(UUID playerUUID);


}

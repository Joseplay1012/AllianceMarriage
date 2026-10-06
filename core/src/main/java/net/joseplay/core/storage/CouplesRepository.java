package net.joseplay.core.storage;

import net.joseplay.core.contexts.MarryContextResult;
import net.joseplay.core.couple.Couple;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CouplesRepository {
    CompletableFuture<Couple> create(UUID partner1, UUID partner2) throws Exception;

    CompletableFuture<Optional<Couple>> find(UUID partner1, UUID partner2);

    CompletableFuture<Optional<Couple>> findById(UUID id);

    /**
     * find partnet of player
     */
    CompletableFuture<Optional<UUID>> findPartner(UUID playerUUID);

    CompletableFuture<MarryContextResult> marryPlayer(UUID playerUUID, UUID partnerUUID);

    CompletableFuture<MarryContextResult> divorcePlayer(UUID playerUUID);


}

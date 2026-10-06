package net.joseplay.test.storage;

import java.util.UUID;

public class MarriagePlayer {
    private final UUID playerUUID;
    private UUID partnerUUID;

    public MarriagePlayer(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
}

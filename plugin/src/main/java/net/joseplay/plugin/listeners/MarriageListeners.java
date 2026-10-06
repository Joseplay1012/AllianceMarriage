package net.joseplay.plugin.listeners;

import net.joseplay.core.Core;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MarriageListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        Core.getInstance()
                .getCouplesRepository()
                .findPartner(playerUUID)
                .thenCompose(partner -> {
                    if (partner.isEmpty()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }

                    return Core.getInstance()
                            .getCouplesRepository()
                            .find(playerUUID, partner.get());
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

}

package net.joseplay.plugin.listeners;

import net.joseplay.core.Core;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.impls.CouplesImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MarriageListeners implements Listener {

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player damager && e.getEntity() instanceof Player target) {

            Couple couple = ((CouplesImpl) Core.getInstance().getCouplesRepository())
                    .getCoupleFromCache(damager.getUniqueId());


            if (couple != null) {

                boolean isPartner = couple.getPartner1().equals(target.getUniqueId())
                        || couple.getPartner2().equals(target.getUniqueId());

                if (isPartner && !(boolean) couple.features().get("pvp")){
                    damager.sendMessage("§cPvP has inactive for couple");
                    e.setCancelled(true);
                }
            }
        }

    }

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

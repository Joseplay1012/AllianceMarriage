package net.joseplay.plugin.listeners;

import net.joseplay.core.Core;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.Plugin;
import net.joseplay.plugin.config.ActivitiesConfig;
import net.joseplay.plugin.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MarriageListeners implements Listener {

    private final Messages messages;
    private final ActivitiesConfig activitiesConfig;

    public MarriageListeners(Messages messages, ActivitiesConfig activitiesConfig) {
        this.messages = messages;
        this.activitiesConfig = activitiesConfig;
    }

    private void addHearts(Couple couple, String type) {
        if (couple == null || !arePartnersNearby(couple)) return;

        ActivitiesConfig.ActivityResult activityResult = activitiesConfig.get(type);
        if (activityResult == null) return;

        if (Plugin.getInstance().getCoolDownManager()
                .isOnCooldown(couple.getId(), type)) return;

        couple.addHeartsWithChance(activityResult.points(), activityResult.canche())
                .thenAccept(hearts -> {
                    if (hearts == null || hearts <= 0) {
                        return;
                    }

                    Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
                        Plugin.getInstance()
                                .getCoolDownManager()
                                .setCooldown(couple.getId(), type, activityResult.cooldown());

                        if (Core.getInstance().getPluginSettings().isHeartsReviceMessage()) {
                            List<UUID> players = List.of(
                                    couple.getPartner1(),
                                    couple.getPartner2()
                            );

                            for (UUID uuid : players) {
                                Player player = Bukkit.getPlayer(uuid);

                                if (player != null && player.isOnline()) {
                                    messages.send(player, "marriage.couple-win-hearts", "%hearts%", String.valueOf(hearts));
                                }
                            }
                        }
                    });
                });
    }

    private Couple getCouple(UUID player) {
        return Core.getInstance().getCouplesRepository().getCoupleFromCache(player);
    }

    private boolean arePartnersNearby(Couple couple) {
        if (couple == null) return false;

        Player partner1 = Bukkit.getPlayer(couple.getPartner1());
        Player partner2 = Bukkit.getPlayer(couple.getPartner2());

        if (partner1 == null || partner2 == null) return false;

        if (!partner1.getWorld().equals(partner2.getWorld())) return false;

        double max = activitiesConfig.getMaxDistance();

        return partner1.getLocation().distanceSquared(partner2.getLocation())
                <= max * max;
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player damager && e.getEntity() instanceof Player target) {
            Couple couple = getCouple(damager.getUniqueId());

            if (couple != null) {
                if (couple.isPartner(target.getUniqueId()) && !(boolean) couple.features().get("pvp")) {
                    messages.send(damager, "pvp.disabled");
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
                    Plugin.getInstance().getLogger().warning("Failed to preload couple data for " + playerUUID + ": " + throwable.getMessage());
                    return null;
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;

        Couple couple = getCouple(event.getPlayer().getUniqueId());
        if (couple == null) return;

        if (!couple.isPartner(target.getUniqueId())) return;

        addHearts(couple, "interact");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "eat");
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Couple couple = getCouple(player.getUniqueId());
        addHearts(couple, "craft");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (!(event.getInventory().getHolder() instanceof org.bukkit.block.Chest
                || event.getInventory().getHolder() instanceof org.bukkit.block.DoubleChest)) return;

        Couple couple = getCouple(player.getUniqueId());
        addHearts(couple, "chest");
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "cooking");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Couple couple = getCouple(event.getEnchanter().getUniqueId());
        addHearts(couple, "enchant");
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        Couple couple = getCouple(player.getUniqueId());
        addHearts(couple, "anvil");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Couple couple = getCouple(killer.getUniqueId());
        if (couple == null) return;

        if (event.getEntity() instanceof Player) {
            addHearts(couple, "playerkill");
            return;
        }

        if (event.getEntity() instanceof Boss) {
            addHearts(couple, "bosskill");
            return;
        }

        addHearts(couple, "deathentity");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        Couple couple = getCouple(damager.getUniqueId());
        if (couple == null) return;

        if (!couple.isPartner(damaged.getUniqueId())) return;

        addHearts(couple, "protect");
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaidFinish(RaidFinishEvent event) {
        Set<UUID> couplesFilled = new HashSet<>();
        for (Player player : event.getWinners()) {
            Couple couple = getCouple(player.getUniqueId());
            if (couple == null) continue;

            if (couplesFilled.contains(couple.getId())) continue;

            addHearts(couple, "raid");
            couplesFilled.add(couple.getId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (event.getAdvancement() == null || event.getAdvancement().getKey().getKey().startsWith("recipes/")) {
            return;
        }

        Couple couple = getCouple(event.getPlayer().getUniqueId());
        if (couple == null) return;

        addHearts(couple, "advancement");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Player player)) return;

        Couple couple = getCouple(player.getUniqueId());
        if (couple == null) return;

        addHearts(couple, "portal");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;

        Couple couple = getCouple(player.getUniqueId());
        addHearts(couple, "breed");
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "harvest");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlant(BlockPlaceEvent event) {
        Material material = event.getBlockPlaced().getType();

        if (!material.name().endsWith("_SEEDS")
                && !material.name().endsWith("_SAPLING")) return;

        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "plant");
    }

    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "harvest");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();

        Couple couple = getCouple(event.getPlayer().getUniqueId());

        if (material.name().endsWith("_LOG")
                || material.name().endsWith("_WOOD")
                || material.name().endsWith("_STEM")
                || material.name().endsWith("_HYPHAE")) {
            addHearts(couple, "woodcutting");
            return;
        }

        if (material.isBlock()) {
            addHearts(couple, "mining");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "placeblock");
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "fishing");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Couple couple = getCouple(event.getPlayer().getUniqueId());
        if (couple == null) return;

        switch (event.getPlayer().getWorld().getEnvironment()) {
            case NETHER -> addHearts(couple, "nether");
            case THE_END -> addHearts(couple, "theend");
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalTravel(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                && event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) return;

        Couple couple = getCouple(event.getPlayer().getUniqueId());
        addHearts(couple, "portaltravel");
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;

        Couple couple = getCouple(player.getUniqueId());
        if (couple == null) return;

        switch (event.getVehicle().getType()) {
            case MINECART, CHEST_MINECART, FURNACE_MINECART -> addHearts(couple, "minecart");
            case HORSE, DONKEY, MULE -> addHearts(couple, "horse");
            default -> {
            }
        }

        if (event.getVehicle() instanceof Boat) {
            addHearts(couple, "boat");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

        Couple couple = getCouple(event.getPlayer().getUniqueId());
        if (couple == null) return;

        addHearts(couple, "sleep");
    }
}

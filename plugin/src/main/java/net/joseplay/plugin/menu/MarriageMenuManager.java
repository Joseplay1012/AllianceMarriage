package net.joseplay.plugin.menu;

import net.joseplay.api.menu.MenuManager;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.shop.HeartsShop;
import net.joseplay.plugin.shop.HeartsShopService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class MarriageMenuManager {
    private final Plugin plugin;
    private final MenuManager menuManager;
    private final MarriageService marriageService;
    private final HeartsShop heartsShop;
    private final HeartsShopService heartsShopService;

    public MarriageMenuManager(
            Plugin plugin,
            MenuManager menuManager,
            MarriageService marriageService,
            HeartsShop heartsShop,
            HeartsShopService heartsShopService
    ) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.marriageService = marriageService;
        this.heartsShop = heartsShop;
        this.heartsShopService = heartsShopService;

        Bukkit.getPluginManager().registerEvents(menuManager, plugin);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public MarriageService getMarriageService() {
        return marriageService;
    }

    public HeartsShop getHeartsShop() {
        return heartsShop;
    }

    public HeartsShopService getHeartsShopService() {
        return heartsShopService;
    }

    public void openMarriageMenu(Player player) {
        if (player == null) return;

        marriageService.getCouple(player.getUniqueId()).thenAccept(optionalCouple -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                MarriageMenu menu = new MarriageMenu(
                        player,
                        optionalCouple.orElse(null),
                        marriageService,
                        this
                );
                menuManager.open(player, menu);
            });
        });
    }

    public void openMarriageRequestMenu(Player player, UUID requesterUuid) {
        if (player == null || requesterUuid == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            MarriageRequestMenu menu = new MarriageRequestMenu(
                    player,
                    requesterUuid,
                    marriageService
            );
            menuManager.open(player, menu);
        });
    }

    public void openHeartsShopMenu(Player player) {
        if (player == null) return;

        marriageService.getCouple(player.getUniqueId()).thenAccept(optionalCouple -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                HeartsShopMenu menu = new HeartsShopMenu(
                        player,
                        optionalCouple.orElse(null),
                        heartsShop,
                        heartsShopService,
                        this
                );
                menuManager.open(player, menu);
            });
        });
    }


}

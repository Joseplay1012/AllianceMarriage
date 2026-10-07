package net.joseplay.plugin;

import net.joseplay.api.menu.MenuManager;
import net.joseplay.core.Core;
import net.joseplay.plugin.commands.MarriageCommands;
import net.joseplay.plugin.listeners.MarriageListeners;
import net.joseplay.plugin.menu.MarriageMenuManager;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.shop.HeartShopLayout;
import net.joseplay.plugin.shop.HeartsShop;
import net.joseplay.plugin.shop.HeartsShopLoader;
import net.joseplay.plugin.shop.HeartsShopService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
    private Core core;
    private MarriageService marriageService;
    private HeartsShop heartsShop;
    private HeartsShopService heartsShopService;
    private MarriageMenuManager marriageMenuManager;

    @Override
    public void onEnable() {
        getLogger().info("Starting plugin...");

        core = new Core(this);
        core.onEnable();

        getLogger().info("Initializing services...");
        marriageService = new MarriageService(this);

        getLogger().info("Loading Hearts shop...");
        HeartsShopLoader loader = new HeartsShopLoader(this);
        heartsShop = new HeartsShop(loader, new HeartShopLayout());
        heartsShopService = new HeartsShopService(this, marriageService);

        getLogger().info("Initializing menu system...");
        MenuManager menuManager = new MenuManager();
        marriageMenuManager = new MarriageMenuManager(
                this,
                menuManager,
                marriageService,
                heartsShop,
                heartsShopService
        );

        getLogger().info("Registering commands...");
        if (getCommand("marriage") == null) {
            getLogger().severe("Failed to register /marriage command. Check plugin.yml.");
            return;
        }

        MarriageCommands marriageCommands = new MarriageCommands(
                marriageMenuManager,
                marriageService,
                heartsShop
        );
        getCommand("marriage").setExecutor(marriageCommands);
        getCommand("marriage").setTabCompleter(marriageCommands);
        getLogger().info("Registered command: /marriage");

        getLogger().info("Registering listeners...");
        Bukkit.getPluginManager().registerEvents(new MarriageListeners(), this);

        getLogger().info("Plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling plugin...");

        if (core != null) {
            core.onDisable();
        }


        if (marriageMenuManager != null && marriageMenuManager.getMenuManager() != null) {
            marriageMenuManager.getMenuManager().shutdown();
        }


        getLogger().info("Plugin disabled successfully.");
    }

    public Core getCore() {
        return core;
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

    public MarriageMenuManager getMarriageMenuManager() {
        return marriageMenuManager;
    }
}

package net.joseplay.plugin;

import net.joseplay.api.cooldown.CooldownManager;
import net.joseplay.api.menu.MenuManager;
import net.joseplay.api.preferences.PreferencesManager;
import net.joseplay.core.Core;
import net.joseplay.core.config.ConfigManager;
import net.joseplay.core.config.PluginSettings;
import net.joseplay.plugin.commands.MarriageCommands;
import net.joseplay.plugin.config.ActivitiesConfig;
import net.joseplay.plugin.config.MenuConfig;
import net.joseplay.plugin.config.Messages;
import net.joseplay.plugin.listeners.MarriageListeners;
import net.joseplay.plugin.menu.MarriageMenuManager;
import net.joseplay.plugin.preferences.PVPPrefenrence;
import net.joseplay.plugin.preferences.PointsPreference;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.shop.ConfiguredHeartShopLayout;
import net.joseplay.plugin.shop.HeartsShop;
import net.joseplay.plugin.shop.HeartsShopLoader;
import net.joseplay.plugin.shop.HeartsShopService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
    private static Plugin instance;
    private Core core;
    private ConfigManager configManager;
    private PluginSettings pluginSettings;
    private Messages messages;
    private ActivitiesConfig activitiesConfig;
    private MenuConfig menuConfig;
    private MarriageService marriageService;
    private HeartsShop heartsShop;
    private HeartsShopService heartsShopService;
    private MarriageMenuManager marriageMenuManager;
    private PreferencesManager preferencesManager;
    private final CooldownManager coolDownManager = new CooldownManager();

    @Override
    public void onEnable() {
        getLogger().info("Starting plugin...");
        instance = this;

        getLogger().info("Loading configurations...");
        configManager = new ConfigManager(this);
        configManager.register("config");
        configManager.register("messages");
        configManager.register("menus");
        configManager.register("hearts-shop");
        configManager.register("activities");

        pluginSettings = new PluginSettings(configManager.require("config"));
        messages = new Messages(configManager.require("messages"));
        menuConfig = new MenuConfig(configManager.require("menus"), getLogger());
        activitiesConfig = new ActivitiesConfig(configManager.require("activities"));

        core = new Core(this, pluginSettings);
        core.onEnable();

        getLogger().info("Initializing services...");
        marriageService = new MarriageService(this, messages);

        getLogger().info("Loading Hearts shop...");
        HeartsShopLoader loader = new HeartsShopLoader(this, configManager.require("hearts-shop"));
        heartsShop = new HeartsShop(loader, new ConfiguredHeartShopLayout(menuConfig));
        heartsShopService = new HeartsShopService(this, marriageService, messages);

        getLogger().info("Initializing menu system...");
        MenuManager menuManager = new MenuManager();
        marriageMenuManager = new MarriageMenuManager(
                this,
                menuManager,
                marriageService,
                heartsShop,
                heartsShopService,
                menuConfig,
                messages
        );
        preferencesManager = new PreferencesManager();
        registerPreferences();

        getLogger().info("Registering commands...");
        if (getCommand("marriage") == null) {
            getLogger().severe("Failed to register /marriage command. Check plugin.yml.");
            return;
        }

        MarriageCommands marriageCommands = new MarriageCommands(
                this,
                marriageMenuManager,
                marriageService,
                messages
        );
        getCommand("marriage").setExecutor(marriageCommands);
        getCommand("marriage").setTabCompleter(marriageCommands);
        getLogger().info("Registered command: /marriage");

        getLogger().info("Registering listeners...");
        Bukkit.getPluginManager().registerEvents(new MarriageListeners(messages, activitiesConfig), this);

        getLogger().info("Plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling plugin...");

        if (marriageMenuManager != null && marriageMenuManager.getMenuManager() != null) {
            marriageMenuManager.getMenuManager().shutdown();
        }

        if (configManager != null) {
            configManager.shutdown();
        }

        if (core != null) {
            core.onDisable();
        }

        getLogger().info("Plugin disabled successfully.");
    }

    /**
     * Reloads YAML files and rebuilds components that cache parsed values.
     * Open menus are closed so they are not left pointing at old item definitions.
     * Database connections are not restarted; change {@code config.yml} database
     * settings requires a full plugin restart.
     */
    public boolean reloadPluginConfiguration() {
        boolean success = configManager.reloadAll(false);
        if (heartsShop != null) {
            heartsShop.load();
        }
        if (marriageMenuManager != null && marriageMenuManager.getMenuManager() != null) {
            marriageMenuManager.getMenuManager().closeAll();
        }
        return success;
    }

    private void registerPreferences() {
        preferencesManager.register(new PVPPrefenrence());
        preferencesManager.register(new PointsPreference());
    }

    public Core getCore() {
        return core;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PluginSettings getPluginSettings() {
        return pluginSettings;
    }

    public Messages getMessages() {
        return messages;
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public ActivitiesConfig getActivitiesConfig() {
        return activitiesConfig;
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

    public PreferencesManager getPreferencesManager() {
        return preferencesManager;
    }

    public static Plugin getInstance() {
        return instance;
    }

    public CooldownManager getCoolDownManager() {
        return coolDownManager;
    }
}

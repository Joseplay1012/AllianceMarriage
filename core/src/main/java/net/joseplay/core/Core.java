package net.joseplay.core;

import net.joseplay.core.config.PluginSettings;
import net.joseplay.core.feature.defaults.HeartsFeature;
import net.joseplay.core.feature.defaults.PvPFeature;
import net.joseplay.core.storage.CouplesRepository;
import net.joseplay.core.storage.Database;
import net.joseplay.core.storage.FeatureRepository;
import net.joseplay.core.storage.impls.CouplesImpl;
import net.joseplay.core.storage.impls.FeaturesImpl;
import org.bukkit.plugin.java.JavaPlugin;

public final class Core {
    public static Core instance;

    private final FeatureRegistry featureRegistry = new FeatureRegistry();
    private final CouplesRepository couplesRepository;
    private final FeatureRepository featureRepository;
    private final Database dataBase;
    private final PluginSettings pluginSettings;
    private final JavaPlugin plugin;

    public Core(JavaPlugin plugin) {
        this(plugin, new PluginSettings(null));
    }

    public Core(JavaPlugin plugin, PluginSettings pluginSettings) {
        instance = this;
        this.plugin = plugin;
        this.pluginSettings = pluginSettings != null ? pluginSettings : new PluginSettings(null);

        if (plugin != null) {
            plugin.getLogger().info("Initializing Core...");
        }

        this.dataBase = new Database(this.pluginSettings, plugin);

        if (plugin != null) {
            plugin.getLogger().info("Starting database connection...");
        }
        dataBase.start().join();
        if (plugin != null) {
            plugin.getLogger().info("Database started successfully.");
        }

        this.featureRepository = new FeaturesImpl(dataBase, plugin);
        this.couplesRepository = new CouplesImpl(dataBase, featureRegistry, featureRepository);

        if (plugin != null) {
            plugin.getLogger().info("Repositories initialized.");
            plugin.getLogger().info("Core initialized successfully.");
        }
    }

    public void onEnable() {
        if (plugin != null) {
            plugin.getLogger().info("Enabling Core features...");
        }

        featureRegistry.register(new HeartsFeature());
        featureRegistry.register(new PvPFeature());

        if (plugin != null) {
            plugin.getLogger().info("Core enabled successfully.");
        }
    }

    public void onDisable() {
        if (plugin != null) {
            plugin.getLogger().info("Disabling Core...");
        }

        dataBase.shutdown();

        if (plugin != null) {
            plugin.getLogger().info("Database shut down successfully.");
        }

        featureRegistry.clear();

        if (plugin != null) {
            plugin.getLogger().info("Core disabled successfully.");
        }
    }

    public FeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public CouplesRepository getCouplesRepository() {
        return couplesRepository;
    }

    public FeatureRepository getFeatureRepository() {
        return featureRepository;
    }

    public Database getDataBase() {
        return dataBase;
    }

    public PluginSettings getPluginSettings() {
        return pluginSettings;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public static Core getInstance() {
        return instance;
    }
}

package net.joseplay.core;

import net.joseplay.core.config.PluginSettings;
import net.joseplay.core.feature.defaults.HeartsFeature;
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
    private final PluginSettings pluginSettings = new PluginSettings();
    private final JavaPlugin plugin;

    public Core(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;

        plugin.getLogger().info("Initializing Core...");

        this.dataBase = new Database(pluginSettings, plugin);

        plugin.getLogger().info("Starting database connection...");
        dataBase.start();
        plugin.getLogger().info("Database started successfully.");

        this.featureRepository = new FeaturesImpl(dataBase, plugin);
        this.couplesRepository = new CouplesImpl();

        plugin.getLogger().info("Repositories initialized.");
        plugin.getLogger().info("Core initialized successfully.");
    }

    public void onEnable() {
        plugin.getLogger().info("Enabling Core features...");

        featureRegistry.register(new HeartsFeature());

        plugin.getLogger().info("Core enabled successfully.");
    }

    public void onDisable() {
        plugin.getLogger().info("Disabling Core...");

        dataBase.shutdown();

        plugin.getLogger().info("Database shut down successfully.");

        featureRegistry.clear();

        plugin.getLogger().info("Core disabled successfully.");
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
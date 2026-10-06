package net.joseplay.core;

import net.joseplay.core.commands.MarriageCommands;
import net.joseplay.core.config.PluginSettings;
import net.joseplay.core.feature.FeatureRegistry;
import net.joseplay.core.feature.defaults.HeartsFeature;
import net.joseplay.core.storage.CouplesRepository;
import net.joseplay.core.storage.Database;
import net.joseplay.core.storage.FeatureRepository;
import net.joseplay.core.storage.impls.CouplesImpl;
import net.joseplay.core.storage.impls.FeaturesImpl;
import org.bukkit.plugin.java.JavaPlugin;

public final class Core {
    private final FeatureRegistry featureRegistry = new FeatureRegistry();
    private final CouplesRepository couplesRepository;
    private final FeatureRepository featureRepository;
    private final Database dataBase;
    private final PluginSettings pluginSettings = new PluginSettings();
    private final JavaPlugin plugin;

    public Core(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataBase = new Database(pluginSettings, plugin);

        dataBase.start();

        this.featureRepository = new FeaturesImpl(dataBase, plugin);
        this.couplesRepository = new CouplesImpl(this);
    }

    public void onEnable() {
        featureRegistry.register(new HeartsFeature());


        plugin.getCommand("marriage").setExecutor(new MarriageCommands(couplesRepository));
    }

    public void onDisable() {
        dataBase.shutdown();
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
}

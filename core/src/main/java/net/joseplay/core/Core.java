package net.joseplay.test.core;

import net.joseplay.test.core.config.PluginSettings;
import net.joseplay.test.core.feature.FeatureRegistry;
import net.joseplay.test.core.feature.defaults.HeartsFeature;
import net.joseplay.test.core.storage.CouplesRepository;
import net.joseplay.test.core.storage.Database;
import net.joseplay.test.core.storage.FeatureRepository;
import net.joseplay.test.core.storage.impls.CouplesImpl;
import net.joseplay.test.core.storage.impls.FeaturesImpl;
import org.bukkit.plugin.java.JavaPlugin;

public final class Core {
    private final FeatureRegistry featureRegistry = new FeatureRegistry();
    private final CouplesRepository couplesRepository;
    private final FeatureRepository featureRepository;
    private final Database dataBase;
    private final PluginSettings pluginSettings = new PluginSettings();

    public Core(JavaPlugin plugin) {
        this.dataBase = new Database(pluginSettings, plugin);

        dataBase.start();

        this.featureRepository = new FeaturesImpl(dataBase, plugin);
        this.couplesRepository = new CouplesImpl(this);
    }

    public void onEnable() {
        featureRegistry.register(new HeartsFeature());
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
}

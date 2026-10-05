package net.joseplay.core;

import net.joseplay.core.config.PluginSettings;
import net.joseplay.core.feature.FeatureRegistry;
import net.joseplay.core.feature.defaults.HeartsFeature;
import net.joseplay.core.storage.*;
import net.joseplay.core.storage.impls.CouplesImpl;
import net.joseplay.core.storage.impls.FeaturesImpl;
import org.bukkit.plugin.java.JavaPlugin;

public final class Core {
    public final FeatureRegistry featureRegistry = new FeatureRegistry();
    public final CouplesRepository couplesRepository;
    public final FeatureRepository featureRepository;
    public final Database dataBase;
    public final PluginSettings pluginSettings = new PluginSettings();

    public Core(JavaPlugin plugin) {
        this.dataBase = new Database(pluginSettings, plugin);
        this.featureRepository = new FeaturesImpl(dataBase, plugin);
        this.couplesRepository = new CouplesImpl(this);
    }

    public void onEnable() {
        featureRegistry.register(new HeartsFeature());
    }

    public void onDisable() {
        // Plugin shutdown logic
    }
}

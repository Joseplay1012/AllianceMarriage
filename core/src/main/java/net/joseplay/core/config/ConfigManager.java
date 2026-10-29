package net.joseplay.core.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry and cache for plugin YAML files.
 *
 * <p>Configurations stay in memory for the lifetime of the plugin. {@code set} only
 * mutates the cache; {@link #save(String)} / {@link #saveAll()} write to disk.
 *
 * <p>Reload behaviour for consumers: values read through {@link PluginConfig} getters
 * are live after a successful reload. Objects built from those values (open menus,
 * database connections, parsed shop catalogs) are not updated automatically — those
 * components must rebuild on reload. Unsaved in-memory edits are not discarded.
 */
public final class ConfigManager {
    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Logger logger;
    private final ConcurrentHashMap<String, PluginConfig> configs = new ConcurrentHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.logger = plugin.getLogger();
    }

    public ConfigManager(File dataFolder, Logger logger) {
        this.plugin = null;
        this.dataFolder = dataFolder;
        this.logger = logger != null ? logger : Logger.getLogger("AllianceMarriage");
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Registers a YAML file relative to the plugin data folder.
     * Existing files are loaded; missing files are copied from the jar or created empty.
     * Registering the same id twice returns the cached instance.
     */
    public PluginConfig register(String name) {
        String id = PluginConfig.normalizeId(name);
        return register(id, id + ".yml");
    }

    public PluginConfig register(String name, String resourcePath) {
        String id = PluginConfig.normalizeId(name);
        PluginConfig existing = configs.get(id);
        if (existing != null) {
            return existing;
        }

        PluginConfig created = new PluginConfig(plugin, dataFolder, logger, id, resourcePath);
        PluginConfig raced = configs.putIfAbsent(id, created);
        if (raced != null) {
            return raced;
        }

        created.load(false);
        return created;
    }

    public PluginConfig getConfig(String name) {
        return configs.get(PluginConfig.normalizeId(name));
    }

    public PluginConfig require(String name) {
        PluginConfig config = getConfig(name);
        if (config == null) {
            throw new IllegalStateException("Configuration '" + name + "' is not registered.");
        }
        return config;
    }

    public boolean isLoaded(String name) {
        PluginConfig config = getConfig(name);
        return config != null && config.isLoaded();
    }

    public Set<String> getLoadedConfigs() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(configs.keySet()));
    }

    public boolean save(String name) {
        PluginConfig config = require(name);
        return config.save();
    }

    public boolean reload(String name) {
        return reload(name, false);
    }

    public boolean reload(String name, boolean discardUnsaved) {
        PluginConfig config = require(name);
        synchronized (config) {
            return config.reload(discardUnsaved);
        }
    }

    public boolean saveAll() {
        boolean success = true;
        for (PluginConfig config : configs.values()) {
            if (!config.save()) {
                success = false;
            }
        }
        return success;
    }

    public boolean reloadAll() {
        return reloadAll(false);
    }

    public boolean reloadAll(boolean discardUnsaved) {
        boolean success = true;
        for (PluginConfig config : configs.values()) {
            synchronized (config) {
                if (!config.reload(discardUnsaved)) {
                    success = false;
                }
            }
        }
        return success;
    }

    /**
     * Removes a configuration from the cache. Dirty files are saved unless {@code save} is false.
     */
    public void unload(String name) {
        unload(name, true);
    }

    public void unload(String name, boolean save) {
        PluginConfig config = configs.remove(PluginConfig.normalizeId(name));
        if (config == null) {
            return;
        }
        if (save && config.isDirty()) {
            config.save();
        }
    }

    public void shutdown() {
        saveAll();
        configs.clear();
    }
}

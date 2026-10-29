package net.joseplay.core.config;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory YAML configuration bound to a single file.
 * {@link #set(String, Object)} updates the cache only; call {@link #save()} to persist.
 */
public final class PluginConfig {
    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Logger logger;
    private final String id;
    private final String resourcePath;
    private final File file;
    private final Object lock = new Object();

    private YamlConfiguration yaml = new YamlConfiguration();
    private boolean dirty;
    private boolean loadFailed;
    private boolean loaded;

    PluginConfig(JavaPlugin plugin, File dataFolder, Logger logger, String id, String resourcePath) {
        this.plugin = plugin;
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.logger = logger != null ? logger : Logger.getLogger("AllianceMarriage");
        this.id = normalizeId(id);
        this.resourcePath = resourcePath != null ? resourcePath.replace('\\', '/') : this.id + ".yml";
        this.file = new File(this.dataFolder, this.id + ".yml");
    }

    public PluginConfig(File file, Logger logger) {
        this.plugin = null;
        this.file = Objects.requireNonNull(file, "file");
        this.dataFolder = file.getParentFile() != null ? file.getParentFile() : new File(".");
        this.logger = logger != null ? logger : Logger.getLogger("AllianceMarriage");
        this.id = normalizeId(stripExtension(file.getName()));
        this.resourcePath = this.id + ".yml";
        load(false);
    }

    public static String normalizeId(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Configuration name cannot be empty");
        }

        String normalized = name.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".yml")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith(".yaml")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        return normalized;
    }

    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean hasLoadFailed() {
        return loadFailed;
    }

    public YamlConfiguration getHandle() {
        return yaml;
    }

    public void load() {
        load(false);
    }

    /**
     * Reloads this file from disk into the cache.
     * Unsaved in-memory changes are kept unless {@code discardUnsaved} is true.
     *
     * @return {@code true} if the file was loaded successfully
     */
    public boolean reload() {
        return reload(false);
    }

    public boolean reload(boolean discardUnsaved) {
        synchronized (lock) {
            if (dirty && !discardUnsaved) {
                logger.warning("Skipped reload of '" + id + "' because there are unsaved in-memory changes. "
                        + "Call save() first or reload(true) to discard them.");
                return false;
            }
            return load(true);
        }
    }

    boolean load(boolean fromReload) {
        synchronized (lock) {
            ensureDefaultFile();

            if (!file.exists()) {
                logger.severe("Configuration file is missing and could not be created: " + file.getAbsolutePath());
                loadFailed = true;
                loaded = false;
                return false;
            }

            try {
                String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                YamlConfiguration loadedYaml = new YamlConfiguration();
                loadedYaml.loadFromString(contents);
                applyJarDefaults(loadedYaml);
                this.yaml = loadedYaml;
                this.dirty = false;
                this.loadFailed = false;
                this.loaded = true;
                return true;
            } catch (InvalidConfigurationException exception) {
                logger.log(Level.SEVERE, "Invalid YAML in '" + file.getAbsolutePath()
                        + "'. The previous in-memory values were kept and the file was not overwritten.", exception);
                loadFailed = true;
                if (!fromReload) {
                    loaded = false;
                }
                return false;
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Failed to read configuration '" + file.getAbsolutePath() + "'.", exception);
                loadFailed = true;
                if (!fromReload) {
                    loaded = false;
                }
                return false;
            }
        }
    }

    public boolean save() {
        synchronized (lock) {
            if (loadFailed) {
                logger.severe("Refusing to save '" + id + "' because the last load failed. Fix the YAML file first.");
                return false;
            }

            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.severe("Could not create directory for configuration: " + parent.getAbsolutePath());
                return false;
            }

            try {
                yaml.save(file);
                dirty = false;
                return true;
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Failed to save configuration '" + file.getAbsolutePath() + "'.", exception);
                return false;
            }
        }
    }

    public String getString(String path) {
        return yaml.getString(path);
    }

    public String getString(String path, String defaultValue) {
        return yaml.getString(path, defaultValue);
    }

    public int getInt(String path) {
        return yaml.getInt(path);
    }

    public int getInt(String path, int defaultValue) {
        return yaml.getInt(path, defaultValue);
    }

    public long getLong(String path) {
        return yaml.getLong(path);
    }

    public long getLong(String path, long defaultValue) {
        return yaml.getLong(path, defaultValue);
    }

    public double getDouble(String path) {
        return yaml.getDouble(path);
    }

    public double getDouble(String path, double defaultValue) {
        return yaml.getDouble(path, defaultValue);
    }

    public float getFloat(String path) {
        return (float) yaml.getDouble(path);
    }

    public float getFloat(String path, float defaultValue) {
        return (float) yaml.getDouble(path, defaultValue);
    }

    public boolean getBoolean(String path) {
        return yaml.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return yaml.getBoolean(path, defaultValue);
    }

    public Number getNumber(String path) {
        return getNumber(path, 0);
    }

    public Number getNumber(String path, Number defaultValue) {
        Object value = yaml.get(path);
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String text) {
            try {
                if (text.contains(".") || text.contains("e") || text.contains("E")) {
                    return Double.parseDouble(text);
                }
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public List<String> getStringList(String path) {
        return yaml.getStringList(path);
    }

    public List<Integer> getIntegerList(String path) {
        return yaml.getIntegerList(path);
    }

    public List<Long> getLongList(String path) {
        return yaml.getLongList(path);
    }

    public List<Double> getDoubleList(String path) {
        return yaml.getDoubleList(path);
    }

    public List<Boolean> getBooleanList(String path) {
        return yaml.getBooleanList(path);
    }

    public List<?> getList(String path) {
        List<?> list = yaml.getList(path);
        return list != null ? list : Collections.emptyList();
    }

    public List<?> getList(String path, List<?> defaultValue) {
        List<?> list = yaml.getList(path);
        return list != null ? list : defaultValue;
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return yaml.getConfigurationSection(path);
    }

    public ConfigurationSection createSection(String path) {
        synchronized (lock) {
            dirty = true;
            return yaml.createSection(path);
        }
    }

    public boolean contains(String path) {
        return yaml.contains(path);
    }

    public boolean isSet(String path) {
        return yaml.isSet(path);
    }

    public boolean isList(String path) {
        return yaml.isList(path);
    }

    public boolean isConfigurationSection(String path) {
        return yaml.isConfigurationSection(path);
    }

    public Set<String> getKeys(String path, boolean deep) {
        if (path == null || path.isEmpty()) {
            return yaml.getKeys(deep);
        }
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return Collections.emptySet();
        }
        return section.getKeys(deep);
    }

    public Map<String, Object> getValues(String path, boolean deep) {
        if (path == null || path.isEmpty()) {
            return yaml.getValues(deep);
        }
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return Collections.emptyMap();
        }
        return section.getValues(deep);
    }

    public Object get(String path) {
        return yaml.get(path);
    }

    public Object get(String path, Object defaultValue) {
        return yaml.get(path, defaultValue);
    }

    public <T extends ConfigurationSerializable> T getSerializable(String path, Class<T> type) {
        return yaml.getSerializable(path, type);
    }

    public ItemStack getItemStack(String path) {
        return yaml.getItemStack(path);
    }

    public ItemStack getItemStack(String path, ItemStack defaultValue) {
        return yaml.getItemStack(path, defaultValue);
    }

    public Location getLocation(String path) {
        Object value = yaml.get(path);
        if (value instanceof Location location) {
            return location;
        }
        if (value instanceof ConfigurationSection section) {
            return ConfigSerializers.deserializeLocation(section);
        }
        if (value instanceof Map<?, ?> map) {
            return ConfigSerializers.deserializeLocation(map);
        }
        ConfigurationSection section = yaml.getConfigurationSection(path);
        return ConfigSerializers.deserializeLocation(section);
    }

    /**
     * Updates a value in memory. Pass {@code null} to remove the path.
     * Does not write to disk until {@link #save()} is called.
     */
    public void set(String path, Object value) {
        synchronized (lock) {
            Object stored = value;
            if (value instanceof Location location) {
                stored = ConfigSerializers.serializeLocation(location);
            } else if (value instanceof ItemStack itemStack) {
                stored = ConfigSerializers.cloneItem(itemStack);
            }
            yaml.set(path, stored);
            dirty = true;
        }
    }

    public void setItemStack(String path, ItemStack itemStack) {
        set(path, itemStack);
    }

    public void setLocation(String path, Location location) {
        set(path, location);
    }

    public void remove(String path) {
        set(path, null);
    }

    private void ensureDefaultFile() {
        if (file.exists()) {
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.severe("Could not create configuration directory: " + parent.getAbsolutePath());
            return;
        }

        if (plugin != null && resourcePath != null && plugin.getResource(resourcePath) != null) {
            try {
                plugin.saveResource(resourcePath, false);
                logger.info("Created default configuration: " + resourcePath);
                return;
            } catch (IllegalArgumentException exception) {
                logger.log(Level.WARNING, "Could not copy default resource '" + resourcePath + "'.", exception);
            }
        }

        try {
            if (file.createNewFile()) {
                logger.info("Created empty configuration file: " + file.getName());
            }
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not create configuration file: " + file.getAbsolutePath(), exception);
        }
    }

    private void applyJarDefaults(YamlConfiguration loadedYaml) {
        if (plugin == null || resourcePath == null) {
            return;
        }

        InputStream resource = plugin.getResource(resourcePath);
        if (resource == null) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            loadedYaml.setDefaults(defaults);
            loadedYaml.options().copyDefaults(false);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Could not load default values for '" + resourcePath + "'.", exception);
        }
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}

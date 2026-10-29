package net.joseplay.core.config;

/**
 * Typed facade over {@code config.yml} for general plugin settings.
 */
public class PluginSettings {
    private final PluginConfig config;

    public PluginSettings(PluginConfig config) {
        this.config = config;
    }

    public PluginConfig config() {
        return config;
    }

    public boolean debug() {
        return getBoolean("debug", false);
    }

    public boolean mysql() {
        return getBoolean("database.mysql.enabled", false);
    }

    public String mysqlDatabase() {
        return getString("database.mysql.database", "AllianceMariage");
    }

    public String mysqlHost() {
        return getString("database.mysql.host", "localhost");
    }

    public int mysqlPort() {
        return getInt("database.mysql.port", 3306);
    }

    public String mysqlUsername() {
        return getString("database.mysql.username", "root");
    }

    public String mysqlPassword() {
        return getString("database.mysql.password", "admin");
    }

    private boolean getBoolean(String path, boolean defaultValue) {
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    private String getString(String path, String defaultValue) {
        return config != null ? config.getString(path, defaultValue) : defaultValue;
    }

    private int getInt(String path, int defaultValue) {
        return config != null ? config.getInt(path, defaultValue) : defaultValue;
    }
}

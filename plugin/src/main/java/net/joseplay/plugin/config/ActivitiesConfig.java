package net.joseplay.plugin.config;

import net.joseplay.core.config.PluginConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActivitiesConfig {
    private final Map<String, ActivityResult> activitiesMap = new ConcurrentHashMap<>();

    private final PluginConfig config;

    public ActivitiesConfig(PluginConfig config) {
        this.config = config;
    }

    public PluginConfig config() {
        return config;
    }

    public double getMaxDistance(){
        return config.getDouble("config.max-distance", 10.0);
    }

    public ActivityResult get(String type) {
        ActivityResult existActivity = activitiesMap.get(type);

        if (existActivity != null) return existActivity;

        ConfigurationSection section = config.getConfigurationSection("activities." + type);

        if (section != null){
            return new ActivityResult(
                    section.getInt("cooldown", -0),
                    section.getInt("hearts", 0),
                    section.getDouble("chance", 0.0)
            );
        }

        return null;
    }


    public record ActivityResult(int cooldown, int points, double canche){}
}

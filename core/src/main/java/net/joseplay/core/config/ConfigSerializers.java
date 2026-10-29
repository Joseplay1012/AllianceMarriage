package net.joseplay.core.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes Bukkit objects using native {@link org.bukkit.configuration.serialization.ConfigurationSerializable}
 * maps where possible, without executing arbitrary classes.
 */
public final class ConfigSerializers {
    private ConfigSerializers() {
    }

    public static Map<String, Object> serializeLocation(Location location) {
        if (location == null) {
            return null;
        }

        Map<String, Object> values = new LinkedHashMap<>();
        World world = location.getWorld();
        values.put("world", world != null ? world.getName() : null);
        values.put("x", location.getX());
        values.put("y", location.getY());
        values.put("z", location.getZ());
        values.put("yaw", location.getYaw());
        values.put("pitch", location.getPitch());
        return values;
    }

    public static Location deserializeLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        World world = resolveWorld(worldName);
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = NumberConversions.toFloat(section.getDouble("yaw"));
        float pitch = NumberConversions.toFloat(section.getDouble("pitch"));
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static Location deserializeLocation(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        String worldName = values.get("world") != null ? String.valueOf(values.get("world")) : null;
        World world = resolveWorld(worldName);
        double x = NumberConversions.toDouble(values.get("x"));
        double y = NumberConversions.toDouble(values.get("y"));
        double z = NumberConversions.toDouble(values.get("z"));
        float yaw = NumberConversions.toFloat(values.get("yaw"));
        float pitch = NumberConversions.toFloat(values.get("pitch"));
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * ItemStacks are stored with Bukkit's own serializer. This method exists so callers
     * do not convert items to strings or drop metadata.
     */
    public static ItemStack cloneItem(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.clone();
    }

    private static World resolveWorld(String worldName) {
        if (worldName == null || worldName.isEmpty() || "null".equalsIgnoreCase(worldName)) {
            return null;
        }

        try {
            return Bukkit.getWorld(worldName);
        } catch (Exception ignored) {
            return null;
        }
    }
}

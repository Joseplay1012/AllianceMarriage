package net.joseplay.plugin.config;

import net.joseplay.core.config.PluginConfig;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Reads menu titles, sizes and items from {@code menus.yml}.
 * Item actions stay in Java; this class only supplies the configurable presentation.
 */
public final class MenuConfig {
    private final PluginConfig config;
    private final Logger logger;

    public MenuConfig(PluginConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public PluginConfig config() {
        return config;
    }

    public String title(String menu, String defaultTitle) {
        return config.getString(menu + ".title", defaultTitle);
    }

    public int rows(String menu, int defaultRows) {
        return config.getInt(menu + ".rows", defaultRows);
    }

    public int slot(String menu, String itemId, int defaultSlot) {
        return config.getInt(menu + ".items." + itemId + ".slot", defaultSlot);
    }

    public List<Integer> integerList(String path, List<Integer> defaultValue) {
        List<Integer> values = config.getIntegerList(path);
        return values.isEmpty() ? defaultValue : values;
    }

    public boolean isAllowed(Player player, String menu, String itemId) {
        String permission = config.getString(menu + ".items." + itemId + ".permission", "");
        if (permission == null || permission.isBlank()) {
            permission = config.getString(menu + ".preferences." + itemId + ".permission", "");
        }
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return player != null && player.hasPermission(permission);
    }

    public ItemStack filler(String menu) {
        return build(menu + ".filler", null, null);
    }

    public ItemStack item(String menu, String itemId, String... replacements) {
        return build(menu + ".items." + itemId, null, replacements);
    }

    public ItemStack item(String menu, String itemId, OfflinePlayer skullOwner, String... replacements) {
        return build(menu + ".items." + itemId, skullOwner, replacements);
    }

    public ItemStack preference(String menu, String preferenceId, String... replacements) {
        return build(menu + ".preferences." + preferenceId, null, replacements);
    }

    public String preferenceText(String menu, String preferenceId, String key, String defaultValue, String... replacements) {
        String value = config.getString(menu + ".preferences." + preferenceId + "." + key, defaultValue);
        return Messages.color(Messages.apply(value, replacements));
    }

    public ItemStack build(String path, OfflinePlayer skullOwner, String... replacements) {
        if (!config.isSet(path) && !config.isConfigurationSection(path)) {
            return ItemBuilder.of(Material.STONE).name(path).build();
        }

        String materialName = config.getString(path + ".material", "STONE");
        Material material = Material.matchMaterial(materialName != null ? materialName : "STONE");
        if (material == null) {
            if (logger != null) {
                logger.warning("Unknown material '" + materialName + "' at " + path);
            }
            material = Material.STONE;
        }

        int amount = Math.max(1, config.getInt(path + ".amount", 1));
        ItemBuilder builder = ItemBuilder.of(material, amount);

        String name = config.getString(path + ".name", "");
        if (name != null && !name.isEmpty()) {
            builder.name(Messages.apply(name, replacements));
        }

        List<String> lore = config.getStringList(path + ".lore");
        if (!lore.isEmpty()) {
            List<String> formatted = new ArrayList<>();
            for (String line : lore) {
                formatted.add(Messages.apply(line, replacements));
            }
            builder.lore(formatted);
        }

        builder.glow(config.getBoolean(path + ".glow", false));
        builder.unbreakable(config.getBoolean(path + ".unbreakable", false));
        builder.hideToolTip(config.getBoolean(path + ".hide-tooltip", false));

        int customModelData = config.getInt(path + ".custom-model-data", 0);
        if (customModelData > 0) {
            builder.customModelData(customModelData);
        }

        if (skullOwner != null) {
            builder.skullOwner(skullOwner);
        }

        for (String flagName : config.getStringList(path + ".hide-flags")) {
            ItemFlag flag = parseItemFlag(flagName);
            if (flag != null) {
                builder.flags(flag);
            }
        }

        ConfigurationSection enchantments = config.getConfigurationSection(path + ".enchantments");
        if (enchantments != null) {
            for (String enchantName : enchantments.getKeys(false)) {
                Enchantment enchantment = parseEnchantment(enchantName);
                if (enchantment != null) {
                    builder.enchant(enchantment, enchantments.getInt(enchantName, 1));
                }
            }
        }

        return builder.build();
    }

    private ItemFlag parseItemFlag(String flagName) {
        if (flagName == null || flagName.isBlank()) {
            return null;
        }
        String normalized = flagName.toUpperCase(Locale.ROOT).trim();
        try {
            return ItemFlag.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
        }
        if (!normalized.startsWith("HIDE_")) {
            try {
                return ItemFlag.valueOf("HIDE_" + normalized);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private Enchantment parseEnchantment(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.toLowerCase(Locale.ROOT).trim();
        if (clean.startsWith("minecraft:")) {
            clean = clean.substring("minecraft:".length());
        }
        try {
            Enchantment byKey = Enchantment.getByKey(NamespacedKey.minecraft(clean));
            if (byKey != null) {
                return byKey;
            }
        } catch (Exception ignored) {
        }
        return Enchantment.getByName(clean.toUpperCase(Locale.ROOT));
    }
}

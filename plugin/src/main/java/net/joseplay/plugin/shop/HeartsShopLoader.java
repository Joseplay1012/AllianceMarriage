package net.joseplay.plugin.shop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class HeartsShopLoader {
    private final Plugin plugin;
    private final File configFile;

    public HeartsShopLoader(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "hearts-shop.yml");
    }

    public Map<String, HeartsShopItem> load() {
        Logger logger = plugin.getLogger();
        Map<String, HeartsShopItem> loadedItems = new LinkedHashMap<>();

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                plugin.saveResource("hearts-shop.yml", false);
                logger.info("Created default hearts-shop.yml configuration.");
            } catch (IllegalArgumentException e) {
                logger.warning("Default hearts-shop.yml not found in jar resources. Generating empty file.");
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("No 'items' section found in hearts-shop.yml!");
            return loadedItems;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection sec = itemsSection.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }

            try {
                HeartsShopItem item = parseItem(key, sec);
                if (item != null) {
                    loadedItems.put(key, item);
                }
            } catch (Exception e) {
                logger.severe("Failed to load shop item '" + key + "': " + e.getMessage());
            }
        }

        logger.info("Loaded " + loadedItems.size() + " shop items from hearts-shop.yml.");
        return loadedItems;
    }

    private HeartsShopItem parseItem(String id, ConfigurationSection sec) {
        Logger logger = plugin.getLogger();

        String materialName = sec.getString("material");
        if (materialName == null || materialName.isBlank()) {
            logger.warning("Item '" + id + "' has no material defined. Skipping.");
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            logger.warning("Item '" + id + "' has invalid material: " + materialName + ". Skipping.");
            return null;
        }

        int amount = sec.getInt("amount", 1);
        if (amount <= 0) {
            amount = 1;
        }

        int price = sec.getInt("price", -1);
        if (price < 0) {
            logger.warning("Item '" + id + "' has invalid price (" + price + "). Must be 0 or higher. Skipping.");
            return null;
        }

        String slot = sec.getString("slot", "AUTO");

        List<String> commands = sec.getStringList("commands");
        if (commands.isEmpty() && sec.isString("commands")) {
            String singleCmd = sec.getString("commands");
            if (singleCmd != null && !singleCmd.isBlank()) {
                commands = List.of(singleCmd);
            }
        }

        if (commands.isEmpty()) {
            logger.warning("Item '" + id + "' has no commands defined. Skipping.");
            return null;
        }

        String name = "";
        List<String> lore = new ArrayList<>();
        boolean glow = false;
        boolean unbreakable = false;
        int customModelData = 0;
        List<ItemFlag> hideFlags = new ArrayList<>();
        Map<Enchantment, Integer> enchantments = new HashMap<>();

        if (sec.isConfigurationSection("display")) {
            ConfigurationSection display = sec.getConfigurationSection("display");
            if (display != null) {
                name = display.getString("name", "");
                lore = display.getStringList("lore");
                glow = display.getBoolean("glow", false);
                unbreakable = display.getBoolean("unbreakable", false);
                customModelData = display.getInt("custom-model-data", 0);

                List<String> flagsList = display.getStringList("hide-flags");
                for (String flagName : flagsList) {
                    ItemFlag flag = parseItemFlag(flagName);
                    if (flag != null) {
                        hideFlags.add(flag);
                    }
                }

                if (display.isConfigurationSection("enchantments")) {
                    ConfigurationSection enchSec = display.getConfigurationSection("enchantments");
                    if (enchSec != null) {
                        for (String enchKey : enchSec.getKeys(false)) {
                            Enchantment ench = parseEnchantment(enchKey);
                            if (ench != null) {
                                int level = enchSec.getInt(enchKey, 1);
                                enchantments.put(ench, level);
                            }
                        }
                    }
                }
            }
        } else {
            name = sec.getString("name", "");
            lore = sec.getStringList("lore");
            glow = sec.getBoolean("glow", false);
            unbreakable = sec.getBoolean("unbreakable", false);
            customModelData = sec.getInt("custom-model-data", 0);
        }

        return new HeartsShopItem(
                id,
                material,
                amount,
                price,
                slot,
                name,
                lore,
                glow,
                unbreakable,
                customModelData,
                hideFlags,
                enchantments,
                commands
        );
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
        plugin.getLogger().warning("Unknown ItemFlag: " + flagName);
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
            NamespacedKey key = NamespacedKey.minecraft(clean);
            Enchantment ench = Enchantment.getByKey(key);
            if (ench != null) {
                return ench;
            }
        } catch (Exception ignored) {
        }

        Enchantment legacy = Enchantment.getByName(clean.toUpperCase(Locale.ROOT));
        if (legacy != null) {
            return legacy;
        }

        plugin.getLogger().warning("Unknown Enchantment: " + name);
        return null;
    }
}

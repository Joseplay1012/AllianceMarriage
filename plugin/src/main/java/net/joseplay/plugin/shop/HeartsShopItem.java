package net.joseplay.plugin.shop;

import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HeartsShopItem {
    private final String id;
    private final Material material;
    private final int amount;
    private final int price;
    private final String slot; // "AUTO" or slot number
    private final String name;
    private final List<String> lore;
    private final boolean glow;
    private final boolean unbreakable;
    private final int customModelData;
    private final List<ItemFlag> hideFlags;
    private final Map<Enchantment, Integer> enchantments;
    private final List<String> commands;

    public HeartsShopItem(
            String id,
            Material material,
            int amount,
            int price,
            String slot,
            String name,
            List<String> lore,
            boolean glow,
            boolean unbreakable,
            int customModelData,
            List<ItemFlag> hideFlags,
            Map<Enchantment, Integer> enchantments,
            List<String> commands
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.material = Objects.requireNonNull(material, "material cannot be null");
        this.amount = Math.max(1, amount);
        this.price = Math.max(0, price);
        this.slot = slot != null && !slot.isBlank() ? slot : "AUTO";
        this.name = name != null ? name : "";
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
        this.glow = glow;
        this.unbreakable = unbreakable;
        this.customModelData = customModelData;
        this.hideFlags = hideFlags != null ? new ArrayList<>(hideFlags) : new ArrayList<>();
        this.enchantments = enchantments != null ? new HashMap<>(enchantments) : new HashMap<>();
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getPrice() {
        return price;
    }

    public String getSlot() {
        return slot;
    }

    public boolean isAutoSlot() {
        return "AUTO".equalsIgnoreCase(slot);
    }

    public Integer getExplicitSlot() {
        if (isAutoSlot()) {
            return null;
        }
        try {
            return Integer.parseInt(slot);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public boolean isGlow() {
        return glow;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public List<ItemFlag> getHideFlags() {
        return Collections.unmodifiableList(hideFlags);
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public ItemStack createItemStack(Player player, Couple couple) {
        int currentHearts = 0;
        if (couple != null) {
            Integer hearts = couple.features().get("hearts");
            if (hearts != null) {
                currentHearts = hearts;
            }
        }

        String playerName = player != null ? player.getName() : "Player";
        String playerUuid = player != null ? player.getUniqueId().toString() : "";
        String heartsStr = String.valueOf(currentHearts);
        String priceStr = String.valueOf(price);

        ItemBuilder builder = ItemBuilder.of(material, amount);

        if (!name.isEmpty()) {
            String formattedName = formatPlaceholders(name, playerName, playerUuid, priceStr, id, heartsStr);
            builder.name(formattedName);
        }

        List<String> formattedLore = new ArrayList<>();
        for (String line : lore) {
            formattedLore.add(formatPlaceholders(line, playerName, playerUuid, priceStr, id, heartsStr));
        }
        builder.lore(formattedLore);

        builder.glow(glow);
        builder.unbreakable(unbreakable);
        if (customModelData > 0) {
            builder.customModelData(customModelData);
        }

        for (ItemFlag flag : hideFlags) {
            builder.flags(flag);
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            builder.enchant(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    public static String formatPlaceholders(String text, String player, String uuid, String price, String item, String hearts) {
        if (text == null) return "";
        return text.replace("%player%", player)
                   .replace("%uuid%", uuid)
                   .replace("%price%", price)
                   .replace("%item%", item)
                   .replace("%hearts%", hearts);
    }
}

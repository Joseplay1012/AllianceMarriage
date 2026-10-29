package net.joseplay.plugin.preferences;

import net.joseplay.api.menu.MenuContext;
import net.joseplay.api.menu.MenuItem;
import net.joseplay.api.preferences.Preference;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.Plugin;
import net.joseplay.plugin.config.MenuConfig;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PointsPreference implements Preference {
    @Override
    public MenuItem get(UUID uuid) {
        return null;
    }

    @Override
    public <T> MenuItem get(T c) {
        Couple couple = (Couple) c;
        MenuConfig menuConfig = Plugin.getInstance().getMenuConfig();

        if (couple != null) {
            Object hearts = couple.features().get("hearts");
            ItemStack itemStack = menuConfig.preference(
                    "couple-settings",
                    "points",
                    "%hearts%",
                    String.valueOf(hearts)
            );

            return new MenuItem(itemStack, ctx -> couple.features().increment("hearts", 10));
        }

        return new MenuItem(ItemBuilder.of(Material.BARRIER).build(), MenuContext::update);
    }
}

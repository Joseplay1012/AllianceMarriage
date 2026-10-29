package net.joseplay.plugin.preferences;

import net.joseplay.api.menu.MenuItem;
import net.joseplay.api.preferences.Preference;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.Plugin;
import net.joseplay.plugin.config.MenuConfig;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PVPPrefenrence implements Preference {
    @Override
    public MenuItem get(UUID uuid) {
        return MenuItem.of(ItemBuilder.of(Material.BARRIER).build(), ctx -> {});
    }

    @Override
    public <T> MenuItem get(T c) {
        Couple couple = (Couple) c;
        MenuConfig menuConfig = Plugin.getInstance().getMenuConfig();

        if (couple != null) {
            boolean pvp = couple.features().get("pvp");
            String status = pvp
                    ? menuConfig.preferenceText("couple-settings", "pvp", "status-active", "&cActive")
                    : menuConfig.preferenceText("couple-settings", "pvp", "status-inactive", "&aInactive");

            ItemStack itemStack = menuConfig.preference("couple-settings", "pvp", "%status%", status);

            return new MenuItem(itemStack, ctx -> couple.features().set("pvp", !pvp));
        }

        return MenuItem.of(ItemBuilder.of(Material.BARRIER).build(), ctx -> {});
    }
}

package net.joseplay.plugin.preferences;

import net.joseplay.api.menu.MenuContext;
import net.joseplay.api.menu.MenuItem;
import net.joseplay.api.preferences.Preference;
import net.joseplay.core.couple.Couple;
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


        if (couple != null){

            ItemStack itemStack = ItemBuilder.of(Material.DIAMOND)
                    .name("§aAdd Points")
                    .addLore("§anow " + couple.features().get("hearts"))
                    .build();



            return new MenuItem(itemStack, ctx -> {
              couple.features().increment("hearts", 10);
            });
        }

        return new MenuItem(ItemBuilder.of(Material.BARRIER).build(), MenuContext::update);
    }
}

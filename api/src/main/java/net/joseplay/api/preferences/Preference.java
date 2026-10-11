package net.joseplay.api.preferences;

import net.joseplay.api.menu.MenuItem;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface Preference {
    MenuItem get(UUID uuid);

    <T> MenuItem get(T couple);
}

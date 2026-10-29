package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuContext;
import net.joseplay.api.menu.MenuItem;
import net.joseplay.api.menu.Pagination;
import net.joseplay.api.preferences.Preference;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.Plugin;
import net.joseplay.plugin.config.MenuConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CoupleSettings extends Menu {
    private static final List<Integer> DEFAULT_SLOTS = List.of(
            10, 12, 14, 16,
            20, 22, 24,
            28, 30, 32, 34
    );

    private final Player player;
    private final Couple couple;
    private final MarriageMenuManager menuManager;
    private final MenuConfig menuConfig;
    private final Pagination<Preference> pagination;
    private final List<Integer> slots;

    public CoupleSettings(Player player, Couple couple, MarriageMenuManager menuManager) {
        super(menuManager.getMenuConfig().title("couple-settings", "Settings"),
                menuManager.getMenuConfig().rows("couple-settings", 5));
        this.player = player;
        this.couple = couple;
        this.menuManager = menuManager;
        this.menuConfig = menuManager.getMenuConfig();
        this.slots = menuConfig.integerList("couple-settings.preference-slots", DEFAULT_SLOTS);
        this.pagination = new Pagination<>(new ArrayList<>(Plugin.getInstance().getPreferencesManager().getAll()), slots.size());

        render();
    }

    public void render() {
        ItemStack filler = menuConfig.filler("couple-settings");
        fill(filler);

        List<Preference> items = pagination.getItemsForCurrentPage();

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);

            if (i < items.size()) {
                MenuItem menuItem = items.get(i).get(couple);
                setItem(slot, menuItem.getItemStack(), ctx -> {
                    menuItem.getAction().execute(ctx);
                    update(player);
                });
            }
        }

        setItem(
                menuConfig.slot("couple-settings", "next", 41),
                menuConfig.item("couple-settings", "next"),
                ctx -> {
                    if (pagination.hasNextPage()) {
                        pagination.nextPage();
                        update(player);
                    }
                }
        );

        setItem(
                menuConfig.slot("couple-settings", "back", 39),
                menuConfig.item("couple-settings", "back"),
                ctx -> {
                    if (pagination.hasPreviousPage()) {
                        pagination.previousPage();
                        update(player);
                    }
                }
        );

        setItem(
                menuConfig.slot("couple-settings", "close", 40),
                menuConfig.item("couple-settings", "close"),
                MenuContext::close
        );
    }

    @Override
    public void update(Player player) {
        clear();
        render();
    }
}

package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuContext;
import net.joseplay.api.menu.MenuItem;
import net.joseplay.api.menu.Pagination;
import net.joseplay.api.preferences.Preference;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.Plugin;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CoupleSettings extends Menu {
    private final Player player;
    private final Couple couple;
    private final MarriageMenuManager menuManager;
    private Pagination<Preference> pagination;
    private static final List<Integer> SLOTS = List.of(
            10, 12, 14, 16,
            20, 22, 24,
            28, 30, 32, 34
    );

    public CoupleSettings(Player player, Couple couple, MarriageMenuManager menuManager) {
        super("Settings", 5);
        this.player = player;
        this.couple = couple;
        this.menuManager = menuManager;

        pagination = new Pagination<>(new ArrayList<>(Plugin.getInstance().getPreferencesManager().getAll()), 10);

        render();
    }


    public void render() {

        ItemStack filler = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideToolTip(true)
                .build();
        fill(filler);


        List<Preference> itens = pagination.getItemsForCurrentPage();

        for (int i = 0; i < SLOTS.size(); i++) {
            int slot = SLOTS.get(i);

            if (i < itens.size()){

                MenuItem menuItem = itens.get(i).get(couple);

                setItem(slot, menuItem.getItemStack(), ctx -> {
                    menuItem.getAction().execute(ctx);
                    update(player);
                });
            }
        }


        ItemStack nextItem = ItemBuilder.of(Material.ARROW)
                .name("Next page")
                .build();

        setItem(41, nextItem, ctx -> {
            if (pagination.hasNextPage()){
                pagination.nextPage();
                update(player);
            }
        });

        ItemStack backItem = ItemBuilder.of(Material.ARROW)
                .name("Back page")
                .build();

        setItem(39, backItem, ctx -> {
            if (pagination.hasPreviousPage()){
                pagination.previousPage();
                update(player);
            }
        });

        ItemStack close = ItemBuilder.of(Material.RED_DYE)
                .name("Close")
                .build();

        setItem(40, close, MenuContext::close);
    }


    @Override
    public void update(Player player) {
        clear();
        render();
    }
}

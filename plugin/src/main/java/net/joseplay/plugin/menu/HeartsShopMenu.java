package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuItem;
import net.joseplay.api.menu.Pagination;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.shop.HeartsShop;
import net.joseplay.plugin.shop.HeartsShopItem;
import net.joseplay.plugin.shop.HeartsShopService;
import net.joseplay.plugin.shop.ShopLayout;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HeartsShopMenu extends Menu {
    private final Player player;
    private Couple couple;
    private final HeartsShop shop;
    private final HeartsShopService shopService;
    private final MarriageMenuManager menuManager;
    private final Pagination<HeartsShopItem> pagination;
    private final ShopLayout layout;

    public HeartsShopMenu(
            Player player,
            Couple couple,
            HeartsShop shop,
            HeartsShopService shopService,
            MarriageMenuManager menuManager
    ) {
        super("&c&l♥ &8Hearts Shop", 6);
        this.player = player;
        this.couple = couple;
        this.shop = shop;
        this.shopService = shopService;
        this.menuManager = menuManager;
        this.layout = shop.getLayout();

        int slotsPerPage = layout.getProductSlots().size();
        this.pagination = new Pagination<>(shop.getItemList(), slotsPerPage);

        render();
    }

    public void setCouple(Couple couple) {
        this.couple = couple;
    }

    public Pagination<HeartsShopItem> getPagination() {
        return pagination;
    }

    public void render() {
        clear();

        ItemStack filler = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideToolTip(true)
                .build();
        fill(filler);

        List<Integer> productSlots = layout.getProductSlots();
        List<HeartsShopItem> pageItems = pagination.getItemsForCurrentPage();

        for (int i = 0; i < productSlots.size(); i++) {
            int slot = productSlots.get(i);
            if (i < pageItems.size()) {
                HeartsShopItem item = pageItems.get(i);
                ItemStack itemStack = item.createItemStack(player, couple);

                setItem(slot, itemStack, context -> {
                    shopService.purchase(player, item).thenAccept(success -> {
                        if (success) {
                            menuManager.getMarriageService().getCouple(player.getUniqueId()).thenAccept(optCouple -> {
                                optCouple.ifPresent(this::setCouple);
                                player.getServer().getScheduler().runTask(
                                        menuManager.getPlugin(),
                                        () -> update(player)
                                );
                            });
                        }
                    });
                });
            } else {
                ItemStack emptyProductSlot = ItemBuilder.of(Material.PINK_STAINED_GLASS_PANE)
                        .name("&d♥")
                        .hideToolTip(true)
                        .build();
                setItem(slot, emptyProductSlot);
            }
        }

        renderNavigationBar();
    }

    private void renderNavigationBar() {
        // Slot 45: Back button
        ItemStack backItem = ItemBuilder.of(Material.ARROW)
                .name("&e« Back to Marriage Menu")
                .lore("&7Return to your main marriage overview.")
                .build();
        setItem(45, backItem, context -> menuManager.openMarriageMenu(player));

        // Slot 48: Previous page button
        if (pagination.hasPreviousPage()) {
            ItemStack prevItem = ItemBuilder.of(Material.SPECTRAL_ARROW)
                    .name("&a« Previous Page")
                    .lore("&7Go to page " + (pagination.getCurrentPage() - 1))
                    .build();
            setItem(48, prevItem, context -> {
                pagination.previousPage();
                update(player);
            });
        } else {
            ItemStack disabledPrev = ItemBuilder.of(Material.BARRIER)
                    .name("&7« Previous Page")
                    .lore("&8No previous page available.")
                    .build();
            setItem(48, disabledPrev);
        }

        // Slot 49: Current page indicator and Hearts balance
        int currentHearts = 0;
        if (couple != null) {
            Integer h = couple.features().get("hearts");
            if (h != null) currentHearts = h;
        }

        ItemStack pageInfo = ItemBuilder.of(Material.NETHER_STAR)
                .name("&ePage &6" + pagination.getCurrentPage() + " &e/ &6" + pagination.getTotalPages())
                .lore(
                        "&7Your Hearts: &c♥ " + currentHearts,
                        "&7Total Products: &f" + pagination.getTotalItems(),
                        "",
                        "&8Click products in the heart layout to purchase!"
                )
                .build();
        setItem(49, pageInfo);

        // Slot 50: Next page button
        if (pagination.hasNextPage()) {
            ItemStack nextItem = ItemBuilder.of(Material.SPECTRAL_ARROW)
                    .name("&aNext Page »")
                    .lore("&7Go to page " + (pagination.getCurrentPage() + 1))
                    .build();
            setItem(50, nextItem, context -> {
                pagination.nextPage();
                update(player);
            });
        } else {
            ItemStack disabledNext = ItemBuilder.of(Material.BARRIER)
                    .name("&7Next Page »")
                    .lore("&8No next page available.")
                    .build();
            setItem(50, disabledNext);
        }

        // Slot 53: Close button
        ItemStack closeItem = ItemBuilder.of(Material.RED_DYE)
                .name("&cClose")
                .lore("&7Close the Hearts shop.")
                .build();
        setItem(53, closeItem, context -> context.close());
    }

    @Override
    public void update(Player player) {
        render();
        player.updateInventory();
    }
}

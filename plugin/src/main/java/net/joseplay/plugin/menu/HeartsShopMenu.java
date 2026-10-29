package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.Pagination;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.config.MenuConfig;
import net.joseplay.plugin.shop.HeartsShop;
import net.joseplay.plugin.shop.HeartsShopItem;
import net.joseplay.plugin.shop.HeartsShopService;
import net.joseplay.plugin.shop.ShopLayout;
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
    private final MenuConfig menuConfig;

    public HeartsShopMenu(
            Player player,
            Couple couple,
            HeartsShop shop,
            HeartsShopService shopService,
            MarriageMenuManager menuManager
    ) {
        super(menuManager.getMenuConfig().title("hearts-shop", "&c&l♥ &8Hearts Shop"),
                menuManager.getMenuConfig().rows("hearts-shop", 6));
        this.player = player;
        this.couple = couple;
        this.shop = shop;
        this.shopService = shopService;
        this.menuManager = menuManager;
        this.layout = shop.getLayout();
        this.menuConfig = menuManager.getMenuConfig();

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

        ItemStack filler = menuConfig.filler("hearts-shop");
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
                setItem(slot, menuConfig.item("hearts-shop", "empty-product"));
            }
        }

        renderNavigationBar();
    }

    private void renderNavigationBar() {
        int currentHearts = 0;
        if (couple != null) {
            Integer hearts = couple.features().get("hearts");
            if (hearts != null) {
                currentHearts = hearts;
            }
        }

        String[] navPlaceholders = {
                "%page%", String.valueOf(pagination.getCurrentPage()),
                "%pages%", String.valueOf(pagination.getTotalPages()),
                "%hearts%", String.valueOf(currentHearts),
                "%total%", String.valueOf(pagination.getTotalItems())
        };

        setItem(
                menuConfig.slot("hearts-shop", "back", 45),
                menuConfig.item("hearts-shop", "back", navPlaceholders),
                context -> menuManager.openMarriageMenu(player)
        );

        if (pagination.hasPreviousPage()) {
            setItem(
                    menuConfig.slot("hearts-shop", "previous", 48),
                    menuConfig.item("hearts-shop", "previous",
                            "%page%", String.valueOf(pagination.getCurrentPage() - 1)),
                    context -> {
                        pagination.previousPage();
                        update(player);
                    }
            );
        } else {
            setItem(
                    menuConfig.slot("hearts-shop", "previous-disabled", 48),
                    menuConfig.item("hearts-shop", "previous-disabled")
            );
        }

        setItem(
                menuConfig.slot("hearts-shop", "page-info", 49),
                menuConfig.item("hearts-shop", "page-info", navPlaceholders)
        );

        if (pagination.hasNextPage()) {
            setItem(
                    menuConfig.slot("hearts-shop", "next", 50),
                    menuConfig.item("hearts-shop", "next",
                            "%page%", String.valueOf(pagination.getCurrentPage() + 1)),
                    context -> {
                        pagination.nextPage();
                        update(player);
                    }
            );
        } else {
            setItem(
                    menuConfig.slot("hearts-shop", "next-disabled", 50),
                    menuConfig.item("hearts-shop", "next-disabled")
            );
        }

        setItem(
                menuConfig.slot("hearts-shop", "close", 53),
                menuConfig.item("hearts-shop", "close"),
                context -> context.close()
        );
    }

    @Override
    public void update(Player player) {
        render();
        player.updateInventory();
    }
}

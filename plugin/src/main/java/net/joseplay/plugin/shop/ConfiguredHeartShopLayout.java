package net.joseplay.plugin.shop;

import net.joseplay.plugin.config.MenuConfig;

import java.util.ArrayList;
import java.util.List;

public class ConfiguredHeartShopLayout implements ShopLayout {
    private final MenuConfig menuConfig;
    private volatile List<Integer> productSlots = HeartShopLayout.HEART_SLOTS;
    private volatile int rows = 6;

    public ConfiguredHeartShopLayout(MenuConfig menuConfig) {
        this.menuConfig = menuConfig;
        reload();
    }

    @Override
    public List<Integer> getProductSlots() {
        return productSlots;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public void reload() {
        if (menuConfig == null) {
            this.productSlots = HeartShopLayout.HEART_SLOTS;
            this.rows = 6;
            return;
        }

        this.rows = Math.max(1, Math.min(6, menuConfig.rows("hearts-shop", 6)));
        List<Integer> configured = menuConfig.integerList("hearts-shop.product-slots", HeartShopLayout.HEART_SLOTS);
        this.productSlots = List.copyOf(new ArrayList<>(configured));
    }
}

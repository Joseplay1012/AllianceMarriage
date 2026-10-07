package net.joseplay.plugin.shop;

import java.util.List;

public class HeartShopLayout implements ShopLayout {
    private static final List<Integer> HEART_SLOTS = List.of(
            2, 3, 5, 6,
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            29, 30, 31, 32, 33,
            39, 40, 41
    );

    @Override
    public List<Integer> getProductSlots() {
        return HEART_SLOTS;
    }

    @Override
    public int getRows() {
        return 6;
    }
}

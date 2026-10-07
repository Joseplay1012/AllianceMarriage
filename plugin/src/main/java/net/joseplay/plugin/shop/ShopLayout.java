package net.joseplay.plugin.shop;

import java.util.List;

public interface ShopLayout {
    List<Integer> getProductSlots();

    int getRows();

    default int getInventorySize() {
        return getRows() * 9;
    }

    default boolean isProductSlot(int slot) {
        return getProductSlots().contains(slot);
    }
}

package net.joseplay.plugin.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShopLayoutTest {

    @Test
    void testHeartLayoutSlots() {
        HeartShopLayout layout = new HeartShopLayout();
        assertEquals(6, layout.getRows());
        assertEquals(54, layout.getInventorySize());

        List<Integer> slots = layout.getProductSlots();
        assertFalse(slots.isEmpty());
        assertEquals(26, slots.size());

        // Ensure all slots are between 0 and 44 (rows 0 to 4), leaving row 5 (45..53) for navigation
        for (int slot : slots) {
            assertTrue(slot >= 0 && slot < 45, "Slot " + slot + " must be in rows 0..4");
            assertTrue(layout.isProductSlot(slot));
        }

        // Navigation row (45..53) must not have any product slots
        for (int navSlot = 45; navSlot < 54; navSlot++) {
            assertFalse(layout.isProductSlot(navSlot), "Navigation slot " + navSlot + " must not be a product slot");
        }

        // Verify key slots in heart shape
        assertTrue(slots.contains(2));
        assertTrue(slots.contains(3));
        assertTrue(slots.contains(5));
        assertTrue(slots.contains(6));
        assertTrue(slots.contains(13)); // Center heart
        assertTrue(slots.contains(40)); // Lower heart tip
    }
}

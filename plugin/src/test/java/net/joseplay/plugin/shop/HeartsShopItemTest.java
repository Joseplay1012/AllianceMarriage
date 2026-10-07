package net.joseplay.plugin.shop;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeartsShopItemTest {

    @Test
    void testPlaceholderFormatting() {
        String template = "give %player% %item% 1 with %price% hearts for %uuid% (balance: %hearts%)";
        String formatted = HeartsShopItem.formatPlaceholders(
                template,
                "Alex",
                "1234-5678",
                "50",
                "diamond",
                "250"
        );

        assertEquals("give Alex diamond 1 with 50 hearts for 1234-5678 (balance: 250)", formatted);
    }

    @Test
    void testItemProperties() {
        HeartsShopItem item = new HeartsShopItem(
                "diamond",
                Material.DIAMOND,
                2,
                75,
                "AUTO",
                "&bDiamond",
                List.of("&7Lore 1", "&ePrice: &c♥ %price%"),
                false,
                false,
                0,
                Collections.emptyList(),
                Collections.emptyMap(),
                List.of("give %player% diamond 2")
        );

        assertEquals("diamond", item.getId());
        assertEquals(Material.DIAMOND, item.getMaterial());
        assertEquals(2, item.getAmount());
        assertEquals(75, item.getPrice());
        assertTrue(item.isAutoSlot());
        assertNull(item.getExplicitSlot());
        assertEquals("&bDiamond", item.getName());
        assertEquals(2, item.getLore().size());
        assertEquals(1, item.getCommands().size());
        assertEquals("give %player% diamond 2", item.getCommands().get(0));
    }

    @Test
    void testExplicitSlot() {
        HeartsShopItem item = new HeartsShopItem(
                "custom",
                Material.GOLD_INGOT,
                1,
                10,
                "14",
                "Gold",
                List.of(),
                false,
                false,
                0,
                List.of(),
                Collections.emptyMap(),
                List.of("give %player% gold_ingot 1")
        );

        assertFalse(item.isAutoSlot());
        assertEquals(14, item.getExplicitSlot());
    }
}

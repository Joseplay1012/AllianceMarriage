package net.joseplay.plugin.cooldown;

import net.joseplay.api.cooldown.CooldownManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CooldownManagerTest {

    private CooldownManager manager;
    private UUID player;

    @BeforeEach
    void setUp() {
        manager = new CooldownManager();
        player = UUID.randomUUID();
    }

    @Test
    void shouldSetCooldown() {
        long expiration = manager.setCooldown(player, "teleport", 10);

        assertTrue(expiration > System.currentTimeMillis());
        assertTrue(manager.isOnCooldown(player, "teleport"));
    }

    @Test
    void shouldReturnRemainingTime() {
        manager.setCooldown(player, "teleport", 10);

        long remaining = manager.getRemainingTime(player, "teleport");

        assertTrue(remaining > 0 && remaining <= 10);
    }

    @Test
    void shouldRemoveCooldown() {
        manager.setCooldown(player, "teleport", 10);

        assertTrue(manager.removeCooldown(player, "teleport"));
        assertFalse(manager.isOnCooldown(player, "teleport"));
    }

    @Test
    void shouldClearAllCooldowns() {
        manager.setCooldown(player, "teleport", 10);
        manager.setCooldown(player, "combat", 30);

        assertTrue(manager.clearCooldown(player));

        assertFalse(manager.isOnCooldown(player, "teleport"));
        assertFalse(manager.isOnCooldown(player, "combat"));
    }

    @Test
    void shouldReturnFalseWhenCooldownDoesNotExist() {
        assertFalse(manager.isOnCooldown(player, "teleport"));
        assertEquals(0, manager.getRemainingTime(player, "teleport"));
    }

    @Test
    void shouldSupportDifferentCooldownTypes() {
        manager.setCooldown(player, "teleport", 10);
        manager.setCooldown(player, "combat", 30);

        assertTrue(manager.isOnCooldown(player, "teleport"));
        assertTrue(manager.isOnCooldown(player, "combat"));

        assertTrue(manager.removeCooldown(player, "teleport"));

        assertFalse(manager.isOnCooldown(player, "teleport"));
        assertTrue(manager.isOnCooldown(player, "combat"));
    }
}


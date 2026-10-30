
package net.joseplay.api.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldownMap =
            new ConcurrentHashMap<>();

    public long setCooldown(UUID player, String typeId, int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Seconds cannot be negative");
        }

        long expiration = System.currentTimeMillis() + (seconds * 1000L);

        cooldownMap.computeIfAbsent(player, key -> new ConcurrentHashMap<>())
                .put(typeId, expiration);

        return expiration;
    }

    public boolean isOnCooldown(UUID player, String typeId) {
        Map<String, Long> cooldowns = cooldownMap.get(player);

        if (cooldowns == null) {
            return false;
        }

        Long expiration = cooldowns.get(typeId);

        return expiration != null
                && System.currentTimeMillis() < expiration;
    }

    public long getRemainingTime(UUID player, String typeId) {
        Map<String, Long> cooldowns = cooldownMap.get(player);

        if (cooldowns == null) {
            return 0;
        }

        Long expiration = cooldowns.get(typeId);

        if (expiration == null) {
            return 0;
        }

        long remaining = expiration - System.currentTimeMillis();

        return remaining <= 0 ? 0 : (remaining + 999) / 1000;
    }

    public boolean removeCooldown(UUID player, String typeId) {
        Map<String, Long> cooldowns = cooldownMap.get(player);

        if (cooldowns == null) {
            return false;
        }

        boolean removed = cooldowns.remove(typeId) != null;

        if (cooldowns.isEmpty()) {
            cooldownMap.remove(player, cooldowns);
        }

        return removed;
    }

    public boolean clearCooldown(UUID player) {
        return cooldownMap.remove(player) != null;
    }
}
package net.joseplay.api.menu;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager implements Listener {
    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();

    public void shutdown() {
        for  (Menu menu : openMenus.values()) {
            menu.getInventory();
            menu.getInventory().clear();
        }
    }

    public void open(Player player, Menu menu) {
        if (player == null || menu == null) {
            return;
        }

        player.closeInventory();
        openMenus.put(player.getUniqueId(), menu);
        menu.open(player);
    }

    public void close(Player player) {
        if (player == null) {
            return;
        }
        openMenus.remove(player.getUniqueId());
        player.closeInventory();
    }

    public Optional<Menu> getOpenMenu(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return getOpenMenu(player.getUniqueId());
    }

    public Optional<Menu> getOpenMenu(UUID uuid) {
        return Optional.ofNullable(openMenus.get(uuid));
    }

    public boolean hasMenuOpen(Player player) {
        return player != null && openMenus.containsKey(player.getUniqueId());
    }

    public void update(Player player) {
        if (player == null) {
            return;
        }
        Menu menu = openMenus.get(player.getUniqueId());
        if (menu != null) {
            menu.update(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }

        if (event.getInventory().getHolder() != menu && !event.getView().getTopInventory().equals(menu.getInventory())) {
            return;
        }

        int rawSlot = event.getRawSlot();
        int menuSize = menu.getSize();

        if (rawSlot >= 0 && rawSlot < menuSize) {
            MenuItem item = menu.getItem(rawSlot);
            if (menu.isCancelClicksByDefault() || (item != null && item.isCancelClick())) {
                event.setCancelled(true);
            }

            MenuContext context = new MenuContext(player, menu, rawSlot, event);
            menu.handleClick(context);
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Menu menu = openMenus.remove(player.getUniqueId());
        if (menu != null) {
            menu.handleClose(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return;
        }

        int menuSize = menu.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < menuSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
    }
}

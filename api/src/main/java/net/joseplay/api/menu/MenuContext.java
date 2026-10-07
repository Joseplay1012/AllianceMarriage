package net.joseplay.api.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MenuContext {
    private final Player player;
    private final Menu menu;
    private final int slot;
    private final InventoryClickEvent clickEvent;
    private final Map<String, Object> metadata;

    public MenuContext(Player player, Menu menu, int slot, InventoryClickEvent clickEvent) {
        this(player, menu, slot, clickEvent, new HashMap<>());
    }

    public MenuContext(Player player, Menu menu, int slot, InventoryClickEvent clickEvent, Map<String, Object> metadata) {
        this.player = player;
        this.menu = menu;
        this.slot = slot;
        this.clickEvent = clickEvent;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public Player getPlayer() {
        return player;
    }

    public Menu getMenu() {
        return menu;
    }

    public int getSlot() {
        return slot;
    }

    public InventoryClickEvent getClickEvent() {
        return clickEvent;
    }

    public ClickType getClickType() {
        return clickEvent != null ? clickEvent.getClick() : null;
    }

    public ItemStack getClickedItem() {
        return clickEvent != null ? clickEvent.getCurrentItem() : null;
    }

    public boolean isLeftClick() {
        return clickEvent != null && clickEvent.isLeftClick();
    }

    public boolean isRightClick() {
        return clickEvent != null && clickEvent.isRightClick();
    }

    public boolean isShiftClick() {
        return clickEvent != null && clickEvent.isShiftClick();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> clazz) {
        Object val = metadata.get(key);
        if (val != null && clazz.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }

    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    public Map<String, Object> getAllMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void close() {
        player.closeInventory();
    }

    public void update() {
        menu.update(player);
    }
}

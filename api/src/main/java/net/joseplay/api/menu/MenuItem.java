package net.joseplay.api.menu;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MenuItem {
    private ItemStack itemStack;
    private int slot = -1;
    private MenuAction action;
    private boolean cancelClick = true;
    private final Map<String, Object> metadata = new HashMap<>();

    public MenuItem(ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack cannot be null");
    }

    public MenuItem(ItemStack itemStack, MenuAction action) {
        this(itemStack);
        this.action = action;
    }

    public MenuItem(int slot, ItemStack itemStack, MenuAction action) {
        this(itemStack, action);
        this.slot = slot;
    }

    public static MenuItem of(ItemStack itemStack) {
        return new MenuItem(itemStack);
    }

    public static MenuItem of(ItemStack itemStack, MenuAction action) {
        return new MenuItem(itemStack, action);
    }

    public static MenuItem of(int slot, ItemStack itemStack, MenuAction action) {
        return new MenuItem(slot, itemStack, action);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack cannot be null");
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public MenuAction getAction() {
        return action;
    }

    public void setAction(MenuAction action) {
        this.action = action;
    }

    public boolean isCancelClick() {
        return cancelClick;
    }

    public void setCancelClick(boolean cancelClick) {
        this.cancelClick = cancelClick;
    }

    public MenuItem cancelClick(boolean cancelClick) {
        this.cancelClick = cancelClick;
        return this;
    }

    public MenuItem withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object val = metadata.get(key);
        if (val != null && type.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }

    public Map<String, Object> getMetadataMap() {
        return metadata;
    }
}

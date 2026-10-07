package net.joseplay.api.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Menu implements InventoryHolder {
    private final String title;
    private final int rows;
    private final Map<Integer, MenuItem> items = new HashMap<>();
    private Inventory inventory;
    private boolean cancelClicksByDefault = true;
    private Consumer<Player> closeHandler;
    private Consumer<Player> openHandler;

    public Menu(String title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Menu rows must be between 1 and 6, got " + rows);
        }
        this.title = title != null ? ChatColor.translateAlternateColorCodes('&', title) : "";
        this.rows = rows;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public int getSize() {
        return rows * 9;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, getSize(), title);
        }
        return inventory;
    }

    public void setItem(int slot, MenuItem item) {
        if (slot < 0 || slot >= getSize()) {
            throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds for menu size " + getSize());
        }
        if (item == null) {
            removeItem(slot);
            return;
        }
        item.setSlot(slot);
        items.put(slot, item);
        getInventory().setItem(slot, item.getItemStack());
    }

    public void setItem(int slot, ItemStack itemStack) {
        if (itemStack == null) {
            removeItem(slot);
            return;
        }
        setItem(slot, new MenuItem(itemStack));
    }

    public void setItem(int slot, ItemStack itemStack, MenuAction action) {
        if (itemStack == null) {
            removeItem(slot);
            return;
        }
        setItem(slot, new MenuItem(slot, itemStack, action));
    }

    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    public void removeItem(int slot) {
        items.remove(slot);
        if (inventory != null && slot >= 0 && slot < getSize()) {
            inventory.setItem(slot, null);
        }
    }

    public void clear() {
        items.clear();
        if (inventory != null) {
            inventory.clear();
        }
    }

    public void fill(ItemStack itemStack) {
        fill(itemStack, null);
    }

    public void fill(ItemStack itemStack, MenuAction action) {
        for (int i = 0; i < getSize(); i++) {
            if (!items.containsKey(i)) {
                setItem(i, itemStack, action);
            }
        }
    }

    public void fillBorder(ItemStack itemStack) {
        fillBorder(itemStack, null);
    }

    public void fillBorder(ItemStack itemStack, MenuAction action) {
        int size = getSize();
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                if (!items.containsKey(i)) {
                    setItem(i, itemStack, action);
                }
            }
        }
    }

    public Map<Integer, MenuItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public boolean isCancelClicksByDefault() {
        return cancelClicksByDefault;
    }

    public void setCancelClicksByDefault(boolean cancelClicksByDefault) {
        this.cancelClicksByDefault = cancelClicksByDefault;
    }

    public void setCloseHandler(Consumer<Player> closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void setOpenHandler(Consumer<Player> openHandler) {
        this.openHandler = openHandler;
    }

    public void open(Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        handleOpen(player);
        player.openInventory(getInventory());
    }

    public void update(Player player) {
        // Can be overridden by subclasses to redraw dynamic content
    }

    public void handleClick(MenuContext context) {
        MenuItem menuItem = items.get(context.getSlot());
        if (menuItem != null && menuItem.getAction() != null) {
            menuItem.getAction().execute(context);
        }
    }

    public void handleClose(Player player) {
        if (closeHandler != null) {
            closeHandler.accept(player);
        }
    }

    public void handleOpen(Player player) {
        if (openHandler != null) {
            openHandler.accept(player);
        }
    }
}

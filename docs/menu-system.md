# Menu & GUI Framework Guide

The **AllianceMarriage Menu System** is an inventory GUI framework located in the `api` module (`net.joseplay.api.menu`). It allows developers to create custom chest menus by registering click handlers directly on items rather than manually managing Bukkit event listeners.

**English** | [Português (Brasil)](pt-br/menu-system.md)

---

## Core Interfaces & Classes

### 1. `Menu` (Abstract Base Class)
Located at: [`Menu.java`](../api/src/main/java/net/joseplay/api/menu/Menu.java)

The base class for all custom chest menus. It implements Bukkit's `InventoryHolder`, meaning Spigot recognizes the menu instance as the inventory's holder.

#### Key Properties:
- `title`: Display title of the inventory (translates color codes with `&`).
- `rows`: Number of chest rows (from 1 to 6). Total slots = `rows * 9`.
- `cancelClicksByDefault`: Defaults to `true`. When active, prevents players from picking up or moving items inside the menu.

#### Main Methods:
```java
// Set an item at an exact slot with a click action
public void setItem(int slot, ItemStack itemStack, MenuAction action)

// Set an item using the MenuItem wrapper
public void setItem(int slot, MenuItem item)

// Fill remaining empty slots with a filler item
public void fill(ItemStack itemStack)

// Fill the outer borders of the chest inventory
public void fillBorder(ItemStack itemStack)

// Clear all items from the menu
public void clear()

// Open the menu for a player
public void open(Player player)

// Hook for redrawing dynamic content
public void update(Player player)
```

---

### 2. `MenuItem`
Located at: [`MenuItem.java`](../api/src/main/java/net/joseplay/api/menu/MenuItem.java)

Combines an `ItemStack` with its slot position, click cancellation flag, and its click callback (`MenuAction`).

```java
// Static icon with no action
MenuItem icon = MenuItem.of(new ItemStack(Material.PAPER));

// Interactive button
MenuItem button = MenuItem.of(itemStack, context -> {
    context.getPlayer().sendMessage("Button clicked!");
});
```

---

### 3. `MenuAction` (Functional Interface)
Located at: [`MenuAction.java`](../api/src/main/java/net/joseplay/api/menu/MenuAction.java)

```java
@FunctionalInterface
public interface MenuAction {
    void execute(MenuContext context);
}
```
Functional interface executed when a player clicks a slot with an assigned action. It receives a `MenuContext` object with event details.

---

### 4. `MenuContext`
Located at: [`MenuContext.java`](../api/src/main/java/net/joseplay/api/menu/MenuContext.java)

Provides click event information and convenience methods:

| Method | Description |
| :--- | :--- |
| `context.getPlayer()` | Returns the `Player` who clicked. |
| `context.getMenu()` | Returns the parent `Menu` instance. |
| `context.getSlot()` | Returns the raw slot index clicked. |
| `context.getClickType()` | Returns the Bukkit `ClickType` (e.g. `LEFT`, `RIGHT`, `SHIFT_LEFT`). |
| `context.close()` | Closes the player's current inventory. |
| `context.update()` | Invokes `menu.update(player)`. |

---

### 5. `MenuManager`
Located at: [`MenuManager.java`](../api/src/main/java/net/joseplay/api/menu/MenuManager.java)

Listener and lifecycle manager for all open menus. It tracks open inventories using a thread-safe map:
```java
Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
```

#### Event Handling:
1. **Click Cancellation**: If `isCancelClicksByDefault()` is true, cancels clicks inside the menu slots (`0 <= rawSlot < menuSize`).
2. **Shift-Click Handling**: Cancels shift-clicks originating from the player's bottom inventory into the top menu.
3. **Drag Cancellation**: Cancels `InventoryDragEvent` if any affected slot is within the top menu inventory.
4. **Disconnect Cleanup**: Removes player references from `openMenus` on `PlayerQuitEvent`.

---

### 6. `Pagination<T>`
Located at: [`Pagination.java`](../api/src/main/java/net/joseplay/api/menu/Pagination.java)

A generic pagination utility that calculates page slices for any `List<T>`.

#### Usage Example:
```java
// Create a paginator with 10 items per page
Pagination<HeartsShopItem> pagination = new Pagination<>(allItems, 10);

// Get items for the current page
List<HeartsShopItem> pageItems = pagination.getItemsForCurrentPage();

// Navigation methods
if (pagination.hasNextPage()) {
    pagination.nextPage();
}

if (pagination.hasPreviousPage()) {
    pagination.previousPage();
}

String info = "Page " + pagination.getCurrentPage() + " / " + pagination.getTotalPages();
```

---

## Built-in Plugin Menus

AllianceMarriage includes four menu implementations:

### 1. `MarriageMenu` (Main Overview)
Located at: [`MarriageMenu.java`](../plugin/src/main/java/net/joseplay/plugin/menu/MarriageMenu.java)

- **Married State**:
  - Displays the partner's head (`PLAYER_HEAD` with skull owner).
  - Displays relationship duration calculated via `Duration.between(anniversary, now)`.
  - Displays the couple's shared Hearts balance (`couple.features().get("hearts")`).
  - Button to open the Hearts Shop.
  - Button to divorce.
- **Unmarried State**:
  - Displays a barrier item explaining how to use `/marry <player>`.

### 2. `HeartsShopMenu` (Shop GUI)
Located at: [`HeartsShopMenu.java`](../plugin/src/main/java/net/joseplay/plugin/menu/HeartsShopMenu.java)

- Arranges product slots using [`HeartShopLayout.java`](../plugin/src/main/java/net/joseplay/plugin/shop/HeartShopLayout.java):
  - Row 1: slots `2, 3, 5, 6`
  - Row 2: slots `10, 11, 12, 13, 14, 15, 16`
  - Row 3: slots `19, 20, 21, 22, 23, 24, 25`
  - Row 4: slots `29, 30, 31, 32, 33`
  - Row 5: slots `39, 40, 41`
- Row 6 is used for navigation:
  - Slot 45: Back button
  - Slot 48: Previous page
  - Slot 49: Current page and Hearts balance indicator
  - Slot 50: Next page
  - Slot 53: Close button
- Interacts with `HeartsShopService` to execute purchases.

### 3. `CoupleSettings` (Preferences Menu)
Located at: [`CoupleSettings.java`](../plugin/src/main/java/net/joseplay/plugin/menu/CoupleSettings.java)

- Displays registered preferences from `PreferencesManager`.
- Places items in designated interactive slots (`SLOTS = [10, 12, 14, 16, 20, 22, 24, 28, 30, 32, 34]`).
- Supports pagination across multiple pages of preferences.

### 4. `MarriageRequestMenu` (Proposal Pop-up)
Located at: [`MarriageRequestMenu.java`](../plugin/src/main/java/net/joseplay/plugin/menu/MarriageRequestMenu.java)

- A 3-row menu opened for the target player when proposed to:
  - Center: Proposer's player skull.
  - Slot 11: Lime concrete (Accept).
  - Slot 15: Red concrete (Deny).

---

## Example: Creating a Custom Menu

```java
package net.joseplay.example;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuContext;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomSampleMenu extends Menu {

    public CustomSampleMenu(Player player) {
        super("&8Sample Menu", 3); // 3 rows (27 slots)
        render(player);
    }

    private void render(Player player) {
        // Border decoration
        ItemStack border = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        fillBorder(border);

        // Center button
        ItemStack actionItem = ItemBuilder.of(Material.EMERALD)
                .name("&aAction Button")
                .lore("&7Click to run action.")
                .build();

        setItem(13, actionItem, context -> {
            context.getPlayer().sendMessage("Action executed!");
            context.close();
        });

        // Close button
        ItemStack closeItem = ItemBuilder.of(Material.BARRIER)
                .name("&cClose")
                .build();
        setItem(26, closeItem, MenuContext::close);
    }
}
```

### Opening the Menu:
```java
MenuManager menuManager = plugin.getMarriageMenuManager().getMenuManager();
menuManager.open(player, new CustomSampleMenu(player));
```

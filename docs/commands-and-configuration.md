# Commands & Configuration

This guide details command syntax, permissions, and the YAML structure of the Hearts Shop configuration file.

**English** | [Português (Brasil)](pt-br/commands-and-configuration.md)

---

## Command Reference: `/marriage`

Command executor: [`MarriageCommands.java`](../plugin/src/main/java/net/joseplay/plugin/commands/MarriageCommands.java)

| Subcommand | Arguments | Permission | Description |
| :--- | :--- | :--- | :--- |
| `/marriage` | *(none)* | None (default) | Opens the main marriage menu ([`MarriageMenu.java`](../plugin/src/main/java/net/joseplay/plugin/menu/MarriageMenu.java)). |
| `/marriage menu` | *(none)* | None (default) | Opens the main marriage menu. |
| `/marriage shop` | *(none)* | None (default) | Opens the Hearts Shop ([`HeartsShopMenu.java`](../plugin/src/main/java/net/joseplay/plugin/menu/HeartsShopMenu.java)). |
| `/marriage marry` | `<player>` | None (default) | Sends a marriage proposal to an online player. |
| `/marriage accept`| *(none)* | None (default) | Accepts an incoming marriage proposal. |
| `/marriage deny` | *(none)* | None (default) | Declines an incoming marriage proposal. |
| `/marriage couple`| *(none)* | None (default) | Opens the preferences menu ([`CoupleSettings.java`](../plugin/src/main/java/net/joseplay/plugin/menu/CoupleSettings.java)). |
| `/marriage divorce`| *(none)* | None (default) | Divorces the current partner. |
| `/marriage reload` | *(none)* | `marry.admin` or OP | Reloads [`hearts-shop.yml`](../plugin/src/main/resources/hearts-shop.yml) from disk. |

### Tab-Completion:
- `/marriage <TAB>` completes subcommands (`menu`, `shop`, `marry`, `accept`, `deny`, `couple`, `divorce`, `reload`).
- `/marriage marry <TAB>` completes online players (excluding the command sender).

---

## Permissions

| Node | Default | Description |
| :--- | :--- | :--- |
| `marry.admin` | `op` | Allows reloading configuration via `/marriage reload`. |

---

## Shop Configuration: `hearts-shop.yml`
File path: [`plugin/src/main/resources/hearts-shop.yml`](../plugin/src/main/resources/hearts-shop.yml)

### Item Schema:

```yaml
items:
  <item_id>:
    material: <MATERIAL_NAME>      # Bukkit Material enum name (e.g. DIAMOND, CAKE)
    amount: <int>                  # Item stack amount (default: 1)
    slot: AUTO                     # "AUTO" uses sequential heart-layout slots, or specify slot index (0-44)
    price: <int>                   # Cost in Hearts
    display:
      name: "<text>"               # Item name (supports '&' color codes)
      lore:                        # Item lore list
        - "&7Description text"
        - "&ePrice: &c♥ %price%"
      glow: <true|false>           # Adds enchantment glint without enchantment text
      unbreakable: <true|false>    # Sets unbreaking item flag
      custom-model-data: <int>     # CustomModelData integer (0 for none)
      hide-flags:                  # ItemFlag names to apply
        - ATTRIBUTES
        - ENCHANTS
    commands:                      # Console commands executed upon purchase
      - "give %player% diamond 1"
```

---

## Placeholders

Placeholders supported in item names, lores, and commands:

| Placeholder | Replaced With |
| :--- | :--- |
| `%player%` | Player's username. |
| `%uuid%` | Player's UUID string. |
| `%price%` | Item price in Hearts. |
| `%item%` | Item ID key from configuration. |
| `%hearts%` | Couple's remaining Hearts balance after transaction. |

---

## Example Configuration Entry

```yaml
items:
  speed_potion:
    material: POTION
    amount: 1
    slot: AUTO
    price: 60
    display:
      name: "&bSwiftness Potion"
      lore:
        - "&7Grants Speed II for 3 minutes."
        - "&ePrice: &c♥ %price%"
    commands:
      - "effect give %player% speed 180 1"
```

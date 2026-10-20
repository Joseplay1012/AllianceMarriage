# AllianceMarriage Documentation

Developer documentation for **AllianceMarriage**, a modular Spigot/Paper plugin designed for couple relationships, shared economies, inventory menus, and dynamic couple attributes.

**English** | [Português (Brasil)](pt-br/README.md)

---

## Documentation Index

| Guide | Description | Relevant Paths |
| :--- | :--- | :--- |
| [Architecture Overview](architecture-overview.md) | Multi-module Maven setup, lifecycle, and thread execution | Root & module POMs |
| [Menu & GUI System](menu-system.md) | Inventory GUI framework, pagination, and menu classes | [`api/menu`](../api/src/main/java/net/joseplay/api/menu/) & [`plugin/menu`](../plugin/src/main/java/net/joseplay/plugin/menu/) |
| [Features & Attributes System](features-system.md) | Dynamic couple attributes, atomic increments, and registration | [`core/feature`](../core/src/main/java/net/joseplay/core/feature/) |
| [Storage & Database Layer](storage-and-database.md) | HikariCP pool, SQLite/MySQL support, caching, and repositories | [`core/storage`](../core/src/main/java/net/joseplay/core/storage/) |
| [Services & Business Logic](services-and-business-logic.md) | Marriage proposal lifecycle, Hearts shop purchasing, and listeners | [`plugin/service`](../plugin/src/main/java/net/joseplay/plugin/service/) & [`plugin/shop`](../plugin/src/main/java/net/joseplay/plugin/shop/) |
| [Preferences API](preferences-api.md) | Pluggable settings toggles in `/marriage couple` | [`api/preferences`](../api/src/main/java/net/joseplay/api/preferences/) |
| [Commands & Configuration](commands-and-configuration.md) | Command syntax, permissions, and `hearts-shop.yml` schema | [`plugin/commands`](../plugin/src/main/java/net/joseplay/plugin/commands/) & [`hearts-shop.yml`](../plugin/src/main/resources/hearts-shop.yml) |

---

## Completed Features

- **Modular Maven Structure**:
  - `api`: Interface abstractions (`Menu`, `MenuItem`, `Pagination`, `Preference`) independent of database or gameplay rules.
  - `core`: Domain models (`Couple`), dynamic attributes (`Feature<T>`), database connection pool (`Database`), and persistence implementations.
  - `plugin`: Spigot event listeners, command routing, YAML loading, and concrete chest menus.
- **Asynchronous Database Operations**:
  - Uses HikariCP for connection pooling. Supports embedded SQLite file storage and remote MySQL connections.
  - Queries and updates run asynchronously using `CompletableFuture` on a dedicated thread pool.
  - Couples repository maintains a bi-directional in-memory cache (`coupleMap` and `playerCoupleMap`).
- **Dynamic Typed Feature System**:
  - Attributes stored as key-value pairs in the `couples_features` table via `Feature<T>`.
  - Numeric features (`INTEGER`, `LONG`, `DOUBLE`) use direct SQL arithmetic (`CAST(value AS SIGNED) + CAST(? AS SIGNED)`) to prevent lost updates from concurrent clicks.
  - Default features: `Hearts` (joint currency) and `PvP` (friendly fire toggle).
- **GUI Framework**:
  - Reusable chest GUI classes (`Menu`, `MenuItem`, `MenuContext`) with automated click cancellation, shift-click protection, and drag interception.
  - Generic pagination helper (`Pagination<T>`).
- **Heart-Shaped Shop**:
  - `HeartShopLayout` arranges 26 product slots in a heart shape on a 6-row chest, keeping row 6 for pagination and close buttons.
  - Shop items loaded from [`hearts-shop.yml`](../plugin/src/main/resources/hearts-shop.yml) with command execution, prices, and placeholders (`%player%`, `%uuid%`, `%price%`, `%item%`, `%hearts%`).
  - Purchase lock (`processingPurchases` concurrent set) prevents duplicate purchases before previous transaction finishes.
- **Marriage Proposal Workflow**:
  - Proposals have a 60-second expiration timeout (`MarriageRequest`).
  - Interactive proposal GUI (`MarriageRequestMenu`) with accept and decline buttons.
  - Divorce workflow cleans up database records and in-memory caches.

---

## Technical Advantages

1. **Separation of Concerns**:
   - The GUI engine is isolated in `api`, allowing it to be compiled or tested independently of core data logic.
   - Database operations only interact with domain entities (`Couple`, `StoredFeature`), keeping SQL out of command handlers and menus.
2. **Asynchronous Execution**:
   - Database reads and writes run on background threads via `CompletableFuture`, avoiding main-thread TPS drops during database latency.
3. **Atomic SQL Increments**:
   - Hearts balance additions and subtractions occur via atomic database-level arithmetic, preventing balance desync from rapid consecutive clicks.
4. **Deterministic Partner Ordering**:
   - UUIDs are normalized alphabetically before saving or querying (`partner1` is always lexicographically smaller than `partner2`). This ensures that querying `(PlayerA, PlayerB)` and `(PlayerB, PlayerA)` always resolves to the exact same database row.

---

## Technical Evaluation and Score

### Score: **7.8 / 10**

| Dimension | Rating | Description |
| :--- | :---: | :--- |
| **Architecture & Modularity** | **9.0 / 10** | Clear separation between `api`, `core`, and `plugin`. Entities, services, and repositories follow consistent boundaries. |
| **Concurrency & Storage** | **8.5 / 10** | Database operations run asynchronously via HikariCP. Atomic increments prevent race conditions. |
| **Extensibility** | **8.5 / 10** | Feature Registry and Preferences system allow adding couple attributes and settings buttons without modifying database tables. |
| **Code Structure & Readability** | **8.0 / 10** | Modern Java features (records, lambdas, switch expressions) are used cleanly. |
| **Error Handling & Configuration** | **6.5 / 10** | Database configuration in `PluginSettings` has hardcoded values in code rather than reading from a `config.yml`. |
| **Testing** | **6.0 / 10** | Basic unit tests exist for `Pagination`, `HeartsShopItem`, and `ShopLayout`. The `test` module contains draft code rather than automated JUnit test suites. |

### Areas for Improvement:
1. **Dynamic Configuration File**: Load MySQL host, port, database, and credentials from a server-side `config.yml` rather than the hardcoded defaults in [`PluginSettings.java`](../core/src/main/java/net/joseplay/core/config/PluginSettings.java).
2. **Divorce Confirmation**: Require a second click or confirmation dialog before executing a permanent divorce.
3. **Automated Integration Tests**: Replace the manual scratchpad code in the `test` module with automated integration tests using SQLite in-memory or mock fixtures.

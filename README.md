# AllianceMarriage

A plugin for Spigot/Paper servers (Minecraft 1.20+) that implements a marriage system between players, a shared economy ("Hearts"), a chest-based shop interface with a thematic layout, PvP toggling between spouses, and data persistence via SQLite or MySQL using HikariCP.

**English** | [Portuguese (Brazil)](README.pt-BR.md)

---

## Documentation

All detailed technical documentation is organized in the [`docs/`](docs/README.md) folder:

| Document | Content |
| :--- | :--- |
| [General Index (`docs/README.md`)](docs/README.md) | Overview, list of features, technical assessment, and current limitations |
| [Architecture (`docs/architecture-overview.md`)](docs/architecture-overview.md) | Maven module breakdown (`api`, `core`, `plugin`), execution flow, and concurrency |
| [Menu System (`docs/menu-system.md`)](docs/menu-system.md) | Inventory API (`Menu`, `MenuItem`, `MenuManager`, `Pagination`) and screen implementation |
| [Features System (`docs/features-system.md`)](docs/features-system.md) | Dynamic attributes per couple (`Feature<T>`), registration, and SQL-based increments |
| [Storage and Database (`docs/storage-and-database.md`)](docs/storage-and-database.md) | SQL tables, HikariCP connection pooling, in-memory caching, and UUID normalization |
| [Services and Business Logic (`docs/services-and-business-logic.md`)](docs/services-and-business-logic.md) | Marriage proposals, shop purchase debouncing, and event listeners |
| [Preferences API (`docs/preferences-api.md`)](docs/preferences-api.md) | Registration of custom options for the `/marriage couple` screen |
| [Commands and Configuration (`docs/commands-and-configuration.md`)](docs/commands-and-configuration.md) | `/marriage` syntax, permissions, and the structure of the `hearts-shop.yml` file |

---

## Project Structure

The project is divided into four Maven modules:

- **`api`** ([`AllianceMarriage-api`](api/pom.xml)): Interfaces, GUI utilities (`Menu`, `MenuItem`, `Pagination`), and preference-related contracts. Contains no references to the database or marriage-specific business logic.
- **`core`** ([`AllianceMarriage-core`](core/pom.xml)): The `Couple` entity, the `Feature<T>` attribute engine, the `Database` connection pool (HikariCP), and data repositories (`CouplesRepository`, `FeatureRepository`).
- **`plugin`** ([`AllianceMarriage-plugin`](plugin/pom.xml)): The Bukkit plugin entry point (`Plugin.java`), command registration, event listeners, `hearts-shop.yml` file loading, and concrete GUI implementations.
- **`test`** ([`test`](test/pom.xml)): A standalone command-line application used for rapid testing of persistence and serialization, without the need to launch a Bukkit server.

---

## Implemented Features

- **Proposals and Marriage**: Sending requests via `/marriage marry <player>` with a 60-second response timeout, a graphical confirmation menu (`MarriageRequestMenu`), and divorce via `/marriage divorce`.
- **Shared Economy (Hearts)**: A joint balance stored in the `couples_features` table. Numeric updates utilize direct increments within the database via SQL (`CAST` and `ON DUPLICATE KEY UPDATE` / `ON CONFLICT`).
- **Heart-Shaped Shop Layout**: The `HeartShopLayout` class maps 26 product slots across the first 5 rows of a double chest to form a heart shape, reserving the final row for page navigation. Items are configurable via [`plugin/src/main/resources/hearts-shop.yml`](plugin/src/main/resources/hearts-shop.yml).
- **Friendly Fire Control (PvP)**: A boolean preference managed per couple. When disabled, the `MarriageListeners` class intercepts and cancels `EntityDamageByEntityEvent` instances occurring between the two partners.
- **Local Cache**: `CouplesImpl` maintains concurrent in-memory maps (`coupleMap` and `playerCoupleMap`). Upon a player logging into the server, their couple data is pre-loaded in the background.

---

## Technical Code Review

**Overall Score:** 7.8 / 10

### Strengths
- Clear separation of responsibilities across the API, domain, and server runtime layers.
- SQL queries are executed outside the server's main thread using `CompletableFuture` and `ExecutorService`.
- Normalization of UUID pairs (where `partner1` is always lexicographically smaller than `partner2`) prevents duplicate records, regardless of which player initiated the request.
- Numeric database increments are handled via atomic SQL operations, avoiding the need for prior in-memory reads and preventing concurrency issues (such as those caused by rapid double-clicks). ### Limitations and Technical Debt
- MySQL configurations in [`core/src/main/java/net/joseplay/core/config/PluginSettings.java`](core/src/main/java/net/joseplay/core/config/PluginSettings.java) are hardcoded in the Java source (`root`, `admin`, empty host) and do not read from a `config.yml` file in the plugin folder.
- The divorce action does not request prior confirmation from the player before deleting the bond from the database.
- The `test` module contains draft code with hardcoded UUIDs.
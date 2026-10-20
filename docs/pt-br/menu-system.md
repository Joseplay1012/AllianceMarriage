# Sistema de Menus e GUI

O **Sistema de Menus do AllianceMarriage** é um framework de inventários localizado no módulo `api` (`net.joseplay.api.menu`). Ele permite criar menus de baú registrando ações de clique diretamente em itens, sem necessidade de gerenciar ouvintes de eventos manualmente.

[English](../menu-system.md) | **Português (Brasil)**

---

## Classes e Interfaces Principais

### 1. `Menu` (Classe Abstrata)
Localizado em: [`Menu.java`](../../api/src/main/java/net/joseplay/api/menu/Menu.java)

Classe base para menus de baú. Implementa `InventoryHolder` do Bukkit, permitindo que o servidor identifique a instância do menu como titular do inventário.

#### Propriedades:
- `title`: Título do inventário (suporta cores com `&`).
- `rows`: Número de linhas do baú (de 1 a 6). Total de slots = `rows * 9`.
- `cancelClicksByDefault`: Padrão `true`. Cancela cliques nos slots da GUI para impedir retirada de itens.

#### Métodos Principais:
```java
// Define um item em slot específico com callback de clique
public void setItem(int slot, ItemStack itemStack, MenuAction action)

// Define um item usando a classe MenuItem
public void setItem(int slot, MenuItem item)

// Preenche slots restantes com um item decorativo
public void fill(ItemStack itemStack)

// Preenche apenas as bordas do baú
public void fillBorder(ItemStack itemStack)

// Limpa itens da interface
public void clear()

// Abre o baú para o jogador
public void open(Player player)

// Método para atualizar itens dinâmicos
public void update(Player player)
```

---

### 2. `MenuItem`
Localizado em: [`MenuItem.java`](../../api/src/main/java/net/joseplay/api/menu/MenuItem.java)

Encapsula um `ItemStack` junto com seu slot, estado de cancelamento de clique e o callback (`MenuAction`).

```java
// Ícone estático
MenuItem icon = MenuItem.of(new ItemStack(Material.PAPER));

// Botão com ação de clique
MenuItem button = MenuItem.of(itemStack, context -> {
    context.getPlayer().sendMessage("Botão clicado!");
});
```

---

### 3. `MenuAction` (Interface Funcional)
Localizado em: [`MenuAction.java`](../../api/src/main/java/net/joseplay/api/menu/MenuAction.java)

```java
@FunctionalInterface
public interface MenuAction {
    void execute(MenuContext context);
}
```
Interface executada ao clicar em um slot configurado com ação. Recebe uma instância de `MenuContext`.

---

### 4. `MenuContext`
Localizado em: [`MenuContext.java`](../../api/src/main/java/net/joseplay/api/menu/MenuContext.java)

Disponibiliza os dados do clique e métodos utilitários:

| Método | Descrição |
| :--- | :--- |
| `context.getPlayer()` | Retorna o `Player` que executou o clique. |
| `context.getMenu()` | Retorna o objeto `Menu` pai. |
| `context.getSlot()` | Retorna o número do slot clicado (0 a N). |
| `context.getClickType()` | Retorna o tipo de clique do Bukkit (`LEFT`, `RIGHT`, `SHIFT_LEFT`, etc.). |
| `context.close()` | Fecha o inventário do jogador. |
| `context.update()` | Invoca `menu.update(player)`. |

---

### 5. `MenuManager`
Localizado em: [`MenuManager.java`](../../api/src/main/java/net/joseplay/api/menu/MenuManager.java)

Ouvinte de eventos e controlador do ciclo de vida dos menus abertos:
```java
Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
```

#### Tratamento de Eventos:
1. **Cancelamento de Clique**: Cancela cliques dentro dos slots da GUI (`0 <= rawSlot < menuSize`) se `isCancelClicksByDefault()` for verdadeiro.
2. **Bloqueio de Shift-Click**: Cancela cliques com Shift vindos do inventário inferior em direção ao menu.
3. **Cancelamento de Arrasto**: Bloqueia `InventoryDragEvent` se afetar slots da tela superior.
4. **Desconexão**: Remove o jogador do mapa no `PlayerQuitEvent`.

---

### 6. `Pagination<T>`
Localizado em: [`Pagination.java`](../../api/src/main/java/net/joseplay/api/menu/Pagination.java)

Classe utilitária que divide listas (`List<T>`) em fatias por página:

```java
Pagination<HeartsShopItem> pagination = new Pagination<>(listaItens, 10);

List<HeartsShopItem> itensPagina = pagination.getItemsForCurrentPage();

if (pagination.hasNextPage()) {
    pagination.nextPage();
}

if (pagination.hasPreviousPage()) {
    pagination.previousPage();
}
```

---

## Menus Nativos do Plugin

### 1. `MarriageMenu` (Visão Geral)
Localizado em: [`MarriageMenu.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/MarriageMenu.java)

- **Casado**: Exibe cabeça do cônjuge, duração calculada via `Duration.between(anniversary, now)`, saldo de Hearts, atalho para loja e botão de divórcio.
- **Solteiro**: Exibe aviso orientando o uso de `/marry <jogador>`.

### 2. `HeartsShopMenu` (Loja)
Localizado em: [`HeartsShopMenu.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/HeartsShopMenu.java)

- Utiliza [`HeartShopLayout.java`](../../plugin/src/main/java/net/joseplay/plugin/shop/HeartShopLayout.java) para posicionar 26 itens no desenho de um coração (linhas 1 a 5).
- A linha 6 contém controles de voltar, página anterior, contador, próxima página e fechar.
- Dispara transações através do `HeartsShopService`.

### 3. `CoupleSettings` (Preferências)
Localizado em: [`CoupleSettings.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/CoupleSettings.java)

- Lê preferências registradas em `PreferencesManager` e as renderiza nos slots `[10, 12, 14, 16, 20, 22, 24, 28, 30, 32, 34]`.

### 4. `MarriageRequestMenu` (Pedido)
Localizado em: [`MarriageRequestMenu.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/MarriageRequestMenu.java)

- Tela de 3 linhas com cabeça do proponente no centro, botão verde de aceitar (slot 11) e botão vermelho de recusar (slot 15).

---

## Exemplo: Criando um Menu Customizado

```java
package net.joseplay.exemplo;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuContext;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MenuExemplo extends Menu {

    public MenuExemplo(Player player) {
        super("&8Menu Exemplo", 3); // 3 linhas (27 slots)
        render(player);
    }

    private void render(Player player) {
        ItemStack borda = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        fillBorder(borda);

        ItemStack botao = ItemBuilder.of(Material.EMERALD)
                .name("&aExecutar")
                .lore("&7Clique para acionar.")
                .build();

        setItem(13, botao, context -> {
            context.getPlayer().sendMessage("Ação executada!");
            context.close();
        });

        ItemStack fechar = ItemBuilder.of(Material.BARRIER)
                .name("&cFechar")
                .build();
        setItem(26, fechar, MenuContext::close);
    }
}
```

### Abertura:
```java
MenuManager menuManager = plugin.getMarriageMenuManager().getMenuManager();
menuManager.open(player, new MenuExemplo(player));
```

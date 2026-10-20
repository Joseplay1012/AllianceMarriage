# AllianceMarriage

Plugin para servidores Spigot/Paper (Minecraft 1.20+) que implementa sistema de casamentos entre jogadores, economia compartilhada de corações ("Hearts"), loja em interface de baú com disposição temática em formato de coração, controle de dano mútuo (PvP) entre parceiros e persistência de dados em SQLite ou MySQL via HikariCP.

[English](README.md) | **Português (Brasil)**

---

## Documentação Técnica

Os guias detalhados de cada subsistema estão disponíveis na pasta [`docs/pt-br/`](docs/pt-br/README.md):

| Documento | Conteúdo |
| :--- | :--- |
| [Central de Documentação (`docs/pt-br/README.md`)](docs/pt-br/README.md) | Índice geral, lista de recursos implementados, avaliação técnica e limitações |
| [Arquitetura (`docs/pt-br/architecture-overview.md`)](docs/pt-br/architecture-overview.md) | Divisão dos módulos Maven (`api`, `core`, `plugin`), fluxo de chamadas e concorrência |
| [Sistema de Menus (`docs/pt-br/menu-system.md`)](docs/pt-br/menu-system.md) | Framework de inventários (`Menu`, `MenuItem`, `MenuManager`, `Pagination`) e tutorial |
| [Sistema de Features (`docs/pt-br/features-system.md`)](docs/pt-br/features-system.md) | Atributos dinâmicos de casais (`Feature<T>`), registro e incrementos diretos no banco |
| [Armazenamento e Banco (`docs/pt-br/storage-and-database.md`)](docs/pt-br/storage-and-database.md) | Tabelas SQL, pool HikariCP, cache em memória e normalização de pares de UUIDs |
| [Serviços e Regras de Negócio (`docs/pt-br/services-and-business-logic.md`)](docs/pt-br/services-and-business-logic.md) | Propostas de casamento, debounce de compras na loja e listeners de eventos |
| [API de Preferências (`docs/pt-br/preferences-api.md`)](docs/pt-br/preferences-api.md) | Inserção de botões na tela `/marriage couple` via `Preference` e `PreferencesManager` |
| [Comandos e Configuração (`docs/pt-br/commands-and-configuration.md`)](docs/pt-br/commands-and-configuration.md) | Sintaxe do `/marriage`, permissões e estrutura do arquivo `hearts-shop.yml` |

---

## Organização dos Módulos

O projeto adota uma divisão Maven em quatro módulos:

- **`api`** ([`AllianceMarriage-api`](api/pom.xml)): Define contratos de interfaces gráficas (`Menu`, `MenuItem`, `Pagination`) e preferências. Não depende do banco de dados nem da lógica específica de casamento.
- **`core`** ([`AllianceMarriage-core`](core/pom.xml)): Contém a classe `Couple`, o sistema de atributos `Feature<T>`, o pool HikariCP (`Database`) e os repositórios de persistência (`CouplesRepository`, `FeatureRepository`).
- **`plugin`** ([`AllianceMarriage-plugin`](plugin/pom.xml)): Ponto de entrada do Spigot (`Plugin.java`), roteamento de comandos, registro de listeners, menus interativos e leitura do arquivo `hearts-shop.yml`.
- **`test`** ([`test`](test/pom.xml)): Ambiente em console isolado utilizado para testes manuais de gravação e serialização de dados sem instanciar o Bukkit.

---

## O que Já Foi Implementado

- **Ciclo de Casamento**: Propostas enviadas com `/marriage marry <jogador>` com cancelamento automático após 60 segundos de inatividade, tela visual de resposta (`MarriageRequestMenu`) e divórcio via `/marriage divorce`.
- **Economia de Hearts Compartilhada**: Saldo conjunto mantido na tabela `couples_features`. A adição e débito de pontos são executados no banco via comandos SQL (`CAST` e `ON DUPLICATE KEY UPDATE` / `ON CONFLICT`), evitando conflitos de concorrência com cliques simultâneos.
- **Loja Visual em Formato de Coração**: A classe `HeartShopLayout` define 26 slots para exibição de itens nas primeiras 5 linhas de um baú duplo de 54 slots, mantendo a 6ª linha para navegação de páginas e botão de fechar. Os produtos são configurados em [`plugin/src/main/resources/hearts-shop.yml`](plugin/src/main/resources/hearts-shop.yml).
- **Controle de Fogo Amigo (PvP)**: Configuração booleana individual de cada casal. Quando desativada, o listener `MarriageListeners` cancela instâncias de `EntityDamageByEntityEvent` entre cônjuges.
- **Cache Local de Casais**: `CouplesImpl` armazena pares em memória através de mapas concorrentes (`coupleMap` e `playerCoupleMap`). Durante o evento de login (`PlayerJoinEvent`), os dados da união são carregados preventivamente em segundo plano.

---

## Avaliação Técnica do Código

**Nota geral:** 7.8 / 10

### Pontos Positivos
- Separação evidente entre API de UI, lógica de domínio e runtime do Bukkit.
- Execução de operações de banco em threads separadas através de `CompletableFuture` e `ExecutorService`.
- Normalização de UUIDs (`partner1` recebe o menor valor comparado a `partner2`), garantindo que consultas por `(A, B)` e `(B, A)` acessem a mesma linha do banco.
- Incrementos numéricos no banco usam operações atômicas no próprio SQL, sem leitura prévia intermediária em memória.

### Limitações e Débitos Técnicos
- Configurações de conexão ao MySQL em [`core/src/main/java/net/joseplay/core/config/PluginSettings.java`](core/src/main/java/net/joseplay/core/config/PluginSettings.java) possuem valores fixos em código Java (`root`, `admin`, host vazio) e não leem um arquivo `config.yml` da pasta do plugin.
- A ação de divórcio não solicita confirmação prévia ao jogador antes de apagar o vínculo no banco.
- O módulo `test` possui código de rascunho com UUIDs fixos e chamadas comentadas em vez de testes automatizados com JUnit.

---

## Exemplos de Uso da API

### Consultar o casal e atualizar atributos

```java
MarriageService marriageService = plugin.getMarriageService();

marriageService.getCouple(player.getUniqueId()).thenAccept(optionalCouple -> {
    if (optionalCouple.isEmpty()) {
        player.sendMessage("Você não está casado.");
        return;
    }

    Couple couple = optionalCouple.get();

    // Leitura de feature
    int hearts = couple.features().get("hearts");
    boolean pvpAtivo = couple.features().get("pvp");

    // Alteração de valor booleano
    couple.features().set("pvp", !pvpAtivo);

    // Incremento numérico atômico
    couple.features().increment("hearts", 10).thenAccept(novoSaldo -> {
        player.sendMessage("Novo saldo: " + novoSaldo);
    });
});
```

### Criar uma tela com a Menu API

```java
public class ExemploMenu extends Menu {

    public ExemploMenu() {
        super("&8Exemplo de Menu", 3); // 3 linhas (27 slots)

        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aClique aqui");
        item.setItemMeta(meta);

        setItem(13, item, context -> {
            context.getPlayer().sendMessage("Slot clicado: " + context.getSlot());
            context.close();
        });

        fillBorder(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
    }
}
```

---

## Compilação

Requisitos:
- Java Development Kit (JDK) 21 ou superior.
- Apache Maven 3.8+.

Para compilar e gerar o pacote com dependências embutidas (shaded jar):

```bash
mvn clean package
```

O arquivo gerado estará em:
```
plugin/target/AllianceMarriage-1.0.jar
```

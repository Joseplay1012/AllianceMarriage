# Documentação do AllianceMarriage

Documentação técnica do **AllianceMarriage**, plugin Spigot/Paper para gerenciamento de casamentos, economia compartilhada, interfaces de baú e atributos dinâmicos por casal.

[English](../README.md) | **Português (Brasil)**

---

## Índice da Documentação

| Guia | Descrição | Pastas Relevantes |
| :--- | :--- | :--- |
| [Visão Geral da Arquitetura](architecture-overview.md) | Divisão dos módulos Maven, ciclo de vida e concorrência | Raiz e POMs dos módulos |
| [Sistema de Menus](menu-system.md) | Framework de inventários, paginação e classes de menu | [`api/menu`](../../api/src/main/java/net/joseplay/api/menu/) e [`plugin/menu`](../../plugin/src/main/java/net/joseplay/plugin/menu/) |
| [Sistema de Features](features-system.md) | Atributos dinâmicos, incrementos atômicos e registro de features | [`core/feature`](../../core/src/main/java/net/joseplay/core/feature/) |
| [Armazenamento e Banco](storage-and-database.md) | Pool HikariCP, suporte a SQLite/MySQL, cache e repositórios | [`core/storage`](../../core/src/main/java/net/joseplay/core/storage/) |
| [Serviços e Regras de Negócio](services-and-business-logic.md) | Propostas de casamento, compras na loja Hearts e listeners | [`plugin/service`](../../plugin/src/main/java/net/joseplay/plugin/service/) e [`plugin/shop`](../../plugin/src/main/java/net/joseplay/plugin/shop/) |
| [API de Preferências](preferences-api.md) | Registro de botões de configuração na tela `/marriage couple` | [`api/preferences`](../../api/src/main/java/net/joseplay/api/preferences/) |
| [Comandos e Configuração](commands-and-configuration.md) | Sintaxe dos comandos, permissões e estrutura do `hearts-shop.yml` | [`plugin/commands`](../../plugin/src/main/java/net/joseplay/plugin/commands/) e [`hearts-shop.yml`](../../plugin/src/main/resources/hearts-shop.yml) |

---

## O que Já Foi Feito

- **Divisão Modular em Maven**:
  - `api`: Contratos de interface gráfica (`Menu`, `MenuItem`, `Pagination`, `Preference`) sem lógica de banco ou regras de jogo.
  - `core`: Modelos de dados (`Couple`), motor dinâmico de atributos (`Feature<T>`), pool de conexões (`Database`) e implementações de repositório.
  - `plugin`: Listeners de eventos do Spigot, comandos, carregador YAML e menus em baú.
- **Operações de Banco Assíncronas**:
  - Pool de conexões via HikariCP com suporte a SQLite local e MySQL remoto.
  - Consultas e atualizações executadas fora da thread do servidor através de `CompletableFuture` em thread pool dedicado.
  - Cache em memória nos dois sentidos (`coupleMap` e `playerCoupleMap`).
- **Sistema Dinâmico de Atributos (Features)**:
  - Atributos gravados em formato chave-valor na tabela `couples_features`.
  - Recursos numéricos (`INTEGER`, `LONG`, `DOUBLE`) somados diretamente via SQL (`CAST(value AS SIGNED) + CAST(? AS SIGNED)`), impedindo que cliques rápidos sobrescrevam o saldo incorretamente.
  - Features padrão: `Hearts` (moeda do casal) e `PvP` (dano mútuo).
- **Framework de Menus e GUI**:
  - Classes reutilizáveis (`Menu`, `MenuItem`, `MenuContext`) com cancelamento automático de cliques, proteção contra shift-click e bloqueio de arrasto.
  - Utilitário de paginação genérica (`Pagination<T>`).
- **Loja em Formato de Coração**:
  - `HeartShopLayout` distribui 26 produtos no desenho de um coração em 6 linhas de baú, deixando a última linha para navegação e botão de fechar.
  - Produtos configurados via [`hearts-shop.yml`](../../plugin/src/main/resources/hearts-shop.yml) com comandos de console, preços e placeholders (`%player%`, `%uuid%`, `%price%`, `%item%`, `%hearts%`).
  - Trava anti-spam concorrente (`processingPurchases`) para evitar compras duplicadas durante o envio ao banco.
- **Fluxo de Proposta de Casamento**:
  - Pedidos expiram automaticamente após 60 segundos (`MarriageRequest`).
  - Menu interativo (`MarriageRequestMenu`) com opções de aceitar e recusar.
  - Fluxo de divórcio que remove registros no banco e limpa os dados em memória.

---

## Vantagens Técnicas

1. **Separação de Responsabilidades**:
   - O motor de GUI fica em `api`, desacoplado do banco de dados e das regras de casamento.
   - As operações SQL lidam apenas com entidades de domínio (`Couple`, `StoredFeature`), sem código de banco em comandos ou ouvintes de eventos.
2. **Execução Assíncrona de I/O**:
   - Leituras e gravações rodam em threads secundárias, prevenindo quedas de TPS causadas por latência de banco de dados.
3. **Incrementos Atômicos no SQL**:
   - Adições e débitos de Hearts são computados pelo próprio motor SQL, eliminando condições de corrida entre cliques de ambos os parceiros.
4. **Normalização de UUIDs**:
   - Os pares de UUIDs são ordenados em ordem alfabética (`partner1` sempre menor que `partner2`). Consultar `(JogadorA, JogadorB)` e `(JogadorB, JogadorA)` retorna exatamente o mesmo registro.

---

## Avaliação Técnica e Nota

### Nota Geral: **7.8 / 10**

| Dimensão | Nota | Descrição Técnica |
| :--- | :---: | :--- |
| **Arquitetura e Modularidade** | **9.0 / 10** | Limites claros entre `api`, `core` e `plugin`. Entidades e repositórios isolados. |
| **Persistência e Concorrência**| **8.5 / 10** | HikariCP configurado, I/O assíncrono com `CompletableFuture`, incrementos atômicos no banco. |
| **Extensibilidade** | **8.5 / 10** | Registries de Features e Preferences permitem novos atributos sem mexer nas tabelas SQL. |
| **Organização do Código** | **8.0 / 10** | Uso de recursos do Java 21 (records, lambdas, switch expressions). |
| **Tratamento e Configurações** | **6.5 / 10** | Dados do MySQL em `PluginSettings` estão fixos em código Java em vez de lerem um `config.yml`. |
| **Testes Automatizados** | **6.0 / 10** | Testes unitários para `Pagination`, `HeartsShopItem` e `ShopLayout`. O módulo `test` possui código de rascunho em vez de testes JUnit. |

### Pontos para Próximas Versões:
1. **Configuração Externa**: Ler host, porta, banco, usuário e senha do MySQL de um `config.yml` na pasta do plugin, substituindo os valores fixos em [`PluginSettings.java`](../../core/src/main/java/net/joseplay/core/config/PluginSettings.java).
2. **Confirmação de Divórcio**: Exigir um segundo clique de confirmação antes de executar o divórcio.
3. **Testes de Integração**: Substituir os testes manuais do módulo `test` por testes automatizados com SQLite em memória.

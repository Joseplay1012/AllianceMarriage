# Comandos e Configuração

Este guia descreve a sintaxe dos comandos, permissões e o formato YAML do arquivo de configuração da loja de Hearts.

[English](../commands-and-configuration.md) | **Português (Brasil)**

---

## Comandos: `/marriage`

Executor do comando: [`MarriageCommands.java`](../../plugin/src/main/java/net/joseplay/plugin/commands/MarriageCommands.java)

| Subcomando | Argumentos | Permissão | Descrição |
| :--- | :--- | :--- | :--- |
| `/marriage` | *(nenhum)* | Livre (padrão) | Abre o menu principal de casamento ([`MarriageMenu.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/MarriageMenu.java)). |
| `/marriage menu` | *(nenhum)* | Livre (padrão) | Abre o menu principal de casamento. |
| `/marriage shop` | *(nenhum)* | Livre (padrão) | Abre a loja de Hearts ([`HeartsShopMenu.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/HeartsShopMenu.java)). |
| `/marriage marry` | `<jogador>` | Livre (padrão) | Envia pedido de casamento para um jogador online. |
| `/marriage accept`| *(nenhum)* | Livre (padrão) | Aceita proposta pendente. |
| `/marriage deny` | *(nenhum)* | Livre (padrão) | Recusa proposta pendente. |
| `/marriage couple`| *(nenhum)* | Livre (padrão) | Abre a tela de ajustes ([`CoupleSettings.java`](../../plugin/src/main/java/net/joseplay/plugin/menu/CoupleSettings.java)). |
| `/marriage divorce`| *(nenhum)* | Livre (padrão) | Desfaz o casamento atual. |
| `/marriage reload` | *(nenhum)* | `marry.admin` ou OP | Recarrega o [`hearts-shop.yml`](../../plugin/src/main/resources/hearts-shop.yml) do disco. |

### Auto-Completar (Tab-Completion):
- `/marriage <TAB>` sugere os subcomandos (`menu`, `shop`, `marry`, `accept`, `deny`, `couple`, `divorce`, `reload`).
- `/marriage marry <TAB>` lista os jogadores online (excluindo quem digitou).

---

## Permissões

| Permissão | Padrão | Descrição |
| :--- | :--- | :--- |
| `marry.admin` | `op` | Permite recarregar a configuração com `/marriage reload`. |

---

## Configuração da Loja: `hearts-shop.yml`
Caminho: [`plugin/src/main/resources/hearts-shop.yml`](../../plugin/src/main/resources/hearts-shop.yml)

### Estrutura dos Itens:

```yaml
items:
  <id_do_item>:
    material: <MATERIAL_NAME>      # Nome de enum válido do Bukkit Material (ex: DIAMOND, CAKE)
    amount: <int>                  # Quantidade do item (padrão: 1)
    slot: AUTO                     # "AUTO" preenche os slots sequenciais do coração, ou informe número de slot (0 a 44)
    price: <int>                   # Custo em Hearts
    display:
      name: "<texto>"              # Nome de exibição (suporta códigos com '&')
      lore:                        # Lista de linhas de descrição
        - "&7Texto de descrição"
        - "&ePreço: &c♥ %price%"
      glow: <true|false>           # Aplica brilho de encantamento sem listar encantamento
      unbreakable: <true|false>    # Define flag de item inquebrável
      custom-model-data: <int>     # Número de CustomModelData (0 para nenhum)
      hide-flags:                  # Nomes de ItemFlag para ocultar atributos
        - ATTRIBUTES
        - ENCHANTS
    commands:                      # Comandos executados pelo console após a compra
      - "give %player% diamond 1"
```

---

## Placeholders

Suportados no nome, lore e comandos:

| Placeholder | Substituído Por |
| :--- | :--- |
| `%player%` | Nome de usuário do jogador. |
| `%uuid%` | UUID em formato String do jogador. |
| `%price%` | Preço pago em Hearts. |
| `%item%` | Chave de identificação do item na configuração. |
| `%hearts%` | Saldo restante de Hearts do casal após a compra. |

---

## Exemplo de Item

```yaml
items:
  pocao_velocidade:
    material: POTION
    amount: 1
    slot: AUTO
    price: 60
    display:
      name: "&bPocao de Rapidez"
      lore:
        - "&7Concede Velocidade II por 3 minutos."
        - "&ePreço: &c♥ %price%"
    commands:
      - "effect give %player% speed 180 1"
```

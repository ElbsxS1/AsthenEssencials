# Asthen Essentials

Plugin **Paper 26.3** (Java 25): menu, homes, warps, spawn, TPA e moderação. Independente do DiscordKitBridge.

## Menu — `/menu`
- **Java:** abre um menu de baú (os itens são botões; nada pode ser pego).
- **Bedrock:** abre um formulário nativo (precisa do **Floodgate**; sem ele, o baú aparece também no Bedrock via Geyser).
- Opções: Minhas homes (ir, criar, apagar), Spawn, Pedir teleporte (lista de jogadores online) e, só para operadores, Warps.

## Comandos
| Quem | Comandos |
|---|---|
| Todos | `/menu`, `/sethome [nome]`, `/home [nome]`, `/delhome <nome>`, `/homes`, `/spawn`, `/tpa <jogador>`, `/tpaccept`, `/tpdeny` |
| Só operadores | `/warp`, `/warps`, `/setwarp`, `/delwarp`, `/setspawn`, `/kick`, `/ban`, `/tempban <jogador> <30m\|2h\|7d>`, `/unban`, `/mute [tempo]`, `/unmute`, `/aessentials reload` |

- Máximo de **5 homes** por jogador (`homes.max`).
- Teleportes têm **espera de 5s**, cancelada se o jogador se mexer (`teleport.warmup-seconds`), e intervalo de 10s.
- Não há economia, `/back` nem `/rtp`.
- Comandos de operador não aparecem para quem não pode usá-los.

## Instalação
1. Baixe o `.jar` em **Actions > Build > Artifacts** (o GitHub compila com JDK 25).
2. Coloque em `plugins/` (e o **Floodgate**, se houver jogadores Bedrock) e reinicie.
3. Dados em `plugins/AsthenEssentials/`: `places.json`, `punishments.json`.

## Testes
`./scripts/run-tests.sh` roda 26 testes (JDK puro) do núcleo. A camada do Paper/Floodgate foi só compilada contra stubs.

### Roteiro em jogo
- [ ] Java: `/menu` abre o baú; clicar não deixa pegar o item; Homes → Definir home aqui → aparece na lista; Apagar uma home funciona.
- [ ] Bedrock: `/menu` abre o formulário; os mesmos botões funcionam.
- [ ] Menu de jogador comum **não** mostra Warps; `/warp` e `/warps` dão "sem permissão"; op vê e usa.
- [ ] 6ª home é recusada; teleporte espera 5s e cancela se andar.
- [ ] `/tpa` + `/tpaccept`; `/tempban Fulano 1m`; `/mute Fulano 1m`; reiniciar mantém homes, warps, bans e mutes.

## Créditos
Inspirado em *Minecraft Essentials* de Paoeni (MIT). Nenhum código ou recurso do add-on foi copiado.

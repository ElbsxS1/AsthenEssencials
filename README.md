# Asthen Essentials

Plugin **Paper 26.3** (Java 25) com o básico de servidor, inspirado no add-on *Minecraft Essentials* (Bedrock),
reescrito do zero em Java. Independente do DiscordKitBridge.

## Funções (Fase 1)
| Área | Comandos |
|---|---|
| Economia | `/money [jogador]`, `/pay <jogador> <valor>`, `/baltop`, `/eco give\|take\|set <jogador> <valor>` (admin) |
| Homes | `/sethome [nome]`, `/home [nome]`, `/delhome <nome>`, `/homes` (limite em `homes.max`) |
| Warps | `/warp <nome>`, `/warps`, `/setwarp <nome>` e `/delwarp <nome>` (admin) |
| Spawn | `/spawn`, `/setspawn` (admin); novos jogadores nascem no spawn |
| Teleporte | `/tpa <jogador>`, `/tpaccept`, `/tpdeny`, `/back` (também volta ao local da morte), `/rtp` |
| Moderação (admin) | `/kick`, `/ban`, `/tempban <jogador> <30m\|2h\|7d>`, `/unban`, `/mute [tempo]`, `/unmute` |
| Geral | `/aessentials reload` |

Valores aceitam `25`, `25,50` ou `25.50`. Dinheiro é guardado em centavos (sem erro de arredondamento).
Teleportes têm espera (cancelada se o jogador se mexer) e intervalo, ambos configuráveis.

## Permissões
- `asthenessentials.admin` (padrão: op): eco, setwarp, delwarp, setspawn, kick, ban, mute, reload.
- `asthenessentials.money|pay|home|warp|spawn|tpa|back|rtp`: padrão **todos**.
Comandos sem permissão não aparecem para quem não pode usá-los.

## Instalação
1. Baixe o `.jar` em **Actions > Build > Artifacts** do repositório (o GitHub compila com JDK 25).
2. Coloque em `plugins/` e reinicie. Edite `plugins/AsthenEssentials/config.yml` e use `/aessentials reload`.
3. Dados em `plugins/AsthenEssentials/`: `economy.json`, `places.json`, `punishments.json`.

## Testes
`./scripts/run-tests.sh` roda 45 testes (JDK puro) do núcleo: dinheiro, transferências atômicas, homes/warps,
TPA com expiração, cooldowns, bans/mutes temporários e persistência. A camada do Paper foi só compilada contra stubs;
valide em jogo com o roteiro abaixo.

### Roteiro em jogo
- [ ] `/money` mostra o saldo inicial; `/pay` entre dois jogadores muda os dois saldos; `/pay` sem saldo é recusado.
- [ ] `/sethome`, `/home` (espera 3s; andar cancela), `/homes`, `/delhome`; limite de homes respeitado.
- [ ] `/setwarp x` (op), `/warp x` (jogador comum), `/spawn`, `/setspawn`.
- [ ] `/tpa` + `/tpaccept` e `/tpdeny`; pedido expira em 60s.
- [ ] Morrer e usar `/back`; `/rtp` leva a lugar seguro e respeita o intervalo.
- [ ] `/tempban Fulano 1m` expulsa e impede entrar por 1 minuto; `/mute Fulano 1m` bloqueia o chat; `/unban`, `/unmute`.
- [ ] Reiniciar o servidor mantém saldo, homes, warps, spawn, bans e mutes.
- [ ] Jogador sem op não vê `/eco`, `/ban`, `/setwarp` etc.

## Roadmap
Fase 2: loja/venda (`/shop`, `/sell`), saque (`/withdraw`), leilão; Fase 3: terrenos, ranks, textos flutuantes.
Possível integração: comprar kits do DiscordKitBridge com o dinheiro do servidor.

## Créditos
Inspirado em *Minecraft Essentials* de Paoeni (MIT). Nenhum código ou recurso do add-on foi copiado.

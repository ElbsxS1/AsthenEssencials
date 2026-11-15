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
`./scripts/run-tests.sh` roda 45 testes (JDK puro) do núcleo: dinheiro, transferências atômicas, homes,
warps, punições (ban/mute com expiração), TPA e cooldowns. A CI do GitHub executa isso a cada push.

## Build local
```bash
mvn -B package   # gera target/AsthenEssentials-*.jar
```
Requer **JDK 25** (Paper 26.3 API).

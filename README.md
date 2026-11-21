# Asthen Essentials

Plugin **Paper 26.3** (Java 25): menu, homes, warps, TPA e moderação. Independente do DiscordKitBridge e compatível com o LobbyGuard.

## Menu — `/menu`
- **Java:** abre um menu de baú (os itens são botões; nada pode ser pego).
- **Bedrock:** abre um formulário nativo (precisa do **Floodgate**; sem ele, o baú aparece também no Bedrock via Geyser).
- Opções: Minhas homes (ir, criar, apagar), Voltar ao lobby (se houver LobbyGuard), Pedir teleporte (lista de jogadores online) e, só para operadores, Warps.

## Item do menu
O item **Menu Asthen** (Estrela do Nether) é um item comum: o jogador pode largar, guardar e perder à vontade.
Não há entrega automática. Ele é pego de duas formas:
- `/bussola` (alias `/menuitem`), em qualquer mundo;
- clicando com o botão direito em um **bloco de retirada** (por exemplo, um baú no lobby). O admin olha para o bloco e usa
  `/aessentials setpickup` (e `/aessentials removepickup` para desfazer). O baú não abre, só entrega o item. Quebrar o bloco o desregistra.

Botão direito com o item abre o `/menu` (baú no Java, formulário no Bedrock), **só na sobrevivência**.
O material fica em `menu-item.material`. **Não use `COMPASS`**: o WorldEdit usa a bússola como varinha de navegação e teleporta
o jogador para o bloco que ele olha. Bússolas do Menu Asthen da versão 0.3.0 são removidas do inventário quando o jogador entra.

## Compatibilidade com o LobbyGuard
- Menu, `/home`, `/sethome`, `/delhome`, `/homes` e `/tpa` só funcionam nos mundos de `worlds.allowed` (padrão: `survival`),
  ou seja, nada disso vale no lobby. `/warp` e `/warps` seguem só para operadores, em qualquer mundo.
- O Asthen Essentials **não tem `/spawn`, `/setspawn` nem teleporte ao entrar**: o spawn e a entrada ficam com o LobbyGuard.
  Com o LobbyGuard instalado, o menu mostra "Voltar ao lobby" (roda o `/lob` dele, com a espera de 5s).
- Os teleportes do Asthen Essentials disparam o evento padrão de teleporte, então o LobbyGuard continua salvando o último
  local da sobrevivência normalmente.

## Comandos
| Quem | Comandos |
|---|---|
| Todos | `/menu`, `/bussola`, `/sethome [nome]`, `/home [nome]`, `/delhome <nome>`, `/homes`, `/tpa <jogador>`, `/tpaccept`, `/tpdeny` |
| Só operadores | `/warp`, `/warps`, `/setwarp`, `/delwarp`, `/kick`, `/ban`, `/tempban <jogador> <30m\|2h\|7d>`, `/unban`, `/mute [tempo]`, `/unmute`, `/aessentials reload\|setpickup\|removepickup` |

- Máximo de **5 homes** por jogador (`homes.max`).
- Teleportes têm **espera de 5s**, cancelada se o jogador se mexer (`teleport.warmup-seconds`), e intervalo de 10s.
- Não há economia, `/back` nem `/rtp`.
- Comandos de operador não aparecem para quem não pode usá-los.

## Instalação
1. Baixe o `.jar` em **Actions > Build > Artifacts** (o GitHub compila com JDK 25).
2. Coloque em `plugins/` (e o **Floodgate**, se houver jogadores Bedrock) e reinicie.
3. Dados em `plugins/AsthenEssentials/`: `places.json`, `punishments.json`.

## Testes
`./scripts/run-tests.sh` roda 32 testes (JDK puro) do núcleo. A camada do Paper/Floodgate foi só compilada contra stubs.

### Roteiro em jogo
- [ ] No lobby, `/aessentials setpickup` olhando para um baú; clicar nele entrega o Menu Asthen (não abre o baú); clicar de novo diz que já tem.
- [ ] Clicar com a Estrela do Nether **não teleporta** (WorldEdit); no lobby avisa que só funciona na sobrevivência.
- [ ] Na sobrevivência, botão direito abre o menu (Java: baú; Bedrock: formulário). O item pode ser largado e guardado em baú.
- [ ] `/home`, `/sethome`, `/tpa`, `/menu` no lobby: "só funciona na sobrevivência". `/spawn` não existe.
- [ ] Menu > Voltar ao lobby roda o `/lob` do LobbyGuard.
- [ ] Menu de jogador comum **não** mostra Warps; `/warp` e `/warps` dão "sem permissão"; op vê e usa.
- [ ] 6ª home é recusada; teleporte espera 5s e cancela se andar; `/tpa` + `/tpaccept` entre dois jogadores na sobrevivência.
- [ ] `/tempban Fulano 1m` e `/mute Fulano 1m`; reiniciar mantém homes, warps, blocos de retirada, bans e mutes.

## Créditos
Inspirado em *Minecraft Essentials* de Paoeni (MIT). Nenhum código ou recurso do add-on foi copiado.

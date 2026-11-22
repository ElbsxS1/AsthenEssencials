# Asthen Essentials

Plugin **Paper 26.3** (Java 25): menu, homes, TPA, painel de administração e moderação. Independente do DiscordKitBridge e compatível com o LobbyGuard.

## Menu — `/menu`
- **Java:** abre um menu de baú (os itens são botões; nada pode ser pego).
- **Bedrock:** abre um formulário nativo (precisa do **Floodgate**; sem ele, o baú aparece também no Bedrock via Geyser).
- Opções: Minhas homes (ir, criar, apagar), Voltar ao lobby (se houver LobbyGuard) e Pedir teleporte (lista de jogadores online).
- **Criar home:** antes de criar, o menu pergunta o **nome** da home. No Bedrock aparece um formulário com campo de texto; no Java
  você digita o nome no chat (a mensagem não aparece para os outros). `pular` ou em branco usa um nome automático (`home`, `home2`...),
  `cancelar` volta. Espaços viram `_` ("Minha casa" vira `Minha_casa`); aceita letras com acento, números, `_` e `-`, até 24 caracteres.
  Nome repetido é recusado (não sobrescreve a home existente).

## Painel Admin
Item exclusivo de administradores (permissão `asthenessentials.admin`, padrão: op), separado do Menu Asthen.
- `/adminitem` (alias `/itemadmin`) entrega o item **Painel Admin** (Vara de Blaze; material em `admin-item.material`). Funciona em **qualquer mundo**.
  `/painel` abre o painel direto, sem o item. Quem não é admin não consegue usar o item, e ele é removido do inventário quando
  o jogador entra no servidor sem a permissão (por exemplo, um ex-admin).
- **Jogadores online** > escolher o jogador:
  - **Ver inventário** e **Ver baú do ender**: mostra tudo (inventário, hotbar, armadura, mão secundária e o baú do ender).
    **Clicar em um item pega a pilha inteira** e leva para o seu inventário (armadura e espada também). Se o seu inventário
    estiver cheio, o que não couber volta para o jogador. Não dá para colocar nem trocar itens de lugar. Cada clique é conferido
    com o que o jogador tem **naquele momento**: se ele mexeu no inventário enquanto você olhava, a tela é atualizada e nada é
    duplicado. Só funciona com o jogador online. Cada item pego fica registrado no console (quem pegou, o quê e de quem).
  - **Expulsar** (pede o motivo), **Silenciar** e **Banir**: escolhe o **tempo** (30 minutos, 2 horas, 1 dia, 7 dias, 30 dias ou
    permanente) e depois o **motivo** (opcional). Usam os mesmos comandos `/kick`, `/tempban`, `/ban` e `/mute`.
  - Não dá para se punir pelo painel.
- **Banidos** e **Silenciados**: lista com motivo, quem aplicou e o tempo restante; clicar em um nome remove a punição.

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
  ou seja, nada disso vale no lobby. O Painel Admin funciona em qualquer mundo.
- O Asthen Essentials **não tem `/spawn`, `/setspawn` nem teleporte ao entrar**: o spawn e a entrada ficam com o LobbyGuard.
  Com o LobbyGuard instalado, o menu mostra "Voltar ao lobby" (roda o `/lob` dele, com a espera de 5s).
- Os teleportes do Asthen Essentials disparam o evento padrão de teleporte, então o LobbyGuard continua salvando o último
  local da sobrevivência normalmente.

## Comandos
| Quem | Comandos |
|---|---|
| Todos | `/menu`, `/bussola`, `/sethome [nome]`, `/home [nome]`, `/delhome <nome>`, `/homes`, `/tpa <jogador>`, `/tpaccept`, `/tpdeny` |
| Só operadores | `/adminitem`, `/painel`, `/kick`, `/ban`, `/tempban <jogador> <30m\|2h\|7d>`, `/unban`, `/mute [tempo]`, `/unmute`, `/aessentials reload\|setpickup\|removepickup` |

- Máximo de **5 homes** por jogador (`homes.max`).
- Teleportes têm **espera de 5s**, cancelada se o jogador se mexer (`teleport.warmup-seconds`), e intervalo de 10s.
- Não há economia, `/back` nem `/rtp`.
- Comandos de operador não aparecem para quem não pode usá-los.

## Instalação
1. Baixe o `.jar` em **Actions > Build > Artifacts** (o GitHub compila com JDK 25).
2. Coloque em `plugins/` (e o **Floodgate**, se houver jogadores Bedrock) e reinicie.
3. Dados em `plugins/AsthenEssentials/`: `places.json`, `punishments.json`. Se vier da 0.4.0, os warps antigos em `places.json`
   são ignorados e somem na próxima gravação (as homes continuam).

## Testes
`./scripts/run-tests.sh` roda 38 testes (JDK puro) do núcleo. A camada do Paper/Floodgate foi só compilada contra stubs.

### Roteiro em jogo
- [ ] No lobby, `/aessentials setpickup` olhando para um baú; clicar nele entrega o Menu Asthen (não abre o baú); clicar de novo diz que já tem.
- [ ] Clicar com a Estrela do Nether **não teleporta** (WorldEdit); no lobby avisa que só funciona na sobrevivência.
- [ ] Na sobrevivência, botão direito abre o menu (Java: baú; Bedrock: formulário). O item pode ser largado e guardado em baú.
- [ ] `/home`, `/sethome`, `/tpa`, `/menu` no lobby: "só funciona na sobrevivência". `/spawn` não existe.
- [ ] Menu > Voltar ao lobby roda o `/lob` do LobbyGuard.
- [ ] Menu de jogador comum não tem Warps (removidos); `/warp` não existe mais.
- [ ] Menu > Minhas homes > Criar home aqui: Java pede o nome no chat (e a mensagem não aparece para os outros), Bedrock abre o formulário; nome repetido é recusado; `cancelar` volta.
- [ ] `/adminitem` com op entrega o item; sem op dá "sem permissão". Botão direito abre o painel em qualquer mundo.
- [ ] Painel > jogador > Ver inventário: clicar em item, armadura, espada e item da mão secundária passa para o seu inventário e some do jogador (dentro do jogo dele). O mesmo no Ver baú do ender.
- [ ] Com o seu inventário cheio, o que não couber volta para o jogador; com o jogador mexendo no inventário, a tela atualiza e nada duplica.
- [ ] Não dá para colocar itens nem arrastar/mover na tela; Voltar funciona.
- [ ] Painel > Banir 30 minutos com motivo: o jogador é expulso, não consegue entrar e a mensagem mostra tempo e motivo; aparece em Banidos e clicar remove.
- [ ] Painel > Silenciar 30 minutos: o jogador não consegue falar; aparece em Silenciados; "Remover silêncio" libera. Expulsar com motivo funciona.
- [ ] 6ª home é recusada; teleporte espera 5s e cancela se andar; `/tpa` + `/tpaccept` entre dois jogadores na sobrevivência.
- [ ] `/tempban Fulano 1m` e `/mute Fulano 1m`; reiniciar mantém homes, blocos de retirada, bans e mutes.

## Créditos
Inspirado em *Minecraft Essentials* de Paoeni (MIT). Nenhum código ou recurso do add-on foi copiado.

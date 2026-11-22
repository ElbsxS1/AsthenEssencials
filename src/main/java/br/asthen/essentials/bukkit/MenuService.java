package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Names;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Builds the menus and opens them in the right style for the player's platform. */
final class MenuService {
    private final AsthenEssentials plugin;
    private final boolean floodgate;

    MenuService(AsthenEssentials plugin) {
        this.plugin = plugin;
        this.floodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    // ------------------------------------------------------------------ rendering
    void open(Player p, Menu menu) {
        if (floodgate) {
            try {
                if (BedrockMenu.isBedrock(p)) {
                    BedrockMenu.open(plugin, p, menu);
                    return;
                }
            } catch (Throwable t) { // Floodgate missing/outdated: fall back to the chest
                plugin.getLogger().warning("Menu Bedrock indisponível, usando baú: " + t);
            }
        }
        JavaMenu.open(p, menu);
    }

    /**
     * Asks the player for a line of text: a form with a text field on Bedrock, the next chat message on Java.
     * onAnswer gets the trimmed text (empty if the player left it blank and optional is true); onCancel runs
     * when the player cancels. Both run on the main thread.
     */
    void ask(Player p, String title, String question, String placeholder, boolean optional,
             Consumer<String> onAnswer, Runnable onCancel) {
        if (floodgate) {
            try {
                if (BedrockMenu.isBedrock(p)) {
                    BedrockMenu.prompt(plugin, p, title, question, placeholder, onAnswer, onCancel);
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Formulário Bedrock indisponível, usando o chat: " + t);
            }
        }
        plugin.prompts().ask(p, question, optional, onAnswer, onCancel);
    }

    private void run(Player p, String command) {
        p.performCommand(command);
    }

    private Menu.Entry back(Player p) {
        return new Menu.Entry("Voltar", Material.ARROW, "Volta ao menu principal.", () -> openMain(p));
    }

    // ------------------------------------------------------------------ menus
    void openMain(Player p) {
        if (!plugin.requireAllowedWorld(p)) return;
        List<Menu.Entry> e = new ArrayList<>();
        e.add(new Menu.Entry("Minhas homes", Material.RED_BED, "Ir, criar ou apagar suas homes.", () -> openHomes(p)));
        if (Bukkit.getPluginManager().getPlugin("LobbyGuard") != null) {
            e.add(new Menu.Entry("Voltar ao lobby", Material.ENDER_PEARL, "Volta ao lobby (fique parado 5s).", () -> run(p, "lob")));
        }
        e.add(new Menu.Entry("Pedir teleporte", Material.PLAYER_HEAD, "Pedir para ir até um jogador.", () -> openPlayers(p)));
        e.add(new Menu.Entry("Configurações", Material.COMPARATOR, "Ajuste como o menu se comporta.", () -> openSettings(p)));
        open(p, new Menu("Menu Asthen", "Bem-vindo! O que você quer fazer?", e));
    }

    void openHomes(Player p) {
        int max = plugin.getConfig().getInt("homes.max", 5);
        List<String> homes = plugin.places().homeNames(p.getUniqueId());
        List<Menu.Entry> e = new ArrayList<>();
        for (String h : homes) {
            e.add(new Menu.Entry(h, Material.RED_BED, "Ir para esta home.", () -> run(p, "home " + h)));
        }
        if (homes.size() < max) {
            e.add(new Menu.Entry("Criar home aqui", Material.LIME_DYE, "Escolha um nome e crie uma home\nna sua posição.", () -> askHomeName(p)));
        }
        if (!homes.isEmpty()) {
            e.add(new Menu.Entry("Apagar uma home", Material.BARRIER, "Escolha qual apagar.", () -> openDeleteHome(p)));
        }
        e.add(back(p));
        open(p, new Menu("Minhas homes", homes.size() + " de " + max + " homes usadas.", e));
    }

    /** Asks for the new home's name, then creates it where the player stands. */
    private void askHomeName(Player p) {
        ask(p, "Nova home", "Que nome você quer dar à home? (letras, números, _ ou -, até 24. Em branco = nome automático)",
                "ex.: casa", true, raw -> createHome(p, raw), () -> openHomes(p));
    }

    private void createHome(Player p, String raw) {
        List<String> homes = plugin.places().homeNames(p.getUniqueId());
        String name = raw.trim().replaceAll("\\s+", "_"); // "Minha casa" -> "Minha_casa"
        if (name.isEmpty()) name = freeHomeName(homes);
        if (!Names.validPlace(name)) {
            Msg.err(p, "Nome inválido. Use letras, números, _ ou -, até 24 caracteres.");
        } else if (homes.contains(name.toLowerCase(Locale.ROOT))) {
            Msg.err(p, "Você já tem uma home chamada '" + name + "'. Escolha outro nome.");
        } else {
            run(p, "sethome " + name);
        }
        openHomes(p);
    }

    private static String freeHomeName(List<String> used) {
        if (!used.contains("home")) return "home";
        for (int i = 2; i < 100; i++) {
            if (!used.contains("home" + i)) return "home" + i;
        }
        return "home";
    }

    void openDeleteHome(Player p) {
        List<Menu.Entry> e = new ArrayList<>();
        for (String h : plugin.places().homeNames(p.getUniqueId())) {
            e.add(new Menu.Entry(h, Material.BARRIER, "Clique para APAGAR.", () -> {
                run(p, "delhome " + h);
                openHomes(p);
            }));
        }
        e.add(new Menu.Entry("Voltar", Material.ARROW, "Volta às homes.", () -> openHomes(p)));
        open(p, new Menu("Apagar home", "Qual home apagar?", e));
    }

    /** Shown to the target when someone asks to teleport to them (unless they turned it off in Configurações). */
    void openTpaRequest(Player target, String fromName) {
        int timeout = Math.max(5, plugin.getConfig().getInt("teleport.tpa-timeout-seconds", 60));
        List<Menu.Entry> e = new ArrayList<>();
        e.add(new Menu.Entry("ACEITAR", Material.LIME_CONCRETE, fromName + " vai até você.", () -> run(target, "tpaccept")));
        e.add(new Menu.Entry("NEGAR", Material.RED_CONCRETE, "Recusa o pedido de " + fromName + ".", () -> run(target, "tpdeny")));
        open(target, new Menu("Pedido de teleporte", fromName + " quer ir até você.\nO pedido expira em " + timeout + "s.", e));
    }

    void openSettings(Player p) {
        boolean on = plugin.settings().tpaWindow(p.getUniqueId());
        List<Menu.Entry> e = new ArrayList<>();
        e.add(new Menu.Entry("Janela de pedidos de TP: " + (on ? "LIGADA" : "DESLIGADA"),
                on ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                on ? "Quando alguém pedir para ir até você, abre\nACEITAR / NEGAR. Clique para desligar\n(aí você usa /tpaccept ou /tpdeny)."
                        : "Você recebe só a mensagem no chat e usa\n/tpaccept ou /tpdeny. Clique para ligar a janela.",
                () -> {
                    plugin.settings().setTpaWindow(p.getUniqueId(), !on);
                    openSettings(p);
                }));
        e.add(back(p));
        open(p, new Menu("Configurações", "Toque para ligar ou desligar.", e));
    }

    void openPlayers(Player p) {
        List<Menu.Entry> e = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            String n = other.getName();
            e.add(new Menu.Entry(n, Material.PLAYER_HEAD, "Pedir para ir até " + n + ".", () -> run(p, "tpa " + n)));
            if (e.size() >= 45) break;
        }
        e.add(back(p));
        open(p, new Menu("Pedir teleporte", e.size() == 1 ? "Não há outros jogadores online." : "Para quem?", e));
    }
}

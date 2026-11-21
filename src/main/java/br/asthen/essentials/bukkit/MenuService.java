package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Places;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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

    private void run(Player p, String command) {
        p.performCommand(command);
    }

    private Menu.Entry back(Player p) {
        return new Menu.Entry("Voltar", Material.ARROW, "Volta ao menu principal.", () -> openMain(p));
    }

    // ------------------------------------------------------------------ menus
    void openMain(Player p) {
        List<Menu.Entry> e = new ArrayList<>();
        e.add(new Menu.Entry("Minhas homes", Material.RED_BED, "Ir, criar ou apagar suas homes.", () -> openHomes(p)));
        e.add(new Menu.Entry("Spawn", Material.ENDER_PEARL, "Ir para o spawn.", () -> run(p, "spawn")));
        e.add(new Menu.Entry("Pedir teleporte", Material.PLAYER_HEAD, "Pedir para ir até um jogador.", () -> openPlayers(p)));
        if (p.hasPermission("asthenessentials.admin")) {
            e.add(new Menu.Entry("Warps", Material.BEACON, "Locais do servidor (só operadores).", () -> openWarps(p)));
        }
        open(p, new Menu("Menu Asthen", "Escolha uma opção:", e));
    }

    void openHomes(Player p) {
        int max = plugin.getConfig().getInt("homes.max", 5);
        List<String> homes = plugin.places().homeNames(p.getUniqueId());
        List<Menu.Entry> e = new ArrayList<>();
        for (String h : homes) {
            e.add(new Menu.Entry(h, Material.RED_BED, "Ir para esta home.", () -> run(p, "home " + h)));
        }
        if (homes.size() < max) {
            e.add(new Menu.Entry("Definir home aqui", Material.LIME_DYE, "Cria uma home na sua posição.", () -> {
                run(p, "sethome " + freeHomeName(homes));
                openHomes(p);
            }));
        }
        if (!homes.isEmpty()) {
            e.add(new Menu.Entry("Apagar uma home", Material.BARRIER, "Escolha qual apagar.", () -> openDeleteHome(p)));
        }
        e.add(back(p));
        open(p, new Menu("Minhas homes", homes.size() + " de " + max + " homes usadas.", e));
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

    void openWarps(Player p) {
        if (!p.hasPermission("asthenessentials.admin")) {
            Msg.err(p, "Sem permissão.");
            return;
        }
        Places places = plugin.places();
        List<Menu.Entry> e = new ArrayList<>();
        for (String w : places.warpNames()) {
            e.add(new Menu.Entry(w, Material.MAP, "Ir para o warp " + w + ".", () -> run(p, "warp " + w)));
            if (e.size() >= 45) break;
        }
        e.add(back(p));
        open(p, new Menu("Warps", e.size() == 1 ? "Nenhum warp criado (use /setwarp)." : "Escolha um warp:", e));
    }
}

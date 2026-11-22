package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Punishments;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin panel: pick a player, then read their inventory / ender chest, kick, mute or ban (with a time).
 * Kick, ban and mute go through the plugin's own commands, so the rules and the log stay in one place.
 */
final class AdminMenu {
    /** Prefixed so a vanilla /ban or /kick can never be picked instead of ours. */
    private static final String PREFIX = "asthenessentials:";

    private record Duration(String label, String token, Material icon) {
    }

    private static final List<Duration> DURATIONS = List.of(
            new Duration("30 minutos", "30m", Material.CLOCK),
            new Duration("2 horas", "2h", Material.CLOCK),
            new Duration("1 dia", "1d", Material.CLOCK),
            new Duration("7 dias", "7d", Material.CLOCK),
            new Duration("30 dias", "30d", Material.CLOCK),
            new Duration("Permanente", "", Material.BEDROCK));

    private final AsthenEssentials plugin;

    AdminMenu(AsthenEssentials plugin) {
        this.plugin = plugin;
    }

    private boolean allowed(Player p) {
        if (p.hasPermission(AdminItem.PERMISSION)) return true;
        Msg.err(p, "Sem permissão.");
        return false;
    }

    private void run(Player p, String command) {
        p.performCommand(PREFIX + command);
    }

    private Menu.Entry back(String label, Runnable to) {
        return new Menu.Entry("Voltar", Material.ARROW, label, to);
    }

    /** The target, or null (after telling the admin and going back to the player list). */
    private Player online(Player admin, UUID id) {
        Player t = Bukkit.getPlayer(id);
        if (t == null) {
            Msg.err(admin, "Esse jogador saiu do servidor.");
            openPlayers(admin);
        }
        return t;
    }

    // ------------------------------------------------------------------ main
    void openMain(Player p) {
        if (!allowed(p)) return;
        List<Menu.Entry> e = new ArrayList<>();
        e.add(new Menu.Entry("Jogadores online", Material.PLAYER_HEAD,
                "Ver inventário e baú do ender,\nexpulsar, silenciar ou banir.", () -> openPlayers(p)));
        e.add(new Menu.Entry("Banidos", Material.BARRIER, "Ver e remover banimentos.", () -> openPunished(p, Punishments.Kind.BAN)));
        e.add(new Menu.Entry("Silenciados", Material.BELL, "Ver e remover silenciamentos.", () -> openPunished(p, Punishments.Kind.MUTE)));
        plugin.menus().open(p, new Menu("Painel Admin", "Escolha uma opção:", e));
    }

    // ------------------------------------------------------------------ players
    void openPlayers(Player p) {
        if (!allowed(p)) return;
        List<Menu.Entry> e = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            UUID id = other.getUniqueId();
            e.add(new Menu.Entry(other.getName(), Material.PLAYER_HEAD, "Clique para gerenciar.", () -> openPlayer(p, id)));
            if (e.size() >= 44) break;
        }
        e.add(back("Volta ao painel.", () -> openMain(p)));
        plugin.menus().open(p, new Menu("Jogadores online", "Escolha um jogador:", e));
    }

    void openPlayer(Player p, UUID id) {
        if (!allowed(p)) return;
        Player t = online(p, id);
        if (t == null) return;
        String name = t.getName();
        List<Menu.Entry> e = new ArrayList<>();
        e.add(new Menu.Entry("Ver inventário", Material.CHEST, "Só leitura: uma cópia do que\n" + name + " tem agora.", () -> viewInventory(p, id)));
        e.add(new Menu.Entry("Ver baú do ender", Material.ENDER_CHEST, "Só leitura.", () -> viewEnder(p, id)));
        if (!t.equals(p)) { // never punish yourself by accident
            if (plugin.punishments().active(Punishments.Kind.MUTE, id) != null) {
                e.add(new Menu.Entry("Remover silêncio", Material.BELL, name + " já está silenciado.\nClique para liberar o chat.", () -> {
                    run(p, "unmute " + name);
                    openPlayer(p, id);
                }));
            } else {
                e.add(new Menu.Entry("Silenciar", Material.PAPER, "Escolher o tempo e o motivo.", () -> openDuration(p, Punishments.Kind.MUTE, id)));
            }
            e.add(new Menu.Entry("Expulsar", Material.LEATHER_BOOTS, "Tira " + name + " do servidor agora\n(ele pode voltar).", () -> askKick(p, id)));
            e.add(new Menu.Entry("Banir", Material.TNT, "Escolher o tempo e o motivo.", () -> openDuration(p, Punishments.Kind.BAN, id)));
        }
        e.add(back("Volta à lista de jogadores.", () -> openPlayers(p)));
        plugin.menus().open(p, new Menu("Gerenciar " + name, "O que fazer com " + name + "?", e));
    }

    // ------------------------------------------------------------------ read-only views
    private void viewInventory(Player p, UUID id) {
        if (!allowed(p)) return;
        Player t = online(p, id);
        if (t == null) return;
        plugin.getLogger().info(p.getName() + " abriu o inventário de " + t.getName());
        InspectMenu.openInventory(p, t, () -> openPlayer(p, id));
    }

    private void viewEnder(Player p, UUID id) {
        if (!allowed(p)) return;
        Player t = online(p, id);
        if (t == null) return;
        plugin.getLogger().info(p.getName() + " abriu o baú do ender de " + t.getName());
        InspectMenu.openEnder(p, t, () -> openPlayer(p, id));
    }

    // ------------------------------------------------------------------ kick
    private void askKick(Player p, UUID id) {
        Player t = online(p, id);
        if (t == null) return;
        String name = t.getName();
        plugin.menus().ask(p, "Expulsar " + name, "Qual o motivo da expulsão de " + name + "?", "ex.: spam", true,
                reason -> {
                    if (online(p, id) == null) return;
                    run(p, withReason("kick " + name, reason));
                    openPlayers(p);
                },
                () -> openPlayer(p, id));
    }

    // ------------------------------------------------------------------ ban / mute: time, then reason
    private void openDuration(Player p, Punishments.Kind kind, UUID id) {
        if (!allowed(p)) return;
        Player t = online(p, id);
        if (t == null) return;
        boolean ban = kind == Punishments.Kind.BAN;
        List<Menu.Entry> e = new ArrayList<>();
        for (Duration d : DURATIONS) {
            e.add(new Menu.Entry(d.label(), d.icon(), (ban ? "Banir " : "Silenciar ") + t.getName() + ": " + d.label() + ".",
                    () -> askReason(p, kind, id, d)));
        }
        e.add(back("Volta ao jogador.", () -> openPlayer(p, id)));
        plugin.menus().open(p, new Menu((ban ? "Banir " : "Silenciar ") + t.getName(), "Por quanto tempo?", e));
    }

    private void askReason(Player p, Punishments.Kind kind, UUID id, Duration d) {
        Player t = online(p, id);
        if (t == null) return;
        boolean ban = kind == Punishments.Kind.BAN;
        String name = t.getName();
        plugin.menus().ask(p, ban ? "Banir " + name : "Silenciar " + name,
                "Qual o motivo para " + (ban ? "banir " : "silenciar ") + name + " (" + d.label() + ")?", "ex.: grief", true,
                reason -> apply(p, kind, id, d, reason),
                () -> openDuration(p, kind, id));
    }

    private void apply(Player p, Punishments.Kind kind, UUID id, Duration d, String reason) {
        if (!allowed(p)) return;
        Player t = online(p, id);
        if (t == null) return;
        String name = t.getName();
        String base;
        if (kind == Punishments.Kind.BAN) base = d.token().isEmpty() ? "ban " + name : "tempban " + name + " " + d.token();
        else base = d.token().isEmpty() ? "mute " + name : "mute " + name + " " + d.token();
        run(p, withReason(base, reason));
        if (kind == Punishments.Kind.BAN) openPlayers(p); // the banned player was kicked
        else openPlayer(p, id);
    }

    private static String withReason(String command, String reason) {
        String why = reason == null ? "" : reason.trim();
        return why.isEmpty() ? command : command + " " + why;
    }

    // ------------------------------------------------------------------ bans / mutes lists
    private void openPunished(Player p, Punishments.Kind kind) {
        if (!allowed(p)) return;
        boolean ban = kind == Punishments.Kind.BAN;
        long now = System.currentTimeMillis();
        List<Menu.Entry> e = new ArrayList<>();
        StringBuilder content = new StringBuilder(ban ? "Clique em um nome para remover o banimento.\n" : "Clique em um nome para remover o silêncio.\n");
        for (Punishments.Entry en : plugin.punishments().list(kind)) {
            String left = en.permanent() ? "Permanente" : "Restam " + Punishments.formatDuration(en.expiresAt() - now);
            e.add(new Menu.Entry(en.name(), ban ? Material.BARRIER : Material.PAPER,
                    "Motivo: " + en.reason() + "\nPor: " + en.by() + "\n" + left + "\nClique para REMOVER.", () -> {
                run(p, (ban ? "unban " : "unmute ") + en.name());
                openPunished(p, kind);
            }));
            content.append("\n").append(en.name()).append(" - ").append(left).append(" - ").append(en.reason());
            if (e.size() >= 44) break;
        }
        if (e.isEmpty()) content = new StringBuilder(ban ? "Ninguém está banido." : "Ninguém está silenciado.");
        e.add(back("Volta ao painel.", () -> openMain(p)));
        plugin.menus().open(p, new Menu(ban ? "Banidos" : "Silenciados", content.toString(), e));
    }
}

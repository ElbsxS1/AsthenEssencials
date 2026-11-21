package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Loc;
import br.asthen.essentials.core.Places;
import br.asthen.essentials.core.Punishments;
import br.asthen.essentials.core.TpaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class Commands implements TabExecutor {
    private final AsthenEssentials plugin;

    Commands(AsthenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        try {
            return switch (cmd.getName().toLowerCase(Locale.ROOT)) {
                case "menu" -> menu(s);
                case "bussola" -> compass(s);
                case "sethome" -> setHome(s, a);
                case "home" -> home(s, a);
                case "delhome" -> delHome(s, a);
                case "homes" -> homes(s);
                case "setwarp" -> setWarp(s, a);
                case "delwarp" -> delWarp(s, a);
                case "warp" -> warp(s, a);
                case "warps" -> warps(s);
                case "setspawn" -> setSpawn(s);
                case "spawn" -> spawn(s);
                case "tpa" -> tpa(s, a);
                case "tpaccept" -> tpaccept(s);
                case "tpdeny" -> tpdeny(s);
                case "kick" -> kick(s, a);
                case "ban" -> ban(s, a, false);
                case "tempban" -> ban(s, a, true);
                case "unban" -> unpunish(s, a, Punishments.Kind.BAN);
                case "mute" -> mute(s, a);
                case "unmute" -> unpunish(s, a, Punishments.Kind.MUTE);
                case "aessentials" -> admin(s, a);
                default -> false;
            };
        } catch (RuntimeException e) {
            Msg.err(s, "Erro interno. Veja o console.");
            plugin.getLogger().severe("Erro em /" + cmd.getName() + ": " + e);
            return true;
        }
    }

    // ------------------------------------------------------------------ helpers
    private static Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        Msg.err(s, "Só jogadores podem usar este comando.");
        return null;
    }

    /** Online player first, then punishment records, then the server's name cache. Null if unknown. */
    private UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        for (Punishments.Kind k : Punishments.Kind.values()) {
            UUID known = plugin.punishments().findByName(k, name);
            if (known != null) return known;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached == null ? null : cached.getUniqueId();
    }

    private String nameOf(UUID id, String fallback) {
        Player p = Bukkit.getPlayer(id);
        return p != null ? p.getName() : fallback;
    }

    private String senderName(CommandSender s) {
        return s instanceof Player p ? p.getName() : "Console";
    }

    private boolean compass(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        if (plugin.menuItem().has(p)) Msg.info(p, "Você já tem a bússola do menu.");
        else if (plugin.menuItem().giveIfMissing(p)) Msg.ok(p, "Bússola do menu entregue.");
        else Msg.err(p, "Inventário cheio. Libere um espaço e tente de novo.");
        return true;
    }

    private boolean menu(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        plugin.menus().openMain(p);
        return true;
    }

    // ------------------------------------------------------------------ homes / warps / spawn
    private boolean setHome(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        String name = a.length > 0 ? a[0] : "home";
        int max = plugin.getConfig().getInt("homes.max", 5);
        switch (plugin.places().setHome(p.getUniqueId(), name, Teleporter.toLoc(p.getLocation()), max)) {
            case OK -> Msg.ok(p, "Home '" + name.toLowerCase() + "' definida.");
            case LIMIT -> Msg.err(p, "Limite de " + max + " homes. Apague uma com /delhome.");
            default -> Msg.err(p, "Nome inválido (use letras, números, _ ou -, até 24).");
        }
        return true;
    }

    private boolean home(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        String name = a.length > 0 ? a[0] : "home";
        Loc l = plugin.places().home(p.getUniqueId(), name);
        if (l == null) {
            Msg.err(p, "Home não encontrada. Veja /homes.");
            return true;
        }
        plugin.teleporter().go(p, Teleporter.toLocation(l), "home " + name.toLowerCase());
        return true;
    }

    private boolean delHome(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        if (a.length != 1) {
            Msg.err(s, "Uso: /delhome <nome>");
            return true;
        }
        if (plugin.places().delHome(p.getUniqueId(), a[0]) == Places.Result.OK) Msg.ok(p, "Home apagada.");
        else Msg.err(p, "Home não encontrada.");
        return true;
    }

    private boolean homes(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        List<String> h = plugin.places().homeNames(p.getUniqueId());
        Msg.info(p, h.isEmpty() ? "Você não tem homes. Use /sethome." : "Suas homes: " + String.join(", ", h));
        return true;
    }

    private boolean setWarp(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        if (a.length != 1) {
            Msg.err(s, "Uso: /setwarp <nome>");
            return true;
        }
        if (plugin.places().setWarp(a[0], Teleporter.toLoc(p.getLocation())) == Places.Result.OK)
            Msg.ok(p, "Warp '" + a[0].toLowerCase() + "' salvo.");
        else Msg.err(p, "Nome inválido (use letras, números, _ ou -, até 24).");
        return true;
    }

    private boolean delWarp(CommandSender s, String[] a) {
        if (a.length != 1) {
            Msg.err(s, "Uso: /delwarp <nome>");
            return true;
        }
        if (plugin.places().delWarp(a[0]) == Places.Result.OK) Msg.ok(s, "Warp apagado.");
        else Msg.err(s, "Warp não encontrado.");
        return true;
    }

    private boolean warp(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        if (a.length != 1) {
            Msg.err(s, "Uso: /warp <nome>. Veja /warps.");
            return true;
        }
        Loc l = plugin.places().warp(a[0]);
        if (l == null) {
            Msg.err(p, "Warp não encontrado. Veja /warps.");
            return true;
        }
        plugin.teleporter().go(p, Teleporter.toLocation(l), "warp " + a[0].toLowerCase());
        return true;
    }

    private boolean warps(CommandSender s) {
        List<String> w = plugin.places().warpNames();
        Msg.info(s, w.isEmpty() ? "Nenhum warp criado." : "Warps: " + String.join(", ", w));
        return true;
    }

    private boolean setSpawn(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        plugin.places().setSpawn(Teleporter.toLoc(p.getLocation()));
        Msg.ok(p, "Spawn definido aqui.");
        return true;
    }

    private boolean spawn(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        Loc l = plugin.places().spawn();
        Location target = l != null ? Teleporter.toLocation(l) : p.getWorld().getSpawnLocation();
        plugin.teleporter().go(p, target, "o spawn");
        return true;
    }

    // ------------------------------------------------------------------ tpa
    private boolean tpa(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        if (a.length != 1) {
            Msg.err(s, "Uso: /tpa <jogador>");
            return true;
        }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) {
            Msg.err(s, "Jogador não está online.");
            return true;
        }
        if (t.equals(p)) {
            Msg.err(s, "Você não pode pedir teleporte para si mesmo.");
            return true;
        }
        plugin.tpa().create(p.getUniqueId(), t.getUniqueId());
        Msg.ok(p, "Pedido enviado para " + t.getName() + ".");
        Msg.info(t, p.getName() + " quer ir até você. Use /tpaccept ou /tpdeny.");
        return true;
    }

    private boolean tpaccept(CommandSender s) {
        Player t = player(s);
        if (t == null) return true;
        TpaManager.Request r = plugin.tpa().take(t.getUniqueId());
        if (r == null) {
            Msg.err(t, "Você não tem pedidos pendentes.");
            return true;
        }
        Player from = Bukkit.getPlayer(r.from());
        if (from == null) {
            Msg.err(t, "Quem pediu saiu do servidor.");
            return true;
        }
        Msg.ok(t, "Pedido aceito.");
        plugin.teleporter().go(from, t.getLocation(), t.getName());
        return true;
    }

    private boolean tpdeny(CommandSender s) {
        Player t = player(s);
        if (t == null) return true;
        TpaManager.Request r = plugin.tpa().take(t.getUniqueId());
        if (r == null) {
            Msg.err(t, "Você não tem pedidos pendentes.");
            return true;
        }
        Msg.ok(t, "Pedido recusado.");
        Player from = Bukkit.getPlayer(r.from());
        if (from != null) Msg.err(from, t.getName() + " recusou seu pedido.");
        return true;
    }

    // ------------------------------------------------------------------ moderation
    private String reason(String[] a, int from) {
        if (a.length <= from) return plugin.getConfig().getString("moderation.default-reason", "Sem motivo");
        return String.join(" ", java.util.Arrays.copyOfRange(a, from, a.length));
    }

    private boolean kick(CommandSender s, String[] a) {
        if (a.length < 1) {
            Msg.err(s, "Uso: /kick <jogador> [motivo]");
            return true;
        }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) {
            Msg.err(s, "Jogador não está online.");
            return true;
        }
        String why = reason(a, 1);
        t.kick(Component.text("Você foi expulso.\nMotivo: " + why, NamedTextColor.RED));
        Msg.ok(s, t.getName() + " expulso.");
        plugin.getLogger().info(senderName(s) + " kick " + t.getName() + ": " + why);
        return true;
    }

    private boolean ban(CommandSender s, String[] a, boolean timed) {
        int min = timed ? 2 : 1;
        if (a.length < min) {
            Msg.err(s, timed ? "Uso: /tempban <jogador> <tempo> [motivo]  (ex.: 30m, 2h, 7d)"
                    : "Uso: /ban <jogador> [motivo]");
            return true;
        }
        UUID id = resolve(a[0]);
        if (id == null) {
            Msg.err(s, "Jogador não encontrado (ele precisa já ter entrado no servidor).");
            return true;
        }
        long dur = 0;
        int reasonFrom = 1;
        if (timed) {
            dur = Punishments.parseDuration(a[1]);
            if (dur < 0) {
                Msg.err(s, "Tempo inválido. Use 30m, 2h, 7d (máximo 365d).");
                return true;
            }
            reasonFrom = 2;
        }
        String why = reason(a, reasonFrom);
        String name = nameOf(id, a[0]);
        Punishments.Entry e = plugin.punishments().add(Punishments.Kind.BAN, id, name, why, senderName(s), dur);
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            online.kick(Component.text("Você foi banido" + (e.permanent() ? "" : " por " + Punishments.formatDuration(dur))
                    + ".\nMotivo: " + why, NamedTextColor.RED));
        }
        Msg.ok(s, name + " banido" + (e.permanent() ? " permanentemente" : " por " + Punishments.formatDuration(dur)) + ".");
        plugin.getLogger().info(senderName(s) + " ban " + name + " (" + (timed ? a[1] : "perm") + "): " + why);
        return true;
    }

    private boolean mute(CommandSender s, String[] a) {
        if (a.length < 1) {
            Msg.err(s, "Uso: /mute <jogador> [tempo] [motivo]");
            return true;
        }
        UUID id = resolve(a[0]);
        if (id == null) {
            Msg.err(s, "Jogador não encontrado.");
            return true;
        }
        long dur = a.length >= 2 ? Punishments.parseDuration(a[1]) : 0;
        int reasonFrom = dur > 0 ? 2 : 1;
        String name = nameOf(id, a[0]);
        Punishments.Entry e = plugin.punishments().add(Punishments.Kind.MUTE, id, name, reason(a, reasonFrom),
                senderName(s), Math.max(0, dur));
        Player online = Bukkit.getPlayer(id);
        if (online != null) Msg.err(online, "Você foi silenciado. Motivo: " + e.reason());
        Msg.ok(s, name + " silenciado" + (e.permanent() ? "." : " por " + Punishments.formatDuration(dur) + "."));
        return true;
    }

    private boolean unpunish(CommandSender s, String[] a, Punishments.Kind kind) {
        if (a.length != 1) {
            Msg.err(s, "Uso: /" + (kind == Punishments.Kind.BAN ? "unban" : "unmute") + " <jogador>");
            return true;
        }
        UUID id = plugin.punishments().findByName(kind, a[0]);
        if (id == null) id = resolve(a[0]);
        if (id != null && plugin.punishments().remove(kind, id)) Msg.ok(s, "Removido para " + a[0] + ".");
        else Msg.err(s, a[0] + " não está " + (kind == Punishments.Kind.BAN ? "banido." : "silenciado."));
        return true;
    }

    private boolean admin(CommandSender s, String[] a) {
        if (a.length == 1 && a[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            Msg.ok(s, "Configuração recarregada.");
        } else {
            Msg.err(s, "Uso: /aessentials reload");
        }
        return true;
    }

    // ------------------------------------------------------------------ tab
    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        String cur = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        boolean playerArg = switch (name) {
            case "tpa", "kick", "ban", "tempban", "mute" -> a.length == 1;
            default -> false;
        };
        if (playerArg) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if ((name.equals("home") || name.equals("delhome")) && a.length == 1 && s instanceof Player p) {
            out.addAll(plugin.places().homeNames(p.getUniqueId()));
        } else if ((name.equals("warp") || name.equals("delwarp")) && a.length == 1) {
            out.addAll(plugin.places().warpNames());
        } else if (name.equals("aessentials") && a.length == 1) {
            out.add("reload");
        }
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(cur));
        return out;
    }
}

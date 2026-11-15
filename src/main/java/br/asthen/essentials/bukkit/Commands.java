package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Economy;
import br.asthen.essentials.core.Loc;
import br.asthen.essentials.core.Money;
import br.asthen.essentials.core.Places;
import br.asthen.essentials.core.Punishments;
import br.asthen.essentials.core.TpaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class Commands implements TabExecutor {
    private static final Set<Material> UNSAFE = Set.of(Material.LAVA, Material.WATER, Material.MAGMA_BLOCK,
            Material.CACTUS, Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE);

    private final AsthenEssentials plugin;

    Commands(AsthenEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        try {
            return switch (cmd.getName().toLowerCase(Locale.ROOT)) {
                case "money" -> money(s, a);
                case "pay" -> pay(s, a);
                case "baltop" -> baltop(s);
                case "eco" -> eco(s, a);
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
                case "back" -> back(s);
                case "rtp" -> rtp(s);
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

    /** Online player first, then anyone the economy has seen, then the server's cache. Null if unknown. */
    private UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        UUID known = plugin.economy().findByName(name);
        if (known != null) return known;
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached == null ? null : cached.getUniqueId();
    }

    private String nameOf(UUID id, String fallback) {
        Player p = Bukkit.getPlayer(id);
        return p != null ? p.getName() : plugin.economy().has(id) ? plugin.economy().nameOf(id) : fallback;
    }

    private String senderName(CommandSender s) {
        return s instanceof Player p ? p.getName() : "Console";
    }

    // ------------------------------------------------------------------ economy
    private boolean money(CommandSender s, String[] a) {
        UUID id;
        String name;
        if (a.length >= 1) {
            id = resolve(a[0]);
            if (id == null) {
                Msg.err(s, "Jogador não encontrado.");
                return true;
            }
            name = nameOf(id, a[0]);
        } else {
            Player p = player(s);
            if (p == null) return true;
            id = p.getUniqueId();
            name = "Seu";
        }
        long bal = plugin.economy().balance(id);
        Msg.info(s, (a.length >= 1 ? "Saldo de " + name : "Seu saldo") + ": " + plugin.money(bal));
        return true;
    }

    private boolean pay(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        if (a.length != 2) {
            Msg.err(s, "Uso: /pay <jogador> <valor>");
            return true;
        }
        UUID to = resolve(a[0]);
        if (to == null) {
            Msg.err(s, "Jogador não encontrado.");
            return true;
        }
        long cents = Money.parse(a[1]);
        if (cents <= 0) {
            Msg.err(s, "Valor inválido. Exemplo: 25 ou 25,50.");
            return true;
        }
        switch (plugin.economy().transfer(p.getUniqueId(), to, cents)) {
            case OK -> {
                String tn = nameOf(to, a[0]);
                Msg.ok(p, "Você enviou " + plugin.money(cents) + " para " + tn + ".");
                Player target = Bukkit.getPlayer(to);
                if (target != null) Msg.ok(target, p.getName() + " enviou " + plugin.money(cents) + " para você.");
            }
            case NO_FUNDS -> Msg.err(p, "Saldo insuficiente.");
            case SAME_ACCOUNT -> Msg.err(p, "Você não pode enviar dinheiro para si mesmo.");
            case OVERFLOW -> Msg.err(p, "O saldo do destinatário passaria do limite.");
            default -> Msg.err(p, "Valor inválido.");
        }
        return true;
    }

    private boolean baltop(CommandSender s) {
        int n = Math.max(1, plugin.getConfig().getInt("economy.baltop-size", 10));
        List<Economy.Entry> top = plugin.economy().top(n);
        Msg.info(s, "Mais ricos:");
        int i = 1;
        for (Economy.Entry e : top) {
            s.sendMessage(Component.text(i++ + ". " + e.name() + " - " + plugin.money(e.cents()), NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean eco(CommandSender s, String[] a) {
        if (a.length != 3) {
            Msg.err(s, "Uso: /eco <give|take|set> <jogador> <valor>");
            return true;
        }
        UUID id = resolve(a[1]);
        if (id == null) {
            Msg.err(s, "Jogador não encontrado.");
            return true;
        }
        long cents = Money.parse(a[2]);
        boolean isSet = a[0].equalsIgnoreCase("set");
        if (cents < 0 || (!isSet && cents == 0)) {
            Msg.err(s, "Valor inválido.");
            return true;
        }
        Economy.Result r = switch (a[0].toLowerCase(Locale.ROOT)) {
            case "give" -> plugin.economy().give(id, cents);
            case "take" -> plugin.economy().take(id, cents);
            case "set" -> plugin.economy().set(id, cents);
            default -> null;
        };
        if (r == null) {
            Msg.err(s, "Use give, take ou set.");
        } else if (r == Economy.Result.OK) {
            Msg.ok(s, "Saldo de " + nameOf(id, a[1]) + " agora: " + plugin.money(plugin.economy().balance(id)));
            plugin.getLogger().info(senderName(s) + " /eco " + a[0] + " " + a[1] + " " + a[2]);
        } else {
            Msg.err(s, r == Economy.Result.NO_FUNDS ? "O jogador não tem esse saldo." : "Valor fora do limite.");
        }
        return true;
    }

    // ------------------------------------------------------------------ homes / warps / spawn
    private boolean setHome(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return true;
        String name = a.length > 0 ? a[0] : "home";
        int max = plugin.getConfig().getInt("homes.max", 3);
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
        plugin.teleporter().go(p, Teleporter.toLocation(l), "tp", "home " + name.toLowerCase());
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
        plugin.teleporter().go(p, Teleporter.toLocation(l), "tp", "warp " + a[0].toLowerCase());
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
        plugin.teleporter().go(p, target, "tp", "o spawn");
        return true;
    }

    // ------------------------------------------------------------------ tpa / back
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
        plugin.teleporter().go(from, t.getLocation(), "tp", t.getName());
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

    private boolean back(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        Loc l = plugin.teleporter().backOf(p.getUniqueId());
        if (l == null) {
            Msg.err(p, "Não há local para voltar.");
            return true;
        }
        plugin.teleporter().go(p, Teleporter.toLocation(l), "tp", "o local anterior");
        return true;
    }

    // ------------------------------------------------------------------ rtp
    private boolean rtp(CommandSender s) {
        Player p = player(s);
        if (p == null) return true;
        World w = p.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) {
            Msg.err(p, "O /rtp só funciona no mundo normal (Overworld).");
            return true;
        }
        long left = plugin.cooldowns().remaining("rtp:" + p.getUniqueId());
        if (left > 0) {
            Msg.err(p, "Aguarde " + left + "s para usar /rtp de novo.");
            return true;
        }
        Msg.info(p, "Procurando um lugar seguro...");
        rtpAttempt(p, w, 0);
        return true;
    }

    private void rtpAttempt(Player p, World w, int n) {
        int max = Math.max(1, plugin.getConfig().getInt("rtp.attempts", 12));
        if (!p.isOnline()) return;
        if (n >= max) {
            Msg.err(p, "Não achei um lugar seguro. Tente de novo.");
            return;
        }
        int r = Math.max(100, plugin.getConfig().getInt("rtp.radius", 3000));
        Location c = w.getSpawnLocation();
        int x = c.getBlockX() + ThreadLocalRandom.current().nextInt(-r, r + 1);
        int z = c.getBlockZ() + ThreadLocalRandom.current().nextInt(-r, r + 1);
        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            int y = w.getHighestBlockYAt(x, z);
            Material ground = w.getBlockAt(x, y, z).getType();
            if (UNSAFE.contains(ground) || w.getBlockAt(x, y, z).isLiquid() || y <= w.getMinHeight() + 1) {
                rtpAttempt(p, w, n + 1);
                return;
            }
            Location target = new Location(w, x + 0.5, y + 1, z + 0.5);
            plugin.cooldowns().start("rtp:" + p.getUniqueId(), plugin.getConfig().getInt("rtp.cooldown-seconds", 300));
            plugin.teleporter().go(p, target, "rtpgo", "um lugar aleatório");
        }));
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
            case "money", "pay", "tpa", "kick", "ban", "tempban", "mute" -> a.length == 1;
            case "eco" -> a.length == 2;
            default -> false;
        };
        if (playerArg) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (name.equals("eco") && a.length == 1) {
            out.addAll(List.of("give", "take", "set"));
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

package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Cooldowns;
import br.asthen.essentials.core.Loc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Warmup + cooldown for every teleport the plugin does. */
final class Teleporter {
    private final AsthenEssentials plugin;
    private final Cooldowns cooldowns;
    private final Map<UUID, Integer> pending = new HashMap<>();

    Teleporter(AsthenEssentials plugin, Cooldowns cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
    }

    static Loc toLoc(Location l) {
        return new Loc(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    static Location toLocation(Loc l) {
        World w = Bukkit.getWorld(l.world());
        return w == null ? null : new Location(w, l.x(), l.y(), l.z(), l.yaw(), l.pitch());
    }

    void forget(UUID id) {
        Integer t = pending.remove(id);
        if (t != null) Bukkit.getScheduler().cancelTask(t);
    }

    /** Teleports after the configured warmup; moving cancels it. Runs on the main thread only. */
    void go(Player p, Location target, String what) {
        if (target == null || target.getWorld() == null) {
            Msg.err(p, "O mundo desse destino não está carregado.");
            return;
        }
        if (pending.containsKey(p.getUniqueId())) {
            Msg.err(p, "Você já tem um teleporte em andamento.");
            return;
        }
        String key = "tp:" + p.getUniqueId();
        long left = cooldowns.remaining(key);
        if (left > 0) {
            Msg.err(p, "Aguarde " + left + "s para teleportar de novo.");
            return;
        }
        int warmup = Math.max(0, plugin.getConfig().getInt("teleport.warmup-seconds", 5));
        if (warmup == 0) {
            finish(p, target, key, what);
            return;
        }
        Location start = p.getLocation();
        Msg.info(p, "Teleportando para " + what + " em " + warmup + "s. Não se mexa!");
        int id = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(p.getUniqueId());
            if (!p.isOnline()) return;
            Location now = p.getLocation();
            boolean moved = !now.getWorld().equals(start.getWorld())
                    || now.getBlockX() != start.getBlockX() || now.getBlockY() != start.getBlockY()
                    || now.getBlockZ() != start.getBlockZ();
            if (moved) {
                Msg.err(p, "Teleporte cancelado: você se mexeu.");
                return;
            }
            finish(p, target, key, what);
        }, warmup * 20L).getTaskId();
        pending.put(p.getUniqueId(), id);
    }

    private void finish(Player p, Location target, String key, String what) {
        p.teleportAsync(target).thenAccept(ok -> {
            if (ok) {
                cooldowns.start(key, plugin.getConfig().getInt("teleport.cooldown-seconds", 10));
                Msg.ok(p, "Teleportado para " + what + ".");
            } else {
                Msg.err(p, "Não foi possível teleportar.");
            }
        });
    }
}

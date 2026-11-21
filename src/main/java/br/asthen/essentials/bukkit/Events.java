package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Punishments;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class Events implements Listener {
    private final AsthenEssentials plugin;

    Events(AsthenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        Punishments.Entry b = plugin.punishments().active(Punishments.Kind.BAN, e.getUniqueId());
        if (b == null) return;
        String until = b.permanent() ? "permanentemente"
                : "por mais " + Punishments.formatDuration(b.expiresAt() - System.currentTimeMillis());
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                Component.text("Você está banido " + until + ".\nMotivo: " + b.reason(), NamedTextColor.RED));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.teleporter().forget(e.getPlayer().getUniqueId());
        plugin.tpa().forget(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Punishments.Entry m = plugin.punishments().active(Punishments.Kind.MUTE, e.getPlayer().getUniqueId());
        if (m == null) return;
        e.setCancelled(true);
        String t = m.permanent() ? "" : " (" + Punishments.formatDuration(m.expiresAt() - System.currentTimeMillis()) + ")";
        e.getPlayer().sendMessage(Component.text("Você está silenciado" + t + ". Motivo: " + m.reason(), NamedTextColor.RED));
    }
}

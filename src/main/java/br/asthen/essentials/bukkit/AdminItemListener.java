package br.asthen.essentials.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Right-click with the Painel Admin item opens the admin panel (any world, admins only). */
final class AdminItemListener implements Listener {
    private final AsthenEssentials plugin;
    private final AdminItem item;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    AdminItemListener(AsthenEssentials plugin, AdminItem item) {
        this.plugin = plugin;
        this.item = item;
    }

    /** Someone who is no longer an admin (deopped, permission removed) must not keep the item. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = e.getPlayer();
            if (p.isOnline() && !p.hasPermission(AdminItem.PERMISSION)) item.purge(p);
        }, 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return; // ignore the off-hand duplicate
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!item.is(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        // Bedrock can fire the interact twice: ignore a second use within half a second.
        long now = System.currentTimeMillis();
        Long last = lastUse.put(p.getUniqueId(), now);
        if (last != null && now - last < 500) return;
        if (!p.hasPermission(AdminItem.PERMISSION)) {
            Msg.err(p, "Sem permissão. Este item é só para administradores.");
            return;
        }
        plugin.adminMenu().openMain(p);
    }

    void forget(UUID id) {
        lastUse.remove(id);
    }
}

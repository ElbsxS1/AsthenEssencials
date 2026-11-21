package br.asthen.essentials.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Right-click with the item opens the menu (survival only). Right-click on a registered block
 * (a chest in the lobby) hands the item out. The item is a normal item: it can be dropped or stored.
 */
final class MenuItemListener implements Listener {
    private final AsthenEssentials plugin;
    private final MenuItem item;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    MenuItemListener(AsthenEssentials plugin, MenuItem item) {
        this.plugin = plugin;
        this.item = item;
    }

    /** Bedrock can fire the interact twice: ignore a second use within half a second. */
    private boolean debounce(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastUse.get(p.getUniqueId());
        if (last != null && now - last < 500) return true;
        lastUse.put(p.getUniqueId(), now);
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (e.getPlayer().isOnline()) item.purgeLegacy(e.getPlayer());
        }, 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return; // ignore the off-hand duplicate
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();

        Block b = e.getClickedBlock();
        if (b != null && plugin.pickups().has(b.getWorld().getName(), b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(true); // do not open the chest, just hand out the item
            if (debounce(p)) return;
            if (item.has(p)) Msg.info(p, "Você já tem o Menu Asthen.");
            else if (item.give(p)) Msg.ok(p, "Você pegou o Menu Asthen. Use-o na sobrevivência.");
            else Msg.err(p, "Inventário cheio. Libere um espaço e tente de novo.");
            return;
        }

        if (!item.is(e.getItem())) return;
        e.setCancelled(true);
        if (debounce(p)) return;
        if (!plugin.inAllowedWorld(p)) {
            Msg.err(p, "O menu só funciona na sobrevivência.");
            return;
        }
        plugin.menus().openMain(p);
    }

    /** Breaking a registered block unregisters it. */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (plugin.pickups().remove(b.getWorld().getName(), b.getX(), b.getY(), b.getZ())) {
            Msg.info(e.getPlayer(), "Este bloco não entrega mais o Menu Asthen.");
        }
    }
}

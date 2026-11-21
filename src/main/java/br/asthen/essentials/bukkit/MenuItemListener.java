package br.asthen.essentials.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Right-click opens the menu; the compass cannot be dropped, stored or kept as a death drop. */
final class MenuItemListener implements Listener {
    private final AsthenEssentials plugin;
    private final MenuItem item;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    MenuItemListener(AsthenEssentials plugin, MenuItem item) {
        this.plugin = plugin;
        this.item = item;
    }

    private void giveLater(Player p, long ticks) {
        if (!plugin.getConfig().getBoolean("compass.give-on-join", true)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !item.giveIfMissing(p)) Msg.err(p, "Inventário cheio: use /bussola quando tiver espaço.");
        }, ticks);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        giveLater(e.getPlayer(), 20);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        giveLater(e.getPlayer(), 5);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(item::is);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return; // ignore the off-hand duplicate
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!item.is(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        long now = System.currentTimeMillis(); // Bedrock can fire the interact twice
        Long last = lastUse.get(p.getUniqueId());
        if (last != null && now - last < 500) return;
        lastUse.put(p.getUniqueId(), now);
        plugin.menus().openMain(p);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (item.is(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        var top = e.getView().getTopInventory();
        if (top.getType() == InventoryType.CRAFTING) return; // only the player's own inventory is open
        boolean inTop = e.getClickedInventory() == top;
        if (item.is(e.getCurrentItem()) && (inTop || e.isShiftClick())) e.setCancelled(true);
        if (item.is(e.getCursor()) && inTop) e.setCancelled(true);
        if (e.getClick() == ClickType.NUMBER_KEY && inTop && e.getWhoClicked() instanceof Player p
                && item.is(p.getInventory().getItem(e.getHotbarButton()))) e.setCancelled(true);
    }
}

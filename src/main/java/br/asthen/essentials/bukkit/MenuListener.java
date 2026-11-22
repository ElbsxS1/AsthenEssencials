package br.asthen.essentials.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

/** Menu items are buttons: nothing can be taken, moved or dropped. The same goes for the read-only admin views. */
final class MenuListener implements Listener {
    private final Plugin plugin;

    MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder top = e.getView().getTopInventory().getHolder();

        if (top instanceof InspectMenu.Holder view) {
            e.setCancelled(true); // read-only: no taking, moving, swapping or shift-clicking
            if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
            if (e.getSlot() == view.backSlot && e.getWhoClicked() instanceof Player p) {
                p.closeInventory();
                Bukkit.getScheduler().runTask(plugin, view.back);
            }
            return;
        }

        if (!(top instanceof JavaMenu.Holder holder)) return;
        e.setCancelled(true); // also blocks shift-click from the player's own inventory
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getSlot();
        if (slot < 0 || slot >= holder.menu.entries().size() || !(e.getWhoClicked() instanceof Player p)) return;
        Runnable action = holder.menu.entries().get(slot).action();
        p.closeInventory();
        Bukkit.getScheduler().runTask(plugin, action); // never open/close inventories inside the click event
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder top = e.getView().getTopInventory().getHolder();
        if (top instanceof JavaMenu.Holder || top instanceof InspectMenu.Holder) e.setCancelled(true);
    }
}

package br.asthen.essentials.bukkit;

import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * The Menu Asthen (Nether Star) and Painel Admin (Blaze Rod) are tagged items, but the game still treats them as
 * their material: a star becomes a beacon, a rod becomes blaze powder or furnace fuel. None of that may happen.
 * They can still be dropped, stored and carried; they just cannot be consumed or turned into something else.
 */
final class ItemGuard implements Listener {
    private final MenuItem menuItem;
    private final AdminItem adminItem;

    ItemGuard(MenuItem menuItem, AdminItem adminItem) {
        this.menuItem = menuItem;
        this.adminItem = adminItem;
    }

    private boolean special(ItemStack it) {
        return it != null && (menuItem.is(it) || adminItem.is(it));
    }

    private boolean anySpecial(ItemStack[] items) {
        for (ItemStack it : items) {
            if (special(it)) return true;
        }
        return false;
    }

    /** Crafting table and the 2x2 grid: no result is offered, so there is nothing to take. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        if (anySpecial(inv.getMatrix())) inv.setResult(null);
    }

    /** Safety net in case a result slips through (shift-click, recipe book, other plugins). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent e) {
        if (!anySpecial(e.getInventory().getMatrix())) return;
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p) Msg.err(p, "Este item especial não pode ser usado em receitas.");
    }

    /** The Crafter block (hopper-fed). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrafter(CrafterCraftEvent e) {
        if (e.getBlock().getState() instanceof Crafter c && anySpecial(c.getInventory().getContents())) e.setCancelled(true);
    }

    /** A Blaze Rod is furnace fuel: the furnace must not burn it. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBurn(FurnaceBurnEvent e) {
        if (special(e.getFuel())) e.setCancelled(true);
    }

    /** Anvil: no renaming, repairing or combining (it would copy or strip the tag). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent e) {
        var inv = e.getInventory();
        if (special(inv.getFirstItem()) || special(inv.getSecondItem())) e.setResult(null);
    }
}

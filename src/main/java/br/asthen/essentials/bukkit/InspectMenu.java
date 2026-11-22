package br.asthen.essentials.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Read-only views of another player's inventory and ender chest. The admin sees a COPY taken when the
 * view opens: every click is cancelled (see MenuListener), so nothing can be taken, moved or changed.
 */
final class InspectMenu {
    private InspectMenu() {
    }

    static final class Holder implements InventoryHolder {
        final int backSlot;
        final Runnable back;
        private Inventory inventory;

        Holder(int backSlot, Runnable back) {
            this.backSlot = backSlot;
            this.back = back;
        }

        void bind(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static ItemStack copy(ItemStack it) {
        return it == null ? null : it.clone();
    }

    private static ItemStack labelled(Material m, String name, NamedTextColor color) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack filler() {
        return labelled(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
    }

    private static ItemStack backButton() {
        return labelled(Material.ARROW, "Voltar", NamedTextColor.GOLD);
    }

    /** Rows 1-3: inventory, row 4: hotbar, row 5: armor + off-hand, row 6: back button. */
    static void openInventory(Player admin, Player target, Runnable back) {
        Holder holder = new Holder(49, back);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Inventário de " + target.getName() + " (leitura)"));
        holder.bind(inv);

        PlayerInventory src = target.getInventory();
        ItemStack[] storage = src.getStorageContents(); // 0-8 hotbar, 9-35 main inventory
        for (int i = 9; i < 36 && i < storage.length; i++) inv.setItem(i - 9, copy(storage[i]));
        for (int i = 0; i < 9 && i < storage.length; i++) inv.setItem(27 + i, copy(storage[i]));

        ItemStack[] armor = src.getArmorContents(); // boots, leggings, chestplate, helmet
        String[] armorNames = {"Capacete", "Peitoral", "Calças", "Botas"};
        for (int i = 0; i < 4; i++) {
            ItemStack piece = i < armor.length ? armor[3 - i] : null;
            inv.setItem(36 + i, piece == null ? labelled(Material.LIGHT_GRAY_STAINED_GLASS_PANE, armorNames[i] + " (vazio)", NamedTextColor.GRAY)
                    : copy(piece));
        }
        ItemStack off = src.getItemInOffHand();
        inv.setItem(40, off == null || off.getType().isAir()
                ? labelled(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Mão secundária (vazio)", NamedTextColor.GRAY) : copy(off));

        for (int s = 41; s < 54; s++) inv.setItem(s, filler());
        inv.setItem(49, backButton());
        admin.openInventory(inv);
    }

    static void openEnder(Player admin, Player target, Runnable back) {
        Holder holder = new Holder(31, back);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Baú do Ender de " + target.getName() + " (leitura)"));
        holder.bind(inv);
        ItemStack[] contents = target.getEnderChest().getContents();
        for (int i = 0; i < 27 && i < contents.length; i++) inv.setItem(i, copy(contents[i]));
        for (int s = 27; s < 36; s++) inv.setItem(s, filler());
        inv.setItem(31, backButton());
        admin.openInventory(inv);
    }
}

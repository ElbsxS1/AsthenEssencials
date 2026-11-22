package br.asthen.essentials.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Chest-style menu for Java players. */
final class JavaMenu {
    private JavaMenu() {
    }

    static final class Holder implements InventoryHolder {
        final Menu menu;
        private Inventory inventory;

        Holder(Menu menu) {
            this.menu = menu;
        }

        void bind(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    static void open(Player p, Menu menu) {
        int n = Math.min(menu.entries().size(), 54);
        int rows = Math.max(1, (n + 8) / 9);
        Holder holder = new Holder(menu);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, Component.text(menu.title()));
        holder.bind(inv);
        for (int i = 0; i < n; i++) {
            Menu.Entry e = menu.entries().get(i);
            ItemStack it = new ItemStack(e.icon());
            ItemMeta meta = it.getItemMeta();
            meta.displayName(plain(e.label(), NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            for (String line : e.description().split("\n")) lore.add(plain(line, NamedTextColor.GRAY));
            meta.lore(lore);
            it.setItemMeta(meta);
            inv.setItem(i, it);
        }
        p.openInventory(inv);
    }
}

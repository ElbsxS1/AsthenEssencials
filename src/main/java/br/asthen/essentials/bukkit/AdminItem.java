package br.asthen.essentials.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** The "Painel Admin" item. Identified by a hidden tag; only usable with asthenessentials.admin. */
final class AdminItem {
    static final String PERMISSION = "asthenessentials.admin";

    private final AsthenEssentials plugin;
    private final NamespacedKey key;

    AdminItem(AsthenEssentials plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "admin_item");
    }

    Material material() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("admin-item.material", "BLAZE_ROD"));
        return m != null && m.isItem() ? m : Material.BLAZE_ROD;
    }

    ItemStack create() {
        ItemStack it = new ItemStack(material());
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text("Painel Admin", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Botão direito: gerenciar jogadores.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    boolean is(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    boolean has(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (is(it)) return true;
        }
        return false;
    }

    /** Adds one item to the inventory. Returns false when there was no room. */
    boolean give(Player p) {
        return p.getInventory().addItem(create()).isEmpty();
    }

    /** Removes every Painel Admin item from the inventory (used for players who are not admins). */
    void purge(Player p) {
        var inv = p.getInventory();
        ItemStack[] all = inv.getContents();
        for (int i = 0; i < all.length; i++) {
            if (is(all[i])) inv.setItem(i, null);
        }
    }
}

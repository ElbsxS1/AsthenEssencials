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

/** The "Menu Asthen" item. Identified by a hidden tag, never by its name or material. */
final class MenuItem {
    private final AsthenEssentials plugin;
    private final NamespacedKey key;

    MenuItem(AsthenEssentials plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    /** Configured material. NETHER_STAR by default: the compass is taken by WorldEdit's navigation wand. */
    Material material() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("menu-item.material", "NETHER_STAR"));
        return m != null && m.isItem() ? m : Material.NETHER_STAR;
    }

    ItemStack create() {
        ItemStack it = new ItemStack(material());
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text("Menu Asthen", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Clique com o botão direito para abrir o menu.", NamedTextColor.GRAY)
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

    /** Removes menu items of an older version (the 0.3.0 compass) so nothing stale stays behind. */
    void purgeLegacy(Player p) {
        var inv = p.getInventory();
        ItemStack[] all = inv.getContents();
        for (int i = 0; i < all.length; i++) {
            if (is(all[i]) && all[i].getType() != material()) inv.setItem(i, null);
        }
    }
}

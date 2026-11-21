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

/** The "Menu Asthen" compass. Identified by a hidden tag, never by its name. */
final class MenuItem {
    static final int HOTBAR_SLOT = 8;
    private final NamespacedKey key;

    MenuItem(AsthenEssentials plugin) {
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    ItemStack create() {
        ItemStack it = new ItemStack(Material.COMPASS);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text("Menu Asthen", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Clique com o botão direito para abrir o menu.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    boolean is(ItemStack it) {
        return it != null && it.getType() == Material.COMPASS && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    boolean has(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (is(it)) return true;
        }
        return false;
    }

    /** Gives the compass if the player has none. Returns false when there was no room. */
    boolean giveIfMissing(Player p) {
        if (has(p)) return true;
        if (p.getInventory().getItem(HOTBAR_SLOT) == null) {
            p.getInventory().setItem(HOTBAR_SLOT, create());
            return true;
        }
        return p.getInventory().addItem(create()).isEmpty();
    }
}

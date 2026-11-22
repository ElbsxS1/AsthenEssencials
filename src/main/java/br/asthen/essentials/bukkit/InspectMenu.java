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
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

/**
 * Views of another player's inventory and ender chest for admins. What the admin sees is a picture; clicking an
 * item TAKES it: the click is checked against what the player really has at that moment, the stack is moved to
 * the admin's inventory, and the picture is refreshed. Nothing can be put in or moved around, so no item can
 * be duplicated by a stale picture.
 */
final class InspectMenu {
    private InspectMenu() {
    }

    static final class Holder implements InventoryHolder {
        final UUID target;
        final boolean ender;
        final int backSlot;
        final Runnable back;
        private Inventory inventory;

        Holder(UUID target, boolean ender, int backSlot, Runnable back) {
            this.target = target;
            this.ender = ender;
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

    // ------------------------------------------------------------------ opening
    /** Rows 1-3: inventory, row 4: hotbar, row 5: armor + off-hand, row 6: back button. */
    static void openInventory(Player admin, Player target, Runnable back) {
        Holder holder = new Holder(target.getUniqueId(), false, 49, back);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Inventário de " + target.getName() + " (clique para pegar)"));
        holder.bind(inv);
        fill(holder, target);
        admin.openInventory(inv);
    }

    static void openEnder(Player admin, Player target, Runnable back) {
        Holder holder = new Holder(target.getUniqueId(), true, 31, back);
        Inventory inv = Bukkit.createInventory(holder, 36, Component.text("Baú do Ender de " + target.getName() + " (clique para pegar)"));
        holder.bind(inv);
        fill(holder, target);
        admin.openInventory(inv);
    }

    // ------------------------------------------------------------------ drawing
    private static void fill(Holder h, Player target) {
        Inventory inv = h.inventory;
        inv.clear();
        if (h.ender) {
            ItemStack[] contents = target.getEnderChest().getContents();
            for (int i = 0; i < 27 && i < contents.length; i++) inv.setItem(i, copy(contents[i]));
            for (int s = 27; s < 36; s++) inv.setItem(s, filler());
            inv.setItem(h.backSlot, backButton());
            return;
        }
        PlayerInventory src = target.getInventory();
        for (int s = 0; s < 36; s++) inv.setItem(s, copy(src.getItem(targetIndex(h, s))));
        String[] armorNames = {"Capacete", "Peitoral", "Calças", "Botas"};
        for (int i = 0; i < 4; i++) {
            ItemStack piece = src.getItem(targetIndex(h, 36 + i));
            inv.setItem(36 + i, piece == null || piece.getType().isAir()
                    ? labelled(Material.LIGHT_GRAY_STAINED_GLASS_PANE, armorNames[i] + " (vazio)", NamedTextColor.GRAY) : copy(piece));
        }
        ItemStack off = src.getItem(targetIndex(h, 40));
        inv.setItem(40, off == null || off.getType().isAir()
                ? labelled(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Mão secundária (vazio)", NamedTextColor.GRAY) : copy(off));
        for (int s = 41; s < 54; s++) inv.setItem(s, filler());
        inv.setItem(h.backSlot, backButton());
    }

    /**
     * Where a slot of the picture lives in the real inventory, or -1 if it is not an item slot.
     * Player inventory indexes: 0-8 hotbar, 9-35 main, 36 boots, 37 leggings, 38 chestplate, 39 helmet, 40 off-hand.
     */
    private static int targetIndex(Holder h, int viewSlot) {
        if (h.ender) return viewSlot >= 0 && viewSlot < 27 ? viewSlot : -1;
        if (viewSlot >= 0 && viewSlot < 27) return 9 + viewSlot;
        if (viewSlot >= 27 && viewSlot < 36) return viewSlot - 27;
        return switch (viewSlot) {
            case 36 -> 39; // helmet
            case 37 -> 38; // chestplate
            case 38 -> 37; // leggings
            case 39 -> 36; // boots
            case 40 -> 40; // off-hand
            default -> -1;
        };
    }

    private static ItemStack read(Holder h, Player t, int idx) {
        return h.ender ? t.getEnderChest().getItem(idx) : t.getInventory().getItem(idx);
    }

    private static void write(Holder h, Player t, int idx, ItemStack it) {
        if (h.ender) t.getEnderChest().setItem(idx, it);
        else t.getInventory().setItem(idx, it);
    }

    // ------------------------------------------------------------------ taking
    /** The admin clicked a slot of the picture: move that whole stack from the player to the admin. Main thread only. */
    static void take(Plugin plugin, Player admin, Holder h, int viewSlot) {
        Player t = Bukkit.getPlayer(h.target);
        if (t == null) {
            admin.closeInventory();
            Msg.err(admin, "Esse jogador saiu do servidor.");
            return;
        }
        int idx = targetIndex(h, viewSlot);
        if (idx < 0) return;
        ItemStack actual = read(h, t, idx);
        if (actual == null || actual.getType().isAir()) return;

        // The picture may be out of date (the player moved things meanwhile): never take what is not shown.
        if (!actual.equals(h.inventory.getItem(viewSlot))) {
            fill(h, t);
            Msg.info(admin, "O conteúdo mudou, atualizei. Clique de novo para pegar.");
            return;
        }

        ItemStack taking = actual.clone();
        write(h, t, idx, null);
        Map<Integer, ItemStack> left = admin.getInventory().addItem(taking);
        if (!left.isEmpty()) { // not everything fits: the rest goes back where it was
            write(h, t, idx, left.values().iterator().next());
            Msg.err(admin, "Seu inventário está cheio.");
        }
        int moved = taking.getAmount() - (left.isEmpty() ? 0 : left.values().iterator().next().getAmount());
        if (moved > 0) {
            plugin.getLogger().info(admin.getName() + " pegou " + moved + "x " + taking.getType().name() + " do "
                    + (h.ender ? "baú do ender" : "inventário") + " de " + t.getName());
        }
        fill(h, t);
    }
}

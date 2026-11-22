package br.asthen.essentials.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.function.Consumer;

/** Native form menu for Bedrock players. Only loaded when Floodgate is installed. */
final class BedrockMenu {
    /** Formatting codes: colour first, then bold (a colour code after §l would cancel the bold). */
    private static final String TITLE = "§6§l";
    private static final String TEXT = "§8";

    /** Button colour (dark tones only: they stay readable on the grey button) and an optional built-in icon. */
    private record Style(String color, String icon) {
    }

    private static final Style PLAIN = new Style("§0", null);

    private BedrockMenu() {
    }

    private static Style style(Material m) {
        return switch (m) {
            case RED_BED -> new Style("§1", "textures/items/bed_red");
            case ENDER_PEARL -> new Style("§5", "textures/items/ender_pearl");
            case PLAYER_HEAD -> new Style("§3", "textures/ui/FriendsIcon");
            case LIME_DYE -> new Style("§2", "textures/ui/color_plus");
            case BARRIER -> new Style("§4", "textures/ui/trash_default");
            case ARROW -> new Style("§8", "textures/ui/arrow_left");
            case CLOCK -> new Style("§1", "textures/items/clock_item");
            case PAPER -> new Style("§5", "textures/items/paper");
            case LEATHER_BOOTS -> new Style("§6", "textures/items/leather_boots");
            case TNT -> new Style("§4", "textures/blocks/tnt_side");
            case BEDROCK -> new Style("§4", "textures/blocks/bedrock");
            case CHEST, ENDER_CHEST -> new Style("§5", null);
            case BELL -> new Style("§6", null);
            default -> PLAIN;
        };
    }

    static boolean isBedrock(Player p) {
        return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
    }

    static void open(Plugin plugin, Player p, Menu menu) {
        SimpleForm.Builder form = SimpleForm.builder().title(TITLE + menu.title()).content(TEXT + menu.content());
        for (Menu.Entry e : menu.entries()) {
            Style st = style(e.icon());
            String text = st.color() + "§l" + e.label();
            if (st.icon() == null) form.button(text);
            else form.button(text, FormImage.Type.PATH, st.icon());
        }
        form.validResultHandler(response -> {
            int id = response.clickedButtonId();
            if (id < 0 || id >= menu.entries().size()) return;
            Runnable action = menu.entries().get(id).action();
            // form callbacks arrive off the main thread
            Bukkit.getScheduler().runTask(plugin, action);
        });
        FloodgateApi.getInstance().sendForm(p.getUniqueId(), form.build());
    }

    /** A form with one text field. Closing the form calls onCancel; an empty field gives an empty answer. */
    static void prompt(Plugin plugin, Player p, String title, String label, String placeholder,
                       Consumer<String> onAnswer, Runnable onCancel) {
        CustomForm.Builder form = CustomForm.builder().title(TITLE + title).input(TEXT + label, placeholder);
        form.validResultHandler(response -> {
            String text = response.asInput(0);
            String answer = text == null ? "" : text.trim();
            Bukkit.getScheduler().runTask(plugin, () -> onAnswer.accept(answer));
        });
        form.closedOrInvalidResultHandler(() -> Bukkit.getScheduler().runTask(plugin, onCancel));
        FloodgateApi.getInstance().sendForm(p.getUniqueId(), form.build());
    }
}

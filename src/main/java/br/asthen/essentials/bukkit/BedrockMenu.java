package br.asthen.essentials.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.function.Consumer;

/** Native form menu for Bedrock players. Only loaded when Floodgate is installed. */
final class BedrockMenu {
    private BedrockMenu() {
    }

    static boolean isBedrock(Player p) {
        return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
    }

    static void open(Plugin plugin, Player p, Menu menu) {
        SimpleForm.Builder form = SimpleForm.builder().title(menu.title()).content(menu.content());
        for (Menu.Entry e : menu.entries()) form.button(e.label());
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
        CustomForm.Builder form = CustomForm.builder().title(title).input(label, placeholder);
        form.validResultHandler(response -> {
            String text = response.asInput(0);
            String answer = text == null ? "" : text.trim();
            Bukkit.getScheduler().runTask(plugin, () -> onAnswer.accept(answer));
        });
        form.closedOrInvalidResultHandler(() -> Bukkit.getScheduler().runTask(plugin, onCancel));
        FloodgateApi.getInstance().sendForm(p.getUniqueId(), form.build());
    }
}

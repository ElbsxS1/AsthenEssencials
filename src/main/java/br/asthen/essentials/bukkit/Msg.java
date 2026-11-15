package br.asthen.essentials.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

final class Msg {
    private static final Component PREFIX = Component.text("[Asthen] ", NamedTextColor.GOLD);

    private Msg() {
    }

    static void ok(CommandSender s, String text) {
        s.sendMessage(PREFIX.append(Component.text(text, NamedTextColor.GREEN)));
    }

    static void err(CommandSender s, String text) {
        s.sendMessage(PREFIX.append(Component.text(text, NamedTextColor.RED)));
    }

    static void info(CommandSender s, String text) {
        s.sendMessage(PREFIX.append(Component.text(text, NamedTextColor.GRAY)));
    }

    static Component plain(String text) {
        return Component.text(text);
    }
}

package br.asthen.essentials.bukkit;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a Java player for a line of text in the chat (the next message is captured and never broadcast).
 * Bedrock players get a native form instead (see BedrockMenu.prompt).
 */
final class Prompts implements Listener {
    private static final long TIMEOUT_TICKS = 20L * 60;

    private record Pending(Consumer<String> onAnswer, Runnable onCancel, boolean optional) {
    }

    private final AsthenEssentials plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    Prompts(AsthenEssentials plugin) {
        this.plugin = plugin;
    }

    /** optional: the player may answer "pular" to give an empty answer. "cancelar" always calls onCancel. */
    void ask(Player p, String question, boolean optional, Consumer<String> onAnswer, Runnable onCancel) {
        Pending mine = new Pending(onAnswer, onCancel, optional);
        pending.put(p.getUniqueId(), mine);
        Msg.info(p, question);
        Msg.info(p, optional ? "Digite 'pular' para deixar em branco ou 'cancelar' para voltar."
                : "Digite 'cancelar' para voltar.");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(p.getUniqueId(), mine) && p.isOnline()) Msg.err(p, "Tempo esgotado, pedido cancelado.");
        }, TIMEOUT_TICKS);
    }

    void forget(UUID id) {
        pending.remove(id);
    }

    /** Runs before the mute check and before chat plugins: a captured answer must not reach anyone. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        Pending pd = pending.remove(p.getUniqueId());
        if (pd == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (text.equalsIgnoreCase("cancelar")) pd.onCancel().run();
            else if (pd.optional() && text.equalsIgnoreCase("pular")) pd.onAnswer().accept("");
            else pd.onAnswer().accept(text);
        });
    }
}

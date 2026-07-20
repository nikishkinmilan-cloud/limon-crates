package com.limonanarchy.anticheat.review;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Пока игрок под проверкой (/acprov), ЛЮБОЕ его сообщение в чат ОБЯЗАТЕЛЬНО дублируется
 * проверяющему личным сообщением - даже если проверяющий отвлёкся и не следит за общим
 * чатом. По умолчанию само сообщение ещё и прячется от остальных игроков сервера
 * (review.hide-chat-from-others) - в изолированной камере проверки постороннему всё
 * равно нечего обсуждать с подозреваемым.
 */
public class ReviewChatListener implements Listener {

    private final AntiCheatPlugin plugin;
    private final ReviewManager reviewManager;

    public ReviewChatListener(AntiCheatPlugin plugin, ReviewManager reviewManager) {
        this.plugin = plugin;
        this.reviewManager = reviewManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!reviewManager.isUnderReview(player.getUniqueId())) return;

        Player reviewer = reviewManager.getReviewer(player.getUniqueId());
        String plainMessage = LegacyComponentSerializer.legacySection().serialize(event.message());

        if (reviewer != null && reviewer.isOnline()) {
            reviewer.sendMessage("§d[Проверка] §f" + player.getName() + " §7» §f" + plainMessage);
        } else {
            // Проверяющий отключился - не теряем сообщение, хотя бы в консоль/лог сервера
            plugin.getLogger().info("[Проверка/проверяющий офлайн] " + player.getName() + ": " + plainMessage);
        }

        if (plugin.getConfig().getBoolean("review.hide-chat-from-others", true)) {
            final Player finalReviewer = reviewer;
            event.viewers().removeIf(viewer -> !viewer.equals(player) && !viewer.equals(finalReviewer));
        }
    }
}

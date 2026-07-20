package com.limonanarchy.anticheat.review;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Пока игрок под проверкой (/acprov) - держим его на месте (камерой вертеть можно,
 * ходить нельзя), чтобы он не убежал из камеры, пока стафф проверяет его AnyDesk.
 * Отключается флагом review.freeze-target: false в конфиге, если это не нужно.
 *
 * Также чистит состояние ReviewManager, если подозреваемый или проверяющий вышли
 * с сервера прямо посреди проверки, чтобы никто не завис в подвешенном состоянии.
 */
public class ReviewListener implements Listener {

    private final AntiCheatPlugin plugin;
    private final ReviewManager reviewManager;

    public ReviewListener(AntiCheatPlugin plugin, ReviewManager reviewManager) {
        this.plugin = plugin;
        this.reviewManager = reviewManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("review.freeze-target", true)) return;

        Player player = event.getPlayer();
        if (!reviewManager.isUnderReview(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Разрешаем вращать камеру (yaw/pitch), но не даём сдвинуться с места
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(new Location(to.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        reviewManager.clearSuspect(player.getUniqueId());
        reviewManager.handleReviewerQuit(player.getUniqueId());
    }
}

package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timer hack: клиент ускоряет собственный игровой тик (timer.mixin и подобные),
 * из-за чего сервер получает движения/действия ЧАЩЕ, чем 20 раз в секунду -
 * это даёт ускоренную добычу, атаки, регенерацию голода и т.д.
 *
 * Считаем количество PlayerMoveEvent за секунду через bukkit runnable раз в 20 тиков.
 * Ванильно это ~20/сек (иногда чуть меньше при стоянии на месте - тогда просто не
 * набирается достаточно событий и мы не флагаем, ложных срабатываний в меньшую
 * сторону не бывает по построению проверки).
 */
public class TimerCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<UUID, Integer> movesThisWindow = new ConcurrentHashMap<>();

    public TimerCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("timer.enabled", true)) return;
        movesThisWindow.merge(event.getPlayer().getUniqueId(), 1, Integer::sum);
    }

    public void startCheckTask() {
        long intervalTicks = plugin.getConfig().getLong("timer.check-interval-ticks", 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("timer.enabled", true)) return;

                int maxMovesPerWindow = plugin.getConfig().getInt("timer.max-moves-per-window", 24);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID id = player.getUniqueId();
                    int count = movesThisWindow.getOrDefault(id, 0);

                    if (player.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(player)) {
                        movesThisWindow.put(id, 0);
                        continue;
                    }

                    if (count > maxMovesPerWindow) {
                        violationManager.flag(player, "TimerCheck",
                                plugin.getConfig().getInt("timer.violation-weight", 3));
                    }
                }
                movesThisWindow.clear();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }
}

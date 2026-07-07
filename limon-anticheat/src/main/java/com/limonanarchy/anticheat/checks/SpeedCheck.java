package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Ловит превышение скорости горизонтального движения (speed-хаки).
 *
 * ВАЖНО: не флагает по одному "быстрому" тику - лаги/скачки пинга (особенно при
 * нескольких аккаунтах на одном интернете) регулярно дают одиночные всплески.
 * Вместо этого считаем СРЕДНЮЮ скорость за несколько последних тиков (окно),
 * и флагаем только если превышение стабильно держится, а не разово скакнуло.
 */
public class SpeedCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    // Скользящее окно последних дистанций по игроку - сглаживает единичные лаг-спайки
    private final Map<String, double[]> recentDistances = new ConcurrentHashMap<>();
    private static final int WINDOW_SIZE = 10;
    private final Map<String, Integer> windowIndex = new ConcurrentHashMap<>();
    private final Map<String, Integer> excessStreak = new ConcurrentHashMap<>();

    public SpeedCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("speed.enabled", true)) return;

        Player player = event.getPlayer();
        String key = player.getUniqueId().toString();

        if (player.getAllowFlight() || player.isFlying() || player.isGliding()
                || player.isInsideVehicle() || player.hasPermission("anticheat.bypass")
                || ExemptionTracker.isExempt(player)) {
            recentDistances.remove(key);
            return;
        }

        var from = event.getFrom();
        var to = event.getTo();
        if (to == null || !from.getWorld().equals(to.getWorld())) {
            recentDistances.remove(key);
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double maxAllowed = player.isSprinting()
                ? plugin.getConfig().getDouble("speed.max-sprint-speed", 0.42)
                : plugin.getConfig().getDouble("speed.max-walk-speed", 0.32);

        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            maxAllowed *= (1 + 0.2 * amplifier);
        }

        // Хороший запас на погрешность сети - топовые античиты тоже не режут впритык
        maxAllowed *= 1.25;

        // Считаем среднюю скорость за последние WINDOW_SIZE тиков
        double[] window = recentDistances.computeIfAbsent(key, k -> new double[WINDOW_SIZE]);
        int idx = windowIndex.merge(key, 1, (a, b) -> (a + 1) % WINDOW_SIZE) - 1;
        if (idx < 0) idx = WINDOW_SIZE - 1;
        window[idx] = horizontalDistance;

        double average = 0;
        for (double d : window) average += d;
        average /= WINDOW_SIZE;

        if (average > maxAllowed) {
            int streak = excessStreak.merge(key, 1, Integer::sum);
            // Флагаем только если превышение держится стабильно несколько раз подряд,
            // а не одно случайное колебание
            if (streak >= 5) {
                int weight = plugin.getConfig().getInt("speed.violation-weight", 1);
                violationManager.flag(player, "SpeedCheck", weight);
                excessStreak.put(key, 0);
            }
        } else {
            excessStreak.put(key, 0);
        }
    }
}

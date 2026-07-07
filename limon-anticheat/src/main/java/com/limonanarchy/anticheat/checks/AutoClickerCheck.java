package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Ловит автокликеры: человек физически не может кликать идеально ровными интервалами
 * (например каждые 100мс ± 2мс) на протяжении десятков ударов подряд, и не может
 * долго держать очень высокий CPS (15+) стабильно. Реальные игроки имеют естественный
 * разброс в ритме клика (усталость, микро-паузы).
 */
public class AutoClickerCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<UUID, Deque<Long>> clickTimestamps = new ConcurrentHashMap<>();
    private static final int SAMPLE_SIZE = 20;

    public AutoClickerCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("autoclicker.enabled", true)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (player.hasPermission("anticheat.bypass")) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = clickTimestamps.computeIfAbsent(id, k -> new ArrayDeque<>());
        timestamps.addLast(now);
        while (timestamps.size() > SAMPLE_SIZE) {
            timestamps.removeFirst();
        }

        if (timestamps.size() < SAMPLE_SIZE) return; // мало данных для анализа

        // Считаем интервалы между кликами
        Long[] times = timestamps.toArray(new Long[0]);
        double[] intervals = new double[times.length - 1];
        for (int i = 1; i < times.length; i++) {
            intervals[i - 1] = times[i] - times[i - 1];
        }

        double meanInterval = 0;
        for (double interval : intervals) meanInterval += interval;
        meanInterval /= intervals.length;

        double cps = meanInterval > 0 ? 1000.0 / meanInterval : 0;

        double variance = 0;
        for (double interval : intervals) variance += Math.pow(interval - meanInterval, 2);
        variance /= intervals.length;
        double stdDev = Math.sqrt(variance);

        double maxLegitCps = plugin.getConfig().getDouble("autoclicker.max-legit-cps", 14);
        double minStdDevForCps = plugin.getConfig().getDouble("autoclicker.min-stddev-ms", 15);

        // Флагаем если: (а) CPS выше человеческого предела, ИЛИ
        // (б) ритм подозрительно ровный (низкий разброс = робот, а не рука человека)
        if (cps > maxLegitCps) {
            violationManager.flag(player, "AutoClicker-HighCPS", plugin.getConfig().getInt("autoclicker.violation-weight", 2));
        } else if (cps > 8 && stdDev < minStdDevForCps) {
            // Высокий CPS + подозрительно стабильный ритм - явный признак кликера/макроса
            violationManager.flag(player, "AutoClicker-StableRhythm", plugin.getConfig().getInt("autoclicker.violation-weight", 2));
        }
    }
}

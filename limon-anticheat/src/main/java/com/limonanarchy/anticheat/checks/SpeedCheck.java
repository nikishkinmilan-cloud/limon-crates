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
 * Автоматически учитывает: эффект Speed (зелье), спринт, элитру, коня/лодку.
 */
public class SpeedCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<String, org.bukkit.Location> lastLocation = new ConcurrentHashMap<>();

    public SpeedCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("speed.enabled", true)) return;

        Player player = event.getPlayer();
        String key = player.getUniqueId().toString();

        // Исключения: полёт, элитра, транспорт (лошадь/лодка/повозка) - там своя скорость
        if (player.getAllowFlight() || player.isFlying() || player.isGliding()
                || player.isInsideVehicle() || player.hasPermission("anticheat.bypass")) {
            lastLocation.remove(key);
            return;
        }

        var from = event.getFrom();
        var to = event.getTo();
        if (to == null || !from.getWorld().equals(to.getWorld())) {
            lastLocation.remove(key);
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double maxAllowed = player.isSprinting()
                ? plugin.getConfig().getDouble("speed.max-sprint-speed", 0.42)
                : plugin.getConfig().getDouble("speed.max-walk-speed", 0.32);

        // Учитываем эффект Speed - каждый уровень добавляет ~20% скорости
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            maxAllowed *= (1 + 0.2 * amplifier);
        }

        // Небольшой запас на погрешность сети/тиков (лаг может дать ложный скачок один раз)
        maxAllowed *= 1.15;

        if (horizontalDistance > maxAllowed) {
            int weight = plugin.getConfig().getInt("speed.violation-weight", 1);
            violationManager.flag(player, "SpeedCheck", weight);
        }
    }
}

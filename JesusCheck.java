package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jesus hack: игрок "ходит по воде", не тонет и не плывёт. Ванильно это возможно
 * только на Frost Walker (лёд вместо воды под ногами) или в лодке - оба случая
 * автоматически исключаются, т.к. блок под игроком в этот момент уже не WATER.
 * Копим счётчик подряд идущих тиков "стоим на воде" и флагаем только после
 * нескольких тиков подряд, чтобы не ловить обычный прыжок в воду/из воды.
 */
public class JesusCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<UUID, Integer> ticksOnWater = new ConcurrentHashMap<>();

    public JesusCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("jesus.enabled", true)) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (player.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(player)) {
            ticksOnWater.remove(id);
            return;
        }
        if (player.isFlying() || player.isGliding() || player.isSwimming()
                || player.isInsideVehicle() || player.isRiptiding() || player.isDead()) {
            ticksOnWater.remove(id);
            return;
        }

        Location to = event.getTo();
        if (to == null) return;

        double horizontalMove = distanceHorizontal(event.getFrom(), to);
        double minMoveToCount = plugin.getConfig().getDouble("jesus.min-horizontal-move", 0.02);

        Block feetBlock = to.clone().subtract(0, 0.2, 0).getBlock();
        Block belowFeetBlock = to.clone().subtract(0, 1.0, 0).getBlock();

        boolean standingOnWaterSurface = feetBlock.getType() == Material.WATER
                && belowFeetBlock.getType() == Material.WATER
                && !player.isInWater();

        if (standingOnWaterSurface && horizontalMove >= minMoveToCount) {
            int ticks = ticksOnWater.merge(id, 1, Integer::sum);
            int requiredTicks = plugin.getConfig().getInt("jesus.min-consecutive-ticks", 12);

            if (ticks >= requiredTicks) {
                violationManager.flag(player, "JesusCheck", plugin.getConfig().getInt("jesus.violation-weight", 2));
                ticksOnWater.put(id, requiredTicks / 2);
            }
        } else {
            ticksOnWater.remove(id);
        }
    }

    private double distanceHorizontal(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}

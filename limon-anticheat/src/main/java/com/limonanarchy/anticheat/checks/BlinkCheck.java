package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ClientVersionUtil;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Blink (packet delay hack): чит задерживает исходящие пакеты движения, чтобы игрок
 * "застревал" для других на месте (удобно уходить от урона/комбата), а затем разом
 * отправляет накопленные пакеты - на сервере это выглядит как один резкий скачок
 * позиции на большое расстояние за один PlayerMoveEvent, гораздо больше того,
 * что физически можно пройти за один тик даже с элитрой.
 *
 * В отличие от SpeedCheck (который смотрит на СРЕДНЮЮ скорость за период),
 * BlinkCheck ловит именно одиночный аномальный скачок.
 */
public class BlinkCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    public BlinkCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("blink.enabled", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(player)) return;
        if (player.isInsideVehicle()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !from.getWorld().equals(to.getWorld())) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double maxHorizontalDistance = plugin.getConfig().getDouble("blink.max-horizontal-distance", 8.0);

        // элитра/риптайд/старые клиенты двигаются рывками сильнее - даём им запас
        if (player.isGliding() || player.isRiptiding()) {
            maxHorizontalDistance *= 2.5;
        }
        if (ClientVersionUtil.isLegacyClient(player,
                plugin.getConfig().getInt("version-compat.legacy-protocol-threshold", 767))) {
            maxHorizontalDistance *= plugin.getConfig().getDouble("version-compat.leniency-multiplier", 1.3);
        }

        if (horizontalDistance > maxHorizontalDistance) {
            violationManager.flag(player, "BlinkCheck", plugin.getConfig().getInt("blink.violation-weight", 4));
            // после срабатывания считаем следующую позицию "нормальной точкой отсчёта",
            // чтобы не флагать повторно на восстановлении после лагов
            ExemptionTracker.exempt(player);
        }
    }
}

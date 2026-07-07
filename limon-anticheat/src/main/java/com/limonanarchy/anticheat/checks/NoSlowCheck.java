package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * NoSlow: по правилам ванильного клиента, игрок ДОЛЖЕН двигаться медленнее,
 * пока держит щит (блокирует), ест еду или натягивает лук. Читы часто убирают
 * это замедление, чтобы можно было есть/блокировать на полной скорости - это
 * называется "NoSlow" и является одним из самых частых читов в PvP.
 */
public class NoSlowCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    public NoSlowCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("noslow.enabled", true)) return;

        Player player = event.getPlayer();

        if (player.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(player)) return;
        if (!player.isHandRaised()) return; // не блокирует/не ест/не целится луком - проверка не актуальна

        var from = event.getFrom();
        var to = event.getTo();
        if (to == null) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Ванильное замедление при использовании предмета - примерно 0.2 блока/тик максимум
        double maxAllowedWhileUsingItem = plugin.getConfig().getDouble("noslow.max-speed-while-using-item", 0.22);

        if (plugin.getConfig().getBoolean("version-compat.enabled", true)) {
            int legacyThreshold = plugin.getConfig().getInt("version-compat.legacy-protocol-threshold", 767);
            if (com.limonanarchy.anticheat.ClientVersionUtil.isLegacyClient(player, legacyThreshold)) {
                maxAllowedWhileUsingItem *= plugin.getConfig().getDouble("version-compat.leniency-multiplier", 1.3);
            }
        }

        if (horizontalDistance > maxAllowedWhileUsingItem) {
            int weight = plugin.getConfig().getInt("noslow.violation-weight", 1);
            violationManager.flag(player, "NoSlowCheck", weight);
        }
    }
}

package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * NoFall: чит, отключающий получение урона от падения с большой высоты
 * (часто идёт в комплекте с fly/jesus hack). Отслеживаем реальную высоту падения
 * через fallDistance, и если игрок приземлился с высоты, требующей урона,
 * но урон не пришёл - это подозрительно.
 */
public class NoFallCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<UUID, Boolean> tookFallDamage = new ConcurrentHashMap<>();

    public NoFallCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player) {
            tookFallDamage.put(player.getUniqueId(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("nofall.enabled", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(player)) return;
        if (player.getAllowFlight() || player.isFlying() || player.isGliding()
                || player.isSwimming() || player.isRiptiding()) return;

        UUID id = player.getUniqueId();

        if (player.isOnGround()) {
            float fallDistance = player.getFallDistance();
            double minFallForDamage = plugin.getConfig().getDouble("nofall.min-fall-distance", 4.0);

            if (fallDistance >= minFallForDamage && !tookFallDamage.getOrDefault(id, false)) {
                // Приземлился с высоты, требующей урона (в ванильном Minecraft это ~3 блока),
                // но damage event ни разу не сработал - подозрительно
                violationManager.flag(player, "NoFallCheck", plugin.getConfig().getInt("nofall.violation-weight", 2));
            }
            tookFallDamage.put(id, false);
        }
    }
}

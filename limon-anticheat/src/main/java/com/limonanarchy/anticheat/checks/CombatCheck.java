package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Ловит killaura / clickaura: атаки без прямой видимости (сквозь стены),
 * на нереальной дистанции, без поворота к цели, или слишком быстрое
 * переключение между разными целями (мульти-аура).
 *
 * Легитимный игрок физически не может: пробить блок кулаком, ударить с 6 блоков,
 * бить не глядя на цель, или атаковать 3 разных мобов за 100мс.
 */
public class CombatCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    // Последняя атакованная цель по игроку: <playerUUID, <targetUUID, timestamp>>
    private final Map<UUID, TargetRecord> lastTarget = new ConcurrentHashMap<>();

    public CombatCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("combat.enabled", true)) return;

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (player.hasPermission("anticheat.bypass")) return;
        if (ExemptionTracker.isExempt(player)) return;

        int weight = plugin.getConfig().getInt("combat.violation-weight", 2);

        double maxReach = plugin.getConfig().getDouble("combat.max-reach", 4.2);
        double maxAngle = plugin.getConfig().getDouble("combat.max-angle-degrees", 60);

        if (plugin.getConfig().getBoolean("version-compat.enabled", true)) {
            int legacyThreshold = plugin.getConfig().getInt("version-compat.legacy-protocol-threshold", 767);
            if (com.limonanarchy.anticheat.ClientVersionUtil.isLegacyClient(player, legacyThreshold)) {
                double leniency = plugin.getConfig().getDouble("version-compat.leniency-multiplier", 1.3);
                maxReach *= leniency;
                maxAngle *= leniency;
            }
        }

        // --- 1. Проверка дистанции (reach) ---
        double distance = player.getEyeLocation().distance(target.getEyeLocation());
        if (distance > maxReach) {
            violationManager.flag(player, "CombatCheck-Reach", weight);
        }

        // --- 2. Проверка угла обзора (бьёт ли игрок туда, куда смотрит) ---
        Vector toTarget = target.getEyeLocation().toVector()
                .subtract(player.getEyeLocation().toVector()).normalize();
        Vector lookDirection = player.getEyeLocation().getDirection().normalize();

        double dot = Math.max(-1.0, Math.min(1.0, lookDirection.dot(toTarget)));
        double angleDegrees = Math.toDegrees(Math.acos(dot));

        if (angleDegrees > maxAngle) {
            violationManager.flag(player, "CombatCheck-Angle", weight);
        }

        // --- 3. Проверка прямой видимости (атака сквозь стену) ---
        if (plugin.getConfig().getBoolean("combat.check-line-of-sight", true)) {
            RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(),
                    toTarget,
                    distance,
                    org.bukkit.FluidCollisionMode.NEVER,
                    true
            );
            // Если луч упёрся в блок РАНЬШЕ, чем долетел до цели - значит между игроком
            // и целью есть твёрдая стена, а удар всё равно прошёл. Это невозможно вживую.
            if (rayTrace != null && rayTrace.getHitBlock() != null) {
                double blockDistance = player.getEyeLocation().distance(rayTrace.getHitPosition().toLocation(player.getWorld()));
                if (blockDistance < distance - 0.5) {
                    violationManager.flag(player, "CombatCheck-NoLineOfSight", weight * 2);
                }
            }
        }

        // --- 4. Проверка мульти-ауры (слишком быстрое переключение между РАЗНЫМИ целями) ---
        long window = plugin.getConfig().getLong("combat.multi-target-window-ms", 150);
        long now = System.currentTimeMillis();
        TargetRecord previous = lastTarget.get(player.getUniqueId());

        if (previous != null
                && !previous.targetId.equals(target.getUniqueId())
                && (now - previous.timestamp) < window) {
            violationManager.flag(player, "CombatCheck-MultiAura", weight);
        }

        lastTarget.put(player.getUniqueId(), new TargetRecord(target.getUniqueId(), now));
    }

    private record TargetRecord(UUID targetId, long timestamp) {
    }
}

package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Fake Criticals: читы рисуют клиенту частицы/звук крита на КАЖДОМ ударе, а часть
 * из них ещё и завышают реальный урон, чтобы он "совпадал" с критом, даже когда
 * ванильные условия крита не выполнены (игрок стоит на земле, в воде, на лестнице,
 * слепой и т.д.).
 *
 * В современном Bukkit API нет DamageModifier.CRITICAL (крит теперь считается
 * на клиенте/сервере иначе, отдельного модификатора нет), поэтому сравниваем
 * НАПРЯМУЮ: базовый урон события (DamageModifier.BASE) против "чистого" значения
 * атрибута GENERIC_ATTACK_DAMAGE атакующего. Ванильный крит даёт примерно x1.5
 * к базовому урону - если фактический BASE заметно выше атрибута, а условия для
 * крита не выполнены, это подозрительно.
 */
public class CriticalsCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    public CriticalsCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("criticals.enabled", true)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (attacker.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(attacker)) return;

        // GENERIC_ATTACK_DAMAGE - актуальное имя константы для paper-api 1.21.1 (pom.xml проекта).
        // Если в будущем поднимете paper-api до 1.21.3+, переименуйте на Attribute.ATTACK_DAMAGE.
        var attackDamageAttribute = attacker.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamageAttribute == null) return;

        double baseAttributeDamage = attackDamageAttribute.getValue();
        double actualBaseDamage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);

        if (baseAttributeDamage <= 0) return;

        double critFactorThreshold = plugin.getConfig().getDouble("criticals.crit-factor-threshold", 1.35);
        boolean looksLikeCrit = actualBaseDamage >= baseAttributeDamage * critFactorThreshold;

        if (!looksLikeCrit) return;

        if (isLegitCritPossible(attacker)) return;

        violationManager.flag(attacker, "CriticalsCheck", plugin.getConfig().getInt("criticals.violation-weight", 3));
    }

    /**
     * Ванильные условия, при которых крит вообще возможен. Если хотя бы одно из них
     * выполнено - удар МОГ быть настоящим критом, и мы не флагаем (лучше пропустить
     * пограничный случай, чем дать ложное срабатывание).
     */
    private boolean isLegitCritPossible(Player attacker) {
        if (attacker.isOnGround()) return false;
        if (attacker.isInWater() || attacker.isInLava()) return false;
        if (attacker.isSprinting()) return false;
        if (attacker.isGliding() || attacker.isRiptiding() || attacker.isInsideVehicle()) return false;
        if (attacker.hasPotionEffect(PotionEffectType.BLINDNESS)) return false;
        if (attacker.getFallDistance() <= 0f) return false;
        if (attacker.getVelocity().getY() >= 0) return false;

        return true;
    }
}

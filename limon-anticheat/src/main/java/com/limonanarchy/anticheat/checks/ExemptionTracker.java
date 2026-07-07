package com.limonanarchy.anticheat.checks;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Общая "память" для всех проверок: если игрок только что телепортировался,
 * respawn'нулся, получил урон (нокбек) или использовал элитру/жемчуг - движение
 * в следующую секунду совершенно нормально может выглядеть "подозрительно"
 * для fly/speed проверок. Помечаем такие моменты и просто пропускаем проверку.
 */
public class ExemptionTracker {

    private static final ConcurrentHashMap<UUID, Long> exemptUntil = new ConcurrentHashMap<>();

    private ExemptionTracker() {
    }

    public static void exempt(Player player, long durationMillis) {
        exemptUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    public static void exempt(Player player) {
        exempt(player, TimeUnit.SECONDS.toMillis(1));
    }

    public static boolean isExempt(Player player) {
        Long until = exemptUntil.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            exemptUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }
}

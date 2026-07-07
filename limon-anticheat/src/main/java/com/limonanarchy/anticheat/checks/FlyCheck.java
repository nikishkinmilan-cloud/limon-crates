package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Ловит игроков, которые "летают" без разрешения (allowFlight не выдан).
 *
 * ВАЖНО: если игрок использовал /fly (или ему выдали полёт через LuckPerms/Essentials),
 * у него автоматически стоит player.getAllowFlight() == true. Мы полностью пропускаем
 * таких игроков - это и есть "исключение для /fly" из твоего запроса.
 *
 * Также пропускаем: креатив/спектейтор режим, элитру (глайдинг), плавание, левитацию,
 * недавнее использование tp/эндер-жемчуга (телепорт даёт мгновенный скачок высоты).
 */
public class FlyCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    // Счётчик тиков нахождения в воздухе без падения, по игроку
    private final Map<String, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<String, Double> lastY = new ConcurrentHashMap<>();

    public FlyCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("fly.enabled", true)) return;

        Player player = event.getPlayer();
        String key = player.getUniqueId().toString();

        // --- Исключения, при которых мы вообще не проверяем ---

        // 1. Игрок легально летает (/fly включен, или выдано через плагин/permission)
        if (player.getAllowFlight() || player.isFlying()) {
            airTicks.remove(key);
            return;
        }

        // 2. Креатив и спектейтор - там полёт разрешён по умолчанию
        switch (player.getGameMode()) {
            case CREATIVE:
            case SPECTATOR:
                airTicks.remove(key);
                return;
            default:
                break;
        }

        // 3. Элитра, плавание, левитация - это не читерский полёт, а игровая механика
        if (player.isGliding() || player.isSwimming() || player.isRiptiding()
                || player.getActivePotionEffects().stream()
                    .anyMatch(e -> e.getType().getName().equalsIgnoreCase("LEVITATION"))) {
            airTicks.remove(key);
            return;
        }

        // 4. Игрок находится в воде/лаве или на лестнице/паутине - там своя физика
        if (isInLiquidOrClimbable(player)) {
            airTicks.remove(key);
            return;
        }

        // 5. Игрок с правом bypass (например известный проверенный стафф)
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }

        // --- Собственно проверка ---

        double currentY = player.getLocation().getY();
        Double previousY = lastY.put(key, currentY);

        boolean onGround = player.isOnGround();

        if (onGround) {
            airTicks.remove(key);
            return;
        }

        // Игрок в воздухе. Если Y не падает (или растёт) несколько тиков подряд -
        // это подозрительно, обычное падение должно постоянно уменьшать Y.
        if (previousY != null && currentY >= previousY - 0.08) {
            int ticks = airTicks.merge(key, 1, Integer::sum);

            int maxAirTime = plugin.getConfig().getInt("fly.max-air-time-ticks", 40);
            if (ticks > maxAirTime) {
                int weight = plugin.getConfig().getInt("fly.violation-weight", 1);
                violationManager.flag(player, "FlyCheck", weight);
                airTicks.put(key, 0); // не спамить флагами каждый тик подряд
            }
        } else {
            // игрок реально падает - сбрасываем счётчик
            airTicks.merge(key, -2, Integer::sum);
            airTicks.computeIfPresent(key, (k, v) -> Math.max(0, v));
        }
    }

    private boolean isInLiquidOrClimbable(Player player) {
        var block = player.getLocation().getBlock();
        var type = block.getType().name();
        return type.contains("WATER") || type.contains("LAVA")
                || type.equals("LADDER") || type.equals("VINE")
                || type.equals("SCAFFOLDING") || type.contains("COBWEB");
    }
}

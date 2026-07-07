package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Scaffold: чит, позволяющий "бежать по воздуху", автоматически ставя блоки под ноги
 * быстрее и точнее, чем способен обычный игрок (обычно смотря почти прямо вниз,
 * идеально ровным ритмом, без пауз на прицеливание).
 * Ловим упрощённо: слишком высокая частота установки блоков подряд + игрок
 * смотрит почти строго вниз в момент постройки.
 */
public class ScaffoldCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<UUID, Long> lastPlaceTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> fastPlaceStreak = new ConcurrentHashMap<>();

    public ScaffoldCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("scaffold.enabled", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("anticheat.bypass")) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        long minIntervalMs = plugin.getConfig().getLong("scaffold.min-place-interval-ms", 110);
        double maxPitchForScaffold = plugin.getConfig().getDouble("scaffold.min-pitch-looking-down", 70);

        Long last = lastPlaceTime.put(id, now);
        float pitch = player.getLocation().getPitch(); // положительный = смотрит вниз

        boolean lookingDown = pitch >= maxPitchForScaffold;
        boolean tooFast = last != null && (now - last) < minIntervalMs;

        if (lookingDown && tooFast) {
            int streak = fastPlaceStreak.merge(id, 1, Integer::sum);
            // Требуем несколько подряд быстрых блоков глядя вниз - не одиночную постройку лестницы
            if (streak >= 4) {
                violationManager.flag(player, "ScaffoldCheck", plugin.getConfig().getInt("scaffold.violation-weight", 1));
                fastPlaceStreak.put(id, 0);
            }
        } else {
            fastPlaceStreak.put(id, 0);
        }
    }
}

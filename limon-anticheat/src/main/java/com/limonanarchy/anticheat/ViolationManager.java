package com.limonanarchy.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ViolationManager {

    private final AntiCheatPlugin plugin;
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    public ViolationManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Регистрирует нарушение для игрока по конкретной проверке.
     * Если бы игрок мог обойти какую-то проверку - весь смысл в накоплении:
     * одно случайное срабатывание (лаг) не банит никого, только систематическое поведение.
     */
    public void flag(Player player, String checkName, int weight) {
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }

        String key = player.getUniqueId().toString();
        int newLevel = violations.merge(key, weight, Integer::sum);

        int alertThreshold = plugin.getConfig().getInt("punishment.alert-threshold", 5);
        int kickThreshold = plugin.getConfig().getInt("punishment.kick-threshold", 15);
        int banThreshold = plugin.getConfig().getInt("punishment.ban-threshold", 0);

        if (plugin.getConfig().getBoolean("log-to-console", true)) {
            plugin.getLogger().info(player.getName() + " -> " + checkName + " (level=" + newLevel + ")");
        }

        if (newLevel >= alertThreshold) {
            broadcastAlert(player, checkName, newLevel);
        }

        if (banThreshold > 0 && newLevel >= banThreshold) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.kickPlayer("§cВы были заблокированы античитом. Обратитесь к администрации.");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "ban " + player.getName() + " Автобан античитом (" + checkName + ")");
            });
            violations.remove(key);
        } else if (newLevel >= kickThreshold) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.kickPlayer("§cПодозрительная активность обнаружена (" + checkName + "). Перезайдите на сервер."));
            violations.put(key, kickThreshold / 2); // не обнуляем полностью, чтобы рецидив копился быстрее
        }
    }

    private void broadcastAlert(Player player, String checkName, int level) {
        String permission = plugin.getConfig().getString("alerts.permission", "anticheat.admin");
        String template = plugin.getConfig().getString("alerts.message",
                "&c[AC] &f%player% &7нарушение: &e%check% &7(уровень: &c%level%&7)");

        String message = template
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace("%level%", String.valueOf(level))
                .replace('&', '§');

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                online.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public int getViolationLevel(String playerName) {
        Player p = Bukkit.getPlayerExact(playerName);
        if (p == null) return 0;
        return violations.getOrDefault(p.getUniqueId().toString(), 0);
    }

    /**
     * Постепенное снижение уровня нарушений со временем,
     * чтобы не банить игроков за старые случайные лаг-спайки.
     */
    public void startDecayTask() {
        long interval = plugin.getConfig().getLong("punishment.decay-interval-seconds", 60) * 20L;
        int decayAmount = plugin.getConfig().getInt("punishment.decay-amount", 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                violations.replaceAll((k, v) -> Math.max(0, v - decayAmount));
                violations.values().removeIf(v -> v <= 0);
            }
        }.runTaskTimer(plugin, interval, interval);
    }
}

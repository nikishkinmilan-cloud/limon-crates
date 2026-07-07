package com.limonanarchy.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ViolationManager {

    private final AntiCheatPlugin plugin;
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();
    // чтобы не спамить "подозрение" каждый тик - шлём один раз, пока уровень не сбросится ниже порога
    private final Map<String, Boolean> suspicionSent = new ConcurrentHashMap<>();

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

        int suspicionThreshold = plugin.getConfig().getInt("punishment.suspicion-threshold", 3);
        int alertThreshold = plugin.getConfig().getInt("punishment.alert-threshold", 5);
        int kickThreshold = plugin.getConfig().getInt("punishment.kick-threshold", 15);
        int banThreshold = plugin.getConfig().getInt("punishment.ban-threshold", 0);

        if (plugin.getConfig().getBoolean("log-to-console", true)) {
            plugin.getLogger().info(player.getName() + " -> " + checkName + " (level=" + newLevel + ")");
        }

        // Раннее уведомление "ведёт себя подозрительно" - один раз, чтобы стафф успел /spec
        if (newLevel >= suspicionThreshold && newLevel < alertThreshold
                && !suspicionSent.getOrDefault(key, false)) {
            broadcastSuspicion(player, checkName);
            suspicionSent.put(key, true);
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

    private void broadcastSuspicion(Player player, String checkName) {
        String permission = plugin.getConfig().getString("alerts.permission", "anticheat.admin");
        String template = plugin.getConfig().getString("alerts.suspicion-message",
                "&e⚠ &f%player% &7ведёт себя подозрительно &7(&e%check%&7). Проверь: &f/spec %player%");

        String message = template
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace('&', '§');

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                online.sendMessage(message);
            }
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
                violations.entrySet().removeIf(e -> {
                    boolean zero = e.getValue() <= 0;
                    if (zero) suspicionSent.remove(e.getKey());
                    return zero;
                });
            }
        }.runTaskTimer(plugin, interval, interval);
    }
}

package com.limonanarchy.anticheat;

import com.limonanarchy.anticheat.bans.BanEntry;
import com.limonanarchy.anticheat.bans.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Философия: по умолчанию (auto-ban-enabled: false) античит НЕ наказывает игроков сам -
 * это даёт слишком много ложных банов из-за лагов/плохого интернета. Вместо этого он
 * копит статистику подозрительного поведения и репортит живому стаффу, который сам
 * принимает решение через /spec.
 *
 * Если auto-ban-enabled: true (как сейчас настроено в config.yml) - при достижении
 * ban-threshold суммарных нарушений (по ВСЕМ проверкам сразу) плагин банит игрока САМ,
 * через свою собственную систему банов (BanManager -> bans.yml), точно так же, как если
 * бы стафф вручную выполнил /ac ban.
 */
public class ViolationManager {

    private final AntiCheatPlugin plugin;
    private final BanManager banManager;
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();
    // чтобы не спамить одно и то же уведомление каждый тик - шлём раз за уровень, пока не сбросится
    private final Map<String, Integer> lastNotifiedLevel = new ConcurrentHashMap<>();

    public ViolationManager(AntiCheatPlugin plugin, BanManager banManager) {
        this.plugin = plugin;
        this.banManager = banManager;
    }

    public void flag(Player player, String checkName, int weight) {
        if (player.hasPermission("anticheat.bypass")) {
            return;
        }

        String key = player.getUniqueId().toString();
        int newLevel = violations.merge(key, weight, Integer::sum);

        int suspicionThreshold = plugin.getConfig().getInt("punishment.suspicion-threshold", 4);
        int alertThreshold = plugin.getConfig().getInt("punishment.alert-threshold", 8);
        int seriousThreshold = plugin.getConfig().getInt("punishment.serious-threshold", 20);

        if (plugin.getConfig().getBoolean("log-to-console", true)) {
            plugin.getLogger().info(player.getName() + " -> " + checkName + " (level=" + newLevel + ")");
        }

        int lastNotified = lastNotifiedLevel.getOrDefault(key, 0);

        // Шлём уведомление только когда пересекли НОВЫЙ порог (не спамим на каждый +1)
        if (newLevel >= seriousThreshold && lastNotified < seriousThreshold) {
            broadcastSerious(player, checkName, newLevel);
            lastNotifiedLevel.put(key, newLevel);
        } else if (newLevel >= alertThreshold && lastNotified < alertThreshold) {
            broadcastAlert(player, checkName, newLevel);
            lastNotifiedLevel.put(key, newLevel);
        } else if (newLevel >= suspicionThreshold && lastNotified < suspicionThreshold) {
            broadcastSuspicion(player, checkName);
            lastNotifiedLevel.put(key, newLevel);
        }

        boolean autoKick = plugin.getConfig().getBoolean("punishment.auto-kick-enabled", false);
        boolean autoBan = plugin.getConfig().getBoolean("punishment.auto-ban-enabled", false);
        int kickThreshold = plugin.getConfig().getInt("punishment.kick-threshold", 30);
        int banThreshold = plugin.getConfig().getInt("punishment.ban-threshold", 50);

        if (autoBan && newLevel >= banThreshold) {
            autoBanPlayer(player, checkName, newLevel);
            violations.remove(key);
            lastNotifiedLevel.remove(key);
        } else if (autoKick && newLevel >= kickThreshold) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.kickPlayer("§cПодозрительная активность обнаружена (" + checkName + "). Перезайдите на сервер."));
            violations.put(key, kickThreshold / 2);
        }
    }

    /**
     * Настоящий бан через встроенную систему плагина (bans.yml) - тот же механизм,
     * что и ручной /ac ban. Записывается в бан-лист, показывается фирменный экран кика
     * при попытке зайти повторно, и уведомляется онлайн-стафф.
     */
    private void autoBanPlayer(Player player, String checkName, int level) {
        String durationInput = plugin.getConfig().getString("punishment.auto-ban-duration", "perm");
        Long expiresAt = banManager.parseDuration(durationInput);
        if (expiresAt == null) {
            expiresAt = -1L; // некорректный формат в конфиге - баним навсегда, чтобы не пропустить читера
        }

        String reason = "Автобан LimonAntiCheat: " + checkName + " (уровень нарушений " + level + ")";
        banManager.ban(player.getName(), reason, "LimonAntiCheat", expiresAt);

        BanEntry entry = banManager.getBan(player.getName());
        String kickScreen = entry != null
                ? banManager.buildKickScreen(entry)
                : "§4§lВы забанены LimonAntiCheat.\n§7Причина: §f" + reason;

        Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(kickScreen));

        String permission = plugin.getConfig().getString("alerts.permission", "anticheat.admin");
        String staffMessage = ("§4§l⛔ АВТОБАН §c" + player.getName() + " §7(&e" + checkName
                + "&7, уровень &c" + level + "&7) - решение принял сам античит").replace('&', '§');

        broadcast(permission, staffMessage);
        Bukkit.getConsoleSender().sendMessage(staffMessage);
    }

    private void broadcastSuspicion(Player player, String checkName) {
        String permission = plugin.getConfig().getString("alerts.permission", "anticheat.admin");
        String template = plugin.getConfig().getString("alerts.suspicion-message",
                "&e⚠ &f%player% &7ведёт себя подозрительно &7(&e%check%&7). Проверь: &f/spec %player%");

        broadcast(permission, format(template, player, checkName, 0));
    }

    private void broadcastAlert(Player player, String checkName, int level) {
        String permission = plugin.getConfig().getString("alerts.permission", "anticheat.admin");
        String template = plugin.getConfig().getString("alerts.message",
                "&c[AC] &f%player% &7нарушение: &e%check% &7(уровень: &c%level%&7) &7- &f/spec %player%");

        String message = format(template, player, checkName, level);
        broadcast(permission, message);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void broadcastSerious(Player player, String checkName, int level) {
        String permission = plugin.getConfig().getString("alerts.permission", "anticheat.admin");
        String template = plugin.getConfig().getString("alerts.serious-message",
                "&4&l⛔ СЕРЬЁЗНОЕ ПОДОЗРЕНИЕ &c%player% &7(&e%check%&7, уровень &c%level%&7) &f/spec %player% §cСРОЧНО");

        String message = format(template, player, checkName, level);
        broadcast(permission, message);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private String format(String template, Player player, String checkName, int level) {
        return template
                .replace("%player%", player.getName())
                .replace("%check%", checkName)
                .replace("%level%", String.valueOf(level))
                .replace('&', '§');
    }

    private void broadcast(String permission, String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                online.sendMessage(message);
            }
        }
    }

    public int getViolationLevel(String playerName) {
        Player p = Bukkit.getPlayerExact(playerName);
        if (p == null) return 0;
        return violations.getOrDefault(p.getUniqueId().toString(), 0);
    }

    /**
     * Постепенное снижение уровня нарушений со временем,
     * чтобы случайные лаг-спайки не копились вечно.
     */
    public void startDecayTask() {
        long interval = plugin.getConfig().getLong("punishment.decay-interval-seconds", 45) * 20L;
        int decayAmount = plugin.getConfig().getInt("punishment.decay-amount", 2);

        new BukkitRunnable() {
            @Override
            public void run() {
                violations.replaceAll((k, v) -> Math.max(0, v - decayAmount));
                violations.entrySet().removeIf(e -> {
                    boolean zero = e.getValue() <= 0;
                    if (zero) lastNotifiedLevel.remove(e.getKey());
                    return zero;
                });
                lastNotifiedLevel.forEach((k, v) -> {
                    int current = violations.getOrDefault(k, 0);
                    if (current < v) {
                        lastNotifiedLevel.put(k, current);
                    }
                });
            }
        }.runTaskTimer(plugin, interval, interval);
    }
}

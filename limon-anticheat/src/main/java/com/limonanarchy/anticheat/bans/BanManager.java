package com.limonanarchy.anticheat.bans;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BanManager {

    private final AntiCheatPlugin plugin;
    private final File file;
    private final Map<String, BanEntry> bans = new ConcurrentHashMap<>(); // key = lowercase имя игрока

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public BanManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bans.yml");
        load();
    }

    /**
     * Парсит строку длительности вида "7d", "12h", "30m", "45s".
     * "perm", "permanent", "0" - навсегда.
     * Возвращает timestamp окончания бана в мс, либо -1 для перманентного.
     * Возвращает null если формат некорректный.
     */
    public Long parseDuration(String input) {
        if (input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent") || input.equals("0")) {
            return -1L;
        }

        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase());
        if (!matcher.matches()) {
            return null;
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        long millis = switch (unit) {
            case "s" -> amount * 1000L;
            case "m" -> amount * 60_000L;
            case "h" -> amount * 3_600_000L;
            case "d" -> amount * 86_400_000L;
            default -> -1L;
        };

        return System.currentTimeMillis() + millis;
    }

    public void ban(String playerName, String reason, String bannedBy, long expiresAt) {
        BanEntry entry = new BanEntry(playerName, reason, bannedBy, System.currentTimeMillis(), expiresAt);
        bans.put(playerName.toLowerCase(), entry);
        save();
    }

    public boolean unban(String playerName) {
        boolean removed = bans.remove(playerName.toLowerCase()) != null;
        if (removed) save();
        return removed;
    }

    public BanEntry getBan(String playerName) {
        BanEntry entry = bans.get(playerName.toLowerCase());
        if (entry == null) return null;

        if (entry.isExpired()) {
            bans.remove(playerName.toLowerCase());
            save();
            return null;
        }
        return entry;
    }

    public boolean isBanned(String playerName) {
        return getBan(playerName) != null;
    }

    /**
     * Форматирует красивый экран кика при попытке зайти забаненному игроку.
     */
    public String buildKickScreen(BanEntry ban) {
        String timeLeft = ban.isPermanent() ? "§4§lНАВСЕГДА" : formatDuration(ban.getRemainingMillis());
        String appealContact = plugin.getConfig().getString("ban-appeal-contact", "Telegram: @Milan4ck3456");

        return "§4§m――――――――――――――――――――――\n" +
                "§c§l⛔ ВЫ ЗАБАНЕНЫ ⛔\n" +
                "§4§m――――――――――――――――――――――\n" +
                "\n" +
                "§7Причина: §f" + ban.getReason() + "\n" +
                "§7Кем: §f" + ban.getBannedBy() + "\n" +
                "§7Осталось: " + timeLeft + "\n" +
                "\n" +
                "§7Забанили по ошибке? Пиши: §f" + appealContact + "\n" +
                "\n" +
                "§6§lLimon Anarchy §7- §flimon.mc-serv.fun\n" +
                "§4§m――――――――――――――――――――――";
    }

    public String formatDuration(long millis) {
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;

        if (days > 0) return "§e" + days + "д " + hours + "ч";
        if (hours > 0) return "§e" + hours + "ч " + minutes + "м";
        if (minutes > 0) return "§e" + minutes + "м";
        return "§eменее минуты";
    }

    private void load() {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("bans") == null) return;

        for (String key : config.getConfigurationSection("bans").getKeys(false)) {
            String path = "bans." + key + ".";
            String playerName = config.getString(path + "player", key);
            String reason = config.getString(path + "reason", "Нарушение правил");
            String bannedBy = config.getString(path + "bannedBy", "Console");
            long bannedAt = config.getLong(path + "bannedAt", System.currentTimeMillis());
            long expiresAt = config.getLong(path + "expiresAt", -1);

            bans.put(key, new BanEntry(playerName, reason, bannedBy, bannedAt, expiresAt));
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        Map<String, Object> root = new HashMap<>();

        for (Map.Entry<String, BanEntry> entry : bans.entrySet()) {
            BanEntry ban = entry.getValue();
            Map<String, Object> map = new HashMap<>();
            map.put("player", ban.getPlayerName());
            map.put("reason", ban.getReason());
            map.put("bannedBy", ban.getBannedBy());
            map.put("bannedAt", ban.getBannedAt());
            map.put("expiresAt", ban.getExpiresAt());
            config.set("bans." + entry.getKey(), map);
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить bans.yml: " + e.getMessage());
        }
    }
}

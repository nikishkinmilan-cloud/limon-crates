package com.limonanarchy.holograms;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Считаем PvP-убийства сами (не через ванильную статистику, чтобы не тянуть
 * убийства мобов) - только когда убийца - реальный игрок.
 */
public class KillTracker implements Listener {

    private final HologramsPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKnownName = new ConcurrentHashMap<>();

    public KillTracker(HologramsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kills.yml");
        load();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        kills.merge(killer.getUniqueId(), 1, Integer::sum);
        lastKnownName.put(killer.getUniqueId(), killer.getName());
        save();
    }

    /**
     * Возвращает топ-N игроков по убийствам в формате [имя, количество].
     */
    public List<Map.Entry<String, Integer>> getTop(int limit) {
        return kills.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(e -> Map.entry(lastKnownName.getOrDefault(e.getKey(), "???"), e.getValue()))
                .collect(Collectors.toList());
    }

    private void load() {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("kills") == null) return;

        for (String key : config.getConfigurationSection("kills").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int count = config.getInt("kills." + key + ".count");
                String name = config.getString("kills." + key + ".name", "???");
                kills.put(uuid, count);
                lastKnownName.put(uuid, name);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            String key = entry.getKey().toString();
            config.set("kills." + key + ".count", entry.getValue());
            config.set("kills." + key + ".name", lastKnownName.getOrDefault(entry.getKey(), "???"));
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить kills.yml: " + e.getMessage());
        }
    }
}

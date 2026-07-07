package com.limonanarchy.holograms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final HologramsPlugin plugin;
    private final File file;
    private final Map<String, HologramData> holograms = new ConcurrentHashMap<>();
    private final Map<String, TextDisplay> activeEntities = new ConcurrentHashMap<>();

    public HologramManager(HologramsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        load();
    }

    public boolean create(String name, Location location, HologramData.Type type, List<String> lines) {
        if (holograms.containsKey(name.toLowerCase())) {
            return false;
        }

        HologramData data = new HologramData(name, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), type, lines);

        holograms.put(name.toLowerCase(), data);
        spawn(data);
        save();
        return true;
    }

    public boolean remove(String name) {
        HologramData data = holograms.remove(name.toLowerCase());
        if (data == null) return false;

        TextDisplay entity = activeEntities.remove(name.toLowerCase());
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        save();
        return true;
    }

    public boolean moveHere(String name, Location location) {
        HologramData data = holograms.get(name.toLowerCase());
        if (data == null) return false;

        data.setLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        respawn(data);
        save();
        return true;
    }

    public boolean setLines(String name, List<String> lines) {
        HologramData data = holograms.get(name.toLowerCase());
        if (data == null) return false;

        data.setLines(lines);
        updateText(name.toLowerCase(), lines);
        save();
        return true;
    }

    public Collection<HologramData> getAll() {
        return holograms.values();
    }

    /**
     * Обновляет только текст (используется LeaderboardTask для авто-топов),
     * не трогая позицию сущности.
     */
    public void updateText(String name, List<String> lines) {
        TextDisplay entity = activeEntities.get(name.toLowerCase());
        if (entity == null || entity.isDead()) return;

        String joined = String.join("\n", lines);
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(joined);
        entity.text(component);
    }

    /**
     * Заспавнить все голограммы из памяти - вызывается при onEnable.
     */
    public void spawnAll() {
        for (HologramData data : holograms.values()) {
            spawn(data);
        }
    }

    /**
     * Убрать все заспавненные сущности из мира - вызывается при onDisable,
     * чтобы не оставлять "осиротевшие" TextDisplay при перезапуске/пересборке.
     */
    public void despawnAll() {
        for (TextDisplay entity : activeEntities.values()) {
            if (!entity.isDead()) {
                entity.remove();
            }
        }
        activeEntities.clear();
    }

    private void spawn(HologramData data) {
        World world = Bukkit.getWorld(data.getWorld());
        if (world == null) {
            plugin.getLogger().warning("Мир " + data.getWorld() + " не найден для голограммы " + data.getName());
            return;
        }

        // если уже была заспавнена (например при reload) - убираем старую сначала
        TextDisplay existing = activeEntities.remove(data.getName().toLowerCase());
        if (existing != null && !existing.isDead()) {
            existing.remove();
        }

        Location location = new Location(world, data.getX(), data.getY(), data.getZ());
        TextDisplay display = world.spawn(location, TextDisplay.class);

        display.setBillboard(Display.Billboard.CENTER); // всегда развёрнут лицом к камере
        display.setShadowed(true);
        display.setSeeThrough(false);
        display.setDefaultBackground(true);
        display.setPersistent(true);

        String joined = String.join("\n", data.getLines().isEmpty()
                ? List.of("&7(пусто, используй /holo addline)")
                : data.getLines());
        display.text(LegacyComponentSerializer.legacyAmpersand().deserialize(joined));

        activeEntities.put(data.getName().toLowerCase(), display);
    }

    private void respawn(HologramData data) {
        spawn(data);
    }

    private void load() {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("holograms") == null) return;

        for (String key : config.getConfigurationSection("holograms").getKeys(false)) {
            String path = "holograms." + key + ".";
            String world = config.getString(path + "world", "world");
            double x = config.getDouble(path + "x");
            double y = config.getDouble(path + "y");
            double z = config.getDouble(path + "z");
            String typeStr = config.getString(path + "type", "STATIC");
            List<String> lines = config.getStringList(path + "lines");

            HologramData.Type type;
            try {
                type = HologramData.Type.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                type = HologramData.Type.STATIC;
            }

            holograms.put(key, new HologramData(key, world, x, y, z, type, lines));
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();

        for (HologramData data : holograms.values()) {
            String path = "holograms." + data.getName().toLowerCase() + ".";
            config.set(path + "world", data.getWorld());
            config.set(path + "x", data.getX());
            config.set(path + "y", data.getY());
            config.set(path + "z", data.getZ());
            config.set(path + "type", data.getType().name());
            config.set(path + "lines", data.getLines());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить holograms.yml: " + e.getMessage());
        }
    }
}

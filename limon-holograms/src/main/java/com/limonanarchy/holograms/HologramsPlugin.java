package com.limonanarchy.holograms;

import org.bukkit.plugin.java.JavaPlugin;

public class HologramsPlugin extends JavaPlugin {

    private HologramManager hologramManager;
    private KillTracker killTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.hologramManager = new HologramManager(this);
        this.killTracker = new KillTracker(this);

        getServer().getPluginManager().registerEvents(killTracker, this);
        getCommand("holo").setExecutor(new HologramCommand(hologramManager));

        // Заспавнить все сохранённые голограммы (в том числе после рестарта сервера)
        hologramManager.spawnAll();

        // Периодическое обновление авто-топов
        long intervalTicks = getConfig().getLong("leaderboard-update-interval-seconds", 30) * 20L;
        new LeaderboardTask(this, hologramManager, killTracker)
                .runTaskTimer(this, 40L, intervalTicks);

        getLogger().info("LimonHolograms включен. Голограмм загружено: " + hologramManager.getAll().size());
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.despawnAll();
        }
        getLogger().info("LimonHolograms выключен.");
    }
}

package com.limonanarchy.anticheat.reports;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportManager {

    private final AntiCheatPlugin plugin;
    private final File file;
    private final List<Report> reports = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public ReportManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
        load();
    }

    public Report addReport(String reporter, String reported, String reason) {
        Report report = new Report(nextId.getAndIncrement(), reporter, reported, reason, System.currentTimeMillis());
        reports.add(report);
        save();
        return report;
    }

    public List<Report> getAllReports() {
        return new ArrayList<>(reports);
    }

    public boolean removeReport(int id) {
        boolean removed = reports.removeIf(r -> r.getId() == id);
        if (removed) save();
        return removed;
    }

    public void clearAll() {
        reports.clear();
        save();
    }

    private void load() {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> rawList = config.getList("reports");
        if (rawList == null) return;

        int maxId = 0;
        for (Object obj : rawList) {
            if (!(obj instanceof java.util.Map<?, ?> map)) continue;
            int id = (int) map.get("id");
            String reporter = (String) map.get("reporter");
            String reported = (String) map.get("reported");
            String reason = (String) map.get("reason");
            long timestamp = map.get("timestamp") instanceof Number n ? n.longValue() : System.currentTimeMillis();

            reports.add(new Report(id, reporter, reported, reason, timestamp));
            maxId = Math.max(maxId, id);
        }
        nextId.set(maxId + 1);
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<java.util.Map<String, Object>> serialized = new ArrayList<>();

        for (Report r : reports) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", r.getId());
            map.put("reporter", r.getReporterName());
            map.put("reported", r.getReportedName());
            map.put("reason", r.getReason());
            map.put("timestamp", r.getTimestamp());
            serialized.add(map);
        }

        config.set("reports", serialized);

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить reports.yml: " + e.getMessage());
        }
    }
}

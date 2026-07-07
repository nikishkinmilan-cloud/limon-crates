package com.limonanarchy.holograms;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LeaderboardTask extends BukkitRunnable {

    private final HologramsPlugin plugin;
    private final HologramManager hologramManager;
    private final KillTracker killTracker;
    private Economy economy;

    public LeaderboardTask(HologramsPlugin plugin, HologramManager hologramManager, KillTracker killTracker) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.killTracker = killTracker;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault не найден - топ богачей работать не будет. Убедись что Vault и плагин экономики (EssentialsX) установлены.");
            return;
        }
        var provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
        }
    }

    @Override
    public void run() {
        int limit = plugin.getConfig().getInt("leaderboard-size", 10);

        for (HologramData data : hologramManager.getAll()) {
            if (data.getType() == HologramData.Type.RICHEST) {
                hologramManager.updateText(data.getName(), buildRichestLines(limit));
            } else if (data.getType() == HologramData.Type.PVP) {
                hologramManager.updateText(data.getName(), buildPvpLines(limit));
            }
        }
    }

    private List<String> buildRichestLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add(plugin.getConfig().getString("richest-title", "&6&l💰 ТОП БОГАЧЕЙ 💰"));

        if (economy == null) {
            lines.add("&7Vault/экономика не настроены");
            return lines;
        }

        List<OfflinePlayer> allPlayers = new ArrayList<>(List.of(Bukkit.getOfflinePlayers()));
        allPlayers.sort(Comparator.comparingDouble((OfflinePlayer p) -> economy.getBalance(p)).reversed());

        String lineFormat = plugin.getConfig().getString("richest-line-format", "&e#%rank% &f%player% &7- &6%value%");
        String emptyFormat = plugin.getConfig().getString("empty-line-format", "&8#%rank% &7- пусто -");

        for (int i = 0; i < limit; i++) {
            int rank = i + 1;
            if (i < allPlayers.size()) {
                OfflinePlayer p = allPlayers.get(i);
                double balance = economy.getBalance(p);
                lines.add(lineFormat
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%player%", p.getName() != null ? p.getName() : "???")
                        .replace("%value%", String.format("%.0f", balance)));
            } else {
                lines.add(emptyFormat.replace("%rank%", String.valueOf(rank)));
            }
        }
        return lines;
    }

    private List<String> buildPvpLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add(plugin.getConfig().getString("pvp-title", "&c&l⚔ ТОП PVP ⚔"));

        List<Map.Entry<String, Integer>> top = killTracker.getTop(limit);
        String lineFormat = plugin.getConfig().getString("pvp-line-format", "&e#%rank% &f%player% &7- &c%value% убийств");
        String emptyFormat = plugin.getConfig().getString("empty-line-format", "&8#%rank% &7- пусто -");

        for (int i = 0; i < limit; i++) {
            int rank = i + 1;
            if (i < top.size()) {
                Map.Entry<String, Integer> entry = top.get(i);
                lines.add(lineFormat
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%player%", entry.getKey())
                        .replace("%value%", String.valueOf(entry.getValue())));
            } else {
                lines.add(emptyFormat.replace("%rank%", String.valueOf(rank)));
            }
        }
        return lines;
    }
}

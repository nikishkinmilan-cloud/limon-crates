package com.limonanarchy.anticheat;

import com.limonanarchy.anticheat.bans.BanEntry;
import com.limonanarchy.anticheat.bans.BanLoginListener;
import com.limonanarchy.anticheat.bans.BanManager;
import com.limonanarchy.anticheat.checks.CombatCheck;
import com.limonanarchy.anticheat.checks.FlyCheck;
import com.limonanarchy.anticheat.checks.SpeedCheck;
import com.limonanarchy.anticheat.commands.ReportCommand;
import com.limonanarchy.anticheat.commands.ReportsCommand;
import com.limonanarchy.anticheat.commands.SpecCommand;
import com.limonanarchy.anticheat.reports.ReportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiCheatPlugin extends JavaPlugin {

    private ViolationManager violationManager;
    private FlyCheck flyCheck;
    private SpeedCheck speedCheck;
    private CombatCheck combatCheck;
    private ReportManager reportManager;
    private BanManager banManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.violationManager = new ViolationManager(this);
        this.reportManager = new ReportManager(this);
        this.banManager = new BanManager(this);

        this.flyCheck = new FlyCheck(this, violationManager);
        this.speedCheck = new SpeedCheck(this, violationManager);
        this.combatCheck = new CombatCheck(this, violationManager);

        getServer().getPluginManager().registerEvents(flyCheck, this);
        getServer().getPluginManager().registerEvents(speedCheck, this);
        getServer().getPluginManager().registerEvents(combatCheck, this);
        getServer().getPluginManager().registerEvents(new BanLoginListener(banManager), this);

        getCommand("report").setExecutor(new ReportCommand(this, reportManager));
        getCommand("reports").setExecutor(new ReportsCommand(reportManager));
        getCommand("spec").setExecutor(new SpecCommand());

        violationManager.startDecayTask();

        getLogger().info("LimonAntiCheat включен. Проверки: fly=" 
                + getConfig().getBoolean("fly.enabled") 
                + ", speed=" + getConfig().getBoolean("speed.enabled"));
    }

    @Override
    public void onDisable() {
        getLogger().info("LimonAntiCheat выключен.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("anticheat.admin")) {
            sender.sendMessage("§cУ тебя нет прав на эту команду.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eИспользование: /ac <reload|violations|ban|unban>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                sender.sendMessage("§aКонфиг LimonAntiCheat перезагружен.");
                break;
            case "violations":
                if (args.length < 2) {
                    sender.sendMessage("§eИспользование: /ac violations <ник>");
                    return true;
                }
                int level = violationManager.getViolationLevel(args[1]);
                sender.sendMessage("§eУровень нарушений §f" + args[1] + "§e: §c" + level);
                break;
            case "ban":
                if (args.length < 3) {
                    sender.sendMessage("§eИспользование: /ac ban <ник> <время> <причина>");
                    sender.sendMessage("§7Время: §f7d §7(7 дней), §f12h §7(12 часов), §f30m §7(30 минут), §fperm §7(навсегда)");
                    return true;
                }

                String targetName = args[1];
                String durationInput = args[2];

                Long expiresAt = banManager.parseDuration(durationInput);
                if (expiresAt == null) {
                    sender.sendMessage("§cНекорректный формат времени. Примеры: 7d, 12h, 30m, perm");
                    return true;
                }

                StringBuilder reasonBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    reasonBuilder.append(args[i]).append(" ");
                }
                String reason = reasonBuilder.toString().trim();
                if (reason.isEmpty()) {
                    reason = "Обнаружен читерский софт (LimonAntiCheat)";
                }

                banManager.ban(targetName, reason, sender.getName(), expiresAt);

                BanEntry newBan = banManager.getBan(targetName);
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && newBan != null) {
                    target.kickPlayer(banManager.buildKickScreen(newBan));
                }

                String durationText = expiresAt == -1 ? "навсегда" : "до " + banManager.formatDuration(expiresAt - System.currentTimeMillis());
                sender.sendMessage("§a" + targetName + " §fзабанен (" + durationText + "). §7Причина: " + reason);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("anticheat.admin") && !online.equals(sender)) {
                        online.sendMessage("§c[AC] §f" + sender.getName() + " §7забанил §f" + targetName
                                + " §7(" + durationText + ", " + reason + ")");
                    }
                }
                break;
            case "unban":
                if (args.length < 2) {
                    sender.sendMessage("§eИспользование: /ac unban <ник>");
                    return true;
                }
                boolean unbanned = banManager.unban(args[1]);
                sender.sendMessage(unbanned
                        ? "§a" + args[1] + " §fразбанен."
                        : "§c" + args[1] + " §fне найден в списке банов.");
                break;
            default:
                sender.sendMessage("§eИспользование: /ac <reload|violations|ban|unban>");
        }
        return true;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }
}

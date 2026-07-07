package com.limonanarchy.anticheat.commands;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.reports.Report;
import com.limonanarchy.anticheat.reports.ReportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {

    private final AntiCheatPlugin plugin;
    private final ReportManager reportManager;

    public ReportCommand(AntiCheatPlugin plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage("§cЭта команда только для игроков.");
            return true;
        }

        if (args.length < 2) {
            reporter.sendMessage("§eИспользование: /report <ник> <причина>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            reporter.sendMessage("§cИгрок §f" + targetName + " §cне в сети или не существует.");
            return true;
        }

        if (target.getUniqueId().equals(reporter.getUniqueId())) {
            reporter.sendMessage("§cНельзя пожаловаться на самого себя.");
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        Report report = reportManager.addReport(reporter.getName(), target.getName(), reason);

        reporter.sendMessage("§aЖалоба на §f" + target.getName() + " §aотправлена персоналу. (ID: " + report.getId() + ")");

        // Уведомляем онлайн-модераторов сразу
        String staffMessage = "§c[Report] §f" + reporter.getName() + " §7пожаловался на §f" + target.getName()
                + " §7(" + reason + ") §8- §7/reports";
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("reports.view")) {
                online.sendMessage(staffMessage);
            }
        }

        return true;
    }
}

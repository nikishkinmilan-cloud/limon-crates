package com.limonanarchy.anticheat.commands;

import com.limonanarchy.anticheat.reports.Report;
import com.limonanarchy.anticheat.reports.ReportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportsCommand implements CommandExecutor {

    private final ReportManager reportManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm");

    public ReportsCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Report> reports = reportManager.getAllReports();

        // /reports clear <id> - закрыть конкретную жалобу
        if (args.length >= 2 && args[0].equalsIgnoreCase("clear")) {
            try {
                int id = Integer.parseInt(args[1]);
                boolean removed = reportManager.removeReport(id);
                sender.sendMessage(removed
                        ? "§aЖалоба #" + id + " закрыта."
                        : "§cЖалоба #" + id + " не найдена.");
            } catch (NumberFormatException e) {
                sender.sendMessage("§cID должен быть числом.");
            }
            return true;
        }

        // /reports clearall - закрыть все сразу
        if (args.length >= 1 && args[0].equalsIgnoreCase("clearall")) {
            reportManager.clearAll();
            sender.sendMessage("§aВсе жалобы очищены.");
            return true;
        }

        if (reports.isEmpty()) {
            sender.sendMessage("§aЖалоб нет. Всё чисто.");
            return true;
        }

        sender.sendMessage("§6§l=== Жалобы (" + reports.size() + ") ===");
        for (Report r : reports) {
            boolean online = Bukkit.getPlayerExact(r.getReportedName()) != null;
            String status = online ? "§a[в сети]" : "§7[не в сети]";

            sender.sendMessage(
                    "§7#" + r.getId() + " §f" + r.getReportedName() + " " + status
                            + " §7- от §f" + r.getReporterName()
                            + " §7(" + dateFormat.format(new Date(r.getTimestamp())) + ")"
            );
            sender.sendMessage("  §7Причина: §f" + r.getReason());
        }
        sender.sendMessage("§7Закрыть: §f/reports clear <id> §7| Очистить всё: §f/reports clearall");

        return true;
    }
}

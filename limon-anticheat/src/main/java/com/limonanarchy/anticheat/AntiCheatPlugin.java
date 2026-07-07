package com.limonanarchy.anticheat;

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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.violationManager = new ViolationManager(this);
        this.reportManager = new ReportManager(this);

        this.flyCheck = new FlyCheck(this, violationManager);
        this.speedCheck = new SpeedCheck(this, violationManager);
        this.combatCheck = new CombatCheck(this, violationManager);

        getServer().getPluginManager().registerEvents(flyCheck, this);
        getServer().getPluginManager().registerEvents(speedCheck, this);
        getServer().getPluginManager().registerEvents(combatCheck, this);

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
            sender.sendMessage("§eИспользование: /ac <reload|violations|ban>");
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
                if (args.length < 2) {
                    sender.sendMessage("§eИспользование: /ac ban <ник> [причина]");
                    return true;
                }
                String targetName = args[1];
                Player target = Bukkit.getPlayerExact(targetName);

                StringBuilder reasonBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    reasonBuilder.append(args[i]).append(" ");
                }
                String reason = reasonBuilder.toString().trim();
                if (reason.isEmpty()) {
                    reason = "Обнаружен читерский софт (LimonAntiCheat)";
                }

                final String finalReason = reason;
                if (target != null) {
                    target.kickPlayer("§cВы забанены античитом.\n§7Причина: " + finalReason);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "ban " + targetName + " " + finalReason);

                sender.sendMessage("§a" + targetName + " §fзабанен через античит. §7(" + finalReason + ")");

                // уведомляем стафф
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("anticheat.admin") && !online.equals(sender)) {
                        online.sendMessage("§c[AC] §f" + sender.getName() + " §7забанил §f" + targetName
                                + " §7через античит (" + finalReason + ")");
                    }
                }
                break;
            default:
                sender.sendMessage("§eИспользование: /ac <reload|violations|ban>");
        }
        return true;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }
}

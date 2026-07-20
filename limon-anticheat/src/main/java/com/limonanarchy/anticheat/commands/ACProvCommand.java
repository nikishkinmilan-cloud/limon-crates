package com.limonanarchy.anticheat.commands;

import com.limonanarchy.anticheat.review.ReviewManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /acprov setroom target    - сохранить текущую позицию как место для подозреваемого
 * /acprov setroom reviewer  - сохранить текущую позицию как место для проверяющего
 * /acprov <ник>             - вызвать игрока на проверку (телепорт обоих в камеру + title + чат-промпт)
 * /acprov <ник> off         - закончить проверку, вернуть игрока туда, где он был
 */
public class ACProvCommand implements CommandExecutor {

    private final ReviewManager reviewManager;

    public ACProvCommand(ReviewManager reviewManager) {
        this.reviewManager = reviewManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reviewer)) {
            sender.sendMessage("§cЭта команда только для игроков.");
            return true;
        }

        if (args.length < 1) {
            reviewer.sendMessage("§eИспользование: §f/acprov <ник> §7- вызвать на проверку");
            reviewer.sendMessage("§7           §f/acprov <ник> off §7- закончить проверку");
            reviewer.sendMessage("§7           §f/acprov setroom <target|reviewer> §7- настроить камеру");
            return true;
        }

        if (args[0].equalsIgnoreCase("setroom")) {
            if (args.length < 2 || (!args[1].equalsIgnoreCase("target") && !args[1].equalsIgnoreCase("reviewer"))) {
                reviewer.sendMessage("§eИспользование: /acprov setroom <target|reviewer>");
                return true;
            }
            String key = args[1].toLowerCase();
            reviewManager.saveRoomSpot(key, reviewer.getLocation());
            reviewer.sendMessage("§aТочка камеры §f\"" + key + "\" §aсохранена на твоей текущей позиции.");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            reviewer.sendMessage("§cИгрок §f" + targetName + " §cне в сети.");
            return true;
        }

        boolean stop = args.length >= 2 && args[1].equalsIgnoreCase("off");

        if (stop) {
            if (!reviewManager.isUnderReview(target.getUniqueId())) {
                reviewer.sendMessage("§c" + target.getName() + " §cсейчас не на проверке.");
                return true;
            }
            reviewManager.endReview(target, reviewer);
            return true;
        }

        if (reviewManager.isUnderReview(target.getUniqueId())) {
            reviewer.sendMessage("§c" + target.getName() + " §cуже на проверке.");
            return true;
        }

        if (!reviewManager.isRoomConfigured()) {
            reviewer.sendMessage("§cКамера проверки ещё не настроена. Встань в нужных местах и выполни:");
            reviewer.sendMessage("§7/acprov setroom target §7(где будет стоять подозреваемый)");
            reviewer.sendMessage("§7/acprov setroom reviewer §7(где будешь стоять ты)");
            return true;
        }

        boolean started = reviewManager.startReview(target, reviewer);
        if (!started) {
            reviewer.sendMessage("§cНе удалось начать проверку - камера не настроена.");
        }
        return true;
    }
}

package com.limonanarchy.spheres.commands;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveSphereShardCommand implements CommandExecutor {

    private final LimonSpheresPlugin plugin;

    public GiveSphereShardCommand(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("limonspheres.give")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /givesphereshard <ник> <количество>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден или не в сети.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Количество должно быть числом.");
            return true;
        }

        plugin.getShardManager().addShards(target, amount);
        sender.sendMessage(ChatColor.GREEN + "Выдано " + amount + " осколков сфер игроку " + target.getName());
        target.sendMessage(ChatColor.GREEN + "Вам выдано " + amount + " осколков сфер.");
        return true;
    }
}

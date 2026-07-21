package com.limonanarchy.spheres.commands;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SpheresCommand implements CommandExecutor {

    private final LimonSpheresPlugin plugin;

    public SpheresCommand(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("limonspheres.admin")) {
                sender.sendMessage(ChatColor.RED + "Недостаточно прав.");
                return true;
            }
            plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "LimonSpheres: конфиг перезагружен.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== LimonSpheres ===");
        sender.sendMessage(ChatColor.GRAY + "/spheromant " + ChatColor.WHITE + "- меню обмена осколков на сферы");
        sender.sendMessage(ChatColor.GRAY + "Носите сферу в офф-руке, чтобы получать её эффекты.");
        sender.sendMessage(ChatColor.GRAY + "Улучшайте сферы в наковальне, переплавляйте в печи на осколки.");
        return true;
    }
}

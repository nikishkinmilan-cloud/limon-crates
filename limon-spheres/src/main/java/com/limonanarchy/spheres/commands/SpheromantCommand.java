package com.limonanarchy.spheres.commands;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpheromantCommand implements CommandExecutor {

    private final LimonSpheresPlugin plugin;

    public SpheromantCommand(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков.");
            return true;
        }
        plugin.getSpheromantGUI().open(player);
        return true;
    }
}

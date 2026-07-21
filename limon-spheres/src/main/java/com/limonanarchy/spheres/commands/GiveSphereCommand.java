package com.limonanarchy.spheres.commands;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import com.limonanarchy.spheres.SphereTier;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /givesphere <ник> <тип> <NORMAL|EPIC|LEGENDARY> [второй_тип для MYTHIC]
 */
public class GiveSphereCommand implements CommandExecutor {

    private final LimonSpheresPlugin plugin;

    public GiveSphereCommand(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("limonspheres.give")) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /givesphere <ник> <тип> <NORMAL|EPIC|LEGENDARY> [тип2 для MYTHIC]");
            sender.sendMessage(ChatColor.YELLOW + "Доступные типы: " + String.join(", ", plugin.getSphereRegistry().getKeys()));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден или не в сети.");
            return true;
        }

        String type = args[1].toUpperCase();
        if (!plugin.getSphereRegistry().exists(type)) {
            sender.sendMessage(ChatColor.RED + "Неизвестный тип сферы. Доступные: " + String.join(", ", plugin.getSphereRegistry().getKeys()));
            return true;
        }

        SphereTier tier;
        try {
            tier = SphereTier.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Неизвестный уровень. Доступные: NORMAL, EPIC, LEGENDARY, MYTHIC");
            return true;
        }

        ItemStack sphere;
        if (tier == SphereTier.MYTHIC) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Для MYTHIC укажите второй тип: /givesphere <ник> <тип1> MYTHIC <тип2>");
                return true;
            }
            String type2 = args[3].toUpperCase();
            if (!plugin.getSphereRegistry().exists(type2)) {
                sender.sendMessage(ChatColor.RED + "Неизвестный второй тип сферы.");
                return true;
            }
            sphere = plugin.getSphereManager().createMythicSphere(type, type2);
        } else {
            sphere = plugin.getSphereManager().createSphere(type, tier);
        }

        var leftover = target.getInventory().addItem(sphere);
        leftover.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));

        sender.sendMessage(ChatColor.GREEN + "Выдана сфера игроку " + target.getName());
        target.sendMessage(ChatColor.GREEN + "Вам выдана сфера от администрации.");
        return true;
    }
}

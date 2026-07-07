package com.limonanarchy.anticheat.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /spec <ник> - переключает модератора в спектейтор и телепортирует к игроку,
 * чтобы незаметно следить за подозрительным поведением.
 * /spec off - вернуться обратно на то место и в тот режим игры, что был до слежки.
 */
public class SpecCommand implements CommandExecutor {

    // Сохраняем состояние ДО входа в спектейтор, чтобы корректно вернуть обратно
    private final Map<UUID, GameMode> previousGameMode = new HashMap<>();
    private final Map<UUID, Location> previousLocation = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player moderator)) {
            sender.sendMessage("§cЭта команда только для игроков.");
            return true;
        }

        if (args.length < 1) {
            moderator.sendMessage("§eИспользование: /spec <ник|off>");
            return true;
        }

        UUID modId = moderator.getUniqueId();

        if (args[0].equalsIgnoreCase("off")) {
            if (!previousGameMode.containsKey(modId)) {
                moderator.sendMessage("§cТы сейчас не в режиме слежки.");
                return true;
            }

            moderator.setGameMode(previousGameMode.remove(modId));
            Location loc = previousLocation.remove(modId);
            if (loc != null) {
                moderator.teleport(loc);
            }
            moderator.sendMessage("§aСлежка остановлена, ты вернулся на место.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            moderator.sendMessage("§cИгрок §f" + args[0] + " §cне в сети.");
            return true;
        }

        // Первый запуск /spec - запоминаем откуда начали, чтобы /spec off вернул именно сюда,
        // а не туда, где закончилась предыдущая слежка (если модератор переключался между игроками)
        if (!previousGameMode.containsKey(modId)) {
            previousGameMode.put(modId, moderator.getGameMode());
            previousLocation.put(modId, moderator.getLocation());
        }

        moderator.setGameMode(GameMode.SPECTATOR);
        moderator.teleport(target.getLocation());
        // В спектейторе можно "прилипнуть" камерой к игроку через teleport на entity дальше вручную,
        // но базовая телепортация плюс спектейтор-режим уже позволяет свободно наблюдать рядом
        moderator.sendMessage("§aСледишь за §f" + target.getName() + "§a. §7/spec off §aчтобы вернуться.");

        return true;
    }
}

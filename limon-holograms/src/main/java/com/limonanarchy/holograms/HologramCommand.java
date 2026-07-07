package com.limonanarchy.holograms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HologramCommand implements CommandExecutor {

    private final HologramManager hologramManager;

    public HologramCommand(HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда только для игроков (нужна твоя текущая позиция).");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage("§eИспользование: /holo create <имя> <static|richest|pvp> [строка1|строка2|...]");
                    return true;
                }
                String name = args[1];
                String typeStr = args[2].toUpperCase();

                HologramData.Type type;
                try {
                    type = HologramData.Type.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cТип должен быть: static, richest или pvp");
                    return true;
                }

                List<String> lines = new ArrayList<>();
                if (args.length > 3) {
                    StringBuilder joined = new StringBuilder();
                    for (int i = 3; i < args.length; i++) joined.append(args[i]).append(" ");
                    // строки разделяются символом |
                    lines.addAll(List.of(joined.toString().trim().split("\\|")));
                }

                boolean created = hologramManager.create(name, player.getLocation(), type, lines);
                player.sendMessage(created
                        ? "§aГолограмма §f" + name + " §aсоздана на твоей позиции."
                        : "§cГолограмма с именем §f" + name + " §cуже существует.");
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage("§eИспользование: /holo remove <имя>");
                    return true;
                }
                boolean removed = hologramManager.remove(args[1]);
                player.sendMessage(removed
                        ? "§aГолограмма §f" + args[1] + " §aудалена."
                        : "§cГолограмма §f" + args[1] + " §cне найдена.");
            }
            case "movehere" -> {
                if (args.length < 2) {
                    player.sendMessage("§eИспользование: /holo movehere <имя>");
                    return true;
                }
                boolean moved = hologramManager.moveHere(args[1], player.getLocation());
                player.sendMessage(moved
                        ? "§aГолограмма §f" + args[1] + " §aперемещена на твою позицию."
                        : "§cГолограмма §f" + args[1] + " §cне найдена.");
            }
            case "addline", "setlines" -> {
                if (args.length < 3) {
                    player.sendMessage("§eИспользование: /holo setlines <имя> <строка1|строка2|...>");
                    return true;
                }
                StringBuilder joined = new StringBuilder();
                for (int i = 2; i < args.length; i++) joined.append(args[i]).append(" ");
                List<String> lines = List.of(joined.toString().trim().split("\\|"));

                boolean updated = hologramManager.setLines(args[1], lines);
                player.sendMessage(updated
                        ? "§aТекст голограммы §f" + args[1] + " §aобновлён."
                        : "§cГолограмма §f" + args[1] + " §cне найдена.");
            }
            case "list" -> {
                if (hologramManager.getAll().isEmpty()) {
                    player.sendMessage("§7Голограмм пока нет.");
                    return true;
                }
                player.sendMessage("§6§l=== Голограммы ===");
                for (HologramData data : hologramManager.getAll()) {
                    player.sendMessage("§7- §f" + data.getName() + " §7(" + data.getType() + ") §8@ " + data.getWorld()
                            + " " + (int) data.getX() + "," + (int) data.getY() + "," + (int) data.getZ());
                }
            }
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage("§eКоманды голограмм:");
        player.sendMessage("§7/holo create <имя> <static|richest|pvp> [строка1|строка2] §8- создать на твоей позиции");
        player.sendMessage("§7/holo remove <имя> §8- удалить");
        player.sendMessage("§7/holo movehere <имя> §8- переместить на твою позицию");
        player.sendMessage("§7/holo setlines <имя> <строка1|строка2> §8- изменить текст (только для static)");
        player.sendMessage("§7/holo list §8- список всех голограмм");
    }
}

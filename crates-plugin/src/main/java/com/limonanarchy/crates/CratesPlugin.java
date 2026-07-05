package com.limonanarchy.crates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CratesPlugin extends JavaPlugin {

    private NamespacedKey keyTag;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyTag = new NamespacedKey(this, "crate_key");
        getLogger().info("LimonCrates включён! Кейсов загружено: " + getConfig().getConfigurationSection("crates").getKeys(false).size());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "cratekey":
                return handleCrateKey(sender, args);
            case "crate":
                return handleCrateOpen(sender, args);
            case "crates":
                return handleCratesList(sender, args);
            default:
                return false;
        }
    }

    // ============ /cratekey give <игрок> <кейс> <количество> ============
    private boolean handleCrateKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.admin")) {
            sender.sendMessage(ChatColor.RED + "У тебя нет прав на эту команду.");
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /cratekey give <игрок> <кейс> [количество]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок " + args[1] + " не найден (должен быть онлайн).");
            return true;
        }

        String crateId = args[2];
        ConfigurationSection crateSection = getConfig().getConfigurationSection("crates." + crateId);
        if (crateSection == null) {
            sender.sendMessage(ChatColor.RED + "Кейс '" + crateId + "' не найден в config.yml.");
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть числом.");
                return true;
            }
        }

        ItemStack key = createKeyItem(crateId, crateSection.getString("display-name", crateId), amount);
        target.getInventory().addItem(key);

        sender.sendMessage(ChatColor.GREEN + "Выдано " + amount + " ключ(ей) от '" + crateId + "' игроку " + target.getName());
        target.sendMessage(ChatColor.GREEN + "Тебе выдано " + amount + " ключ(ей) от " + ChatColor.translateAlternateColorCodes('&', crateSection.getString("display-name", crateId)));
        return true;
    }

    private ItemStack createKeyItem(String crateId, String displayName, int amount) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f&lКлюч: " + displayName));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Используй команду:");
        lore.add(ChatColor.YELLOW + "/crate open " + crateId);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, crateId);
        item.setItemMeta(meta);
        return item;
    }

    // ============ /crate open <кейс> ============
    private boolean handleCrateOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду можно использовать только в игре.");
            return true;
        }
        if (!sender.hasPermission("crates.open")) {
            sender.sendMessage(ChatColor.RED + "У тебя нет прав открывать кейсы.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2 || !args[0].equalsIgnoreCase("open")) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /crate open <кейс>");
            return true;
        }

        String crateId = args[1];
        ConfigurationSection crateSection = getConfig().getConfigurationSection("crates." + crateId);
        if (crateSection == null) {
            player.sendMessage(ChatColor.RED + "Кейс '" + crateId + "' не существует.");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !hand.hasItemMeta()) {
            player.sendMessage(ChatColor.RED + "Возьми в руку ключ от этого кейса.");
            return true;
        }
        String heldCrate = hand.getItemMeta().getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
        if (heldCrate == null || !heldCrate.equals(crateId)) {
            player.sendMessage(ChatColor.RED + "В руке должен быть ключ именно от '" + crateId + "'.");
            return true;
        }

        // Списываем один ключ
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        giveRandomReward(player, crateId, crateSection);
        return true;
    }

    private void giveRandomReward(Player player, String crateId, ConfigurationSection crateSection) {
        List<?> rewards = crateSection.getList("rewards");
        if (rewards == null || rewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "У этого кейса нет наград (проверь config.yml).");
            return;
        }

        int totalWeight = 0;
        List<ConfigurationSection> rewardSections = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            ConfigurationSection rs = crateSection.getConfigurationSection("rewards." + i);
            if (rs != null) {
                rewardSections.add(rs);
                totalWeight += rs.getInt("weight", 1);
            }
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        ConfigurationSection chosen = rewardSections.get(0);
        for (ConfigurationSection rs : rewardSections) {
            cumulative += rs.getInt("weight", 1);
            if (roll < cumulative) {
                chosen = rs;
                break;
            }
        }

        String rewardName = ChatColor.translateAlternateColorCodes('&', chosen.getString("name", "Награда"));
        String type = chosen.getString("type", "ITEM");

        if (type.equalsIgnoreCase("ITEM")) {
            Material mat = Material.matchMaterial(chosen.getString("material", "STONE"));
            int amount = chosen.getInt("amount", 1);
            if (mat != null) {
                player.getInventory().addItem(new ItemStack(mat, amount));
            }
        } else if (type.equalsIgnoreCase("COMMAND")) {
            String cmd = chosen.getString("command", "").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        player.sendMessage(ChatColor.GOLD + "Ты открыл кейс и получил: " + ChatColor.WHITE + rewardName);

        if (getConfig().getBoolean("broadcast-on-open", true)) {
            String displayName = ChatColor.translateAlternateColorCodes('&', crateSection.getString("display-name", crateId));
            String format = getConfig().getString("broadcast-format", "&e%player% &7открыл &6%crate%");
            format = format.replace("%player%", player.getName())
                    .replace("%crate%", displayName)
                    .replace("%reward%", rewardName);
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', format));
        }
    }

    // ============ /crates list | reload ============
    private boolean handleCratesList(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("crates.admin")) {
                sender.sendMessage(ChatColor.RED + "У тебя нет прав на эту команду.");
                return true;
            }
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Конфиг кейсов перезагружен.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Доступные кейсы:");
        for (String crateId : getConfig().getConfigurationSection("crates").getKeys(false)) {
            String name = getConfig().getString("crates." + crateId + ".display-name", crateId);
            sender.sendMessage(ChatColor.GRAY + " - " + crateId + ": " + ChatColor.translateAlternateColorCodes('&', name));
        }
        return true;
    }
}

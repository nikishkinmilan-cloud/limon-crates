package com.limonanarchy.crates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CratesPlugin extends JavaPlugin implements Listener {

    private NamespacedKey keyTag;
    private final Random random = new Random();

    private final Map<String, String> crateBlocks = new HashMap<>();
    private File blocksFile;
    private YamlConfiguration blocksConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyTag = new NamespacedKey(this, "crate_key");

        blocksFile = new File(getDataFolder(), "blocks.yml");
        if (!blocksFile.exists()) {
            try {
                blocksFile.getParentFile().mkdirs();
                blocksFile.createNewFile();
            } catch (Exception e) {
                getLogger().warning("Не удалось создать blocks.yml: " + e.getMessage());
            }
        }
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        for (String key : blocksConfig.getKeys(false)) {
            crateBlocks.put(key, blocksConfig.getString(key));
        }

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("LimonCrates включён! Кейсов загружено: " + getConfig().getConfigurationSection("crates").getKeys(false).size());
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void saveCrateBlocks() {
        for (Map.Entry<String, String> entry : crateBlocks.entrySet()) {
            blocksConfig.set(entry.getKey(), entry.getValue());
        }
        try {
            blocksConfig.save(blocksFile);
        } catch (Exception e) {
            getLogger().warning("Не удалось сохранить blocks.yml: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        String key = locationKey(block.getLocation());
        String crateId = crateBlocks.get(key);
        if (crateId == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        ConfigurationSection crateSection = getConfig().getConfigurationSection("crates." + crateId);
        if (crateSection == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: кейс '" + crateId + "' не найден в config.yml.");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        String heldCrate = null;
        if (hand != null && hand.hasItemMeta()) {
            heldCrate = hand.getItemMeta().getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
        }

        if (heldCrate == null || !heldCrate.equals(crateId)) {
            player.sendMessage(ChatColor.RED + "Нужен ключ от этого кейса в руке!");
            return;
        }

        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        giveRandomReward(player, crateId, crateSection);
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
        List<String>

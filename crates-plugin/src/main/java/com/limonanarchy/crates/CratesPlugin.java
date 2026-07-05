package com.limonanarchy.crates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
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

        if (args.length >= 2 && args[0].equalsIgnoreCase("setblock")) {
            if (!sender.hasPermission("crates.admin")) {
                player.sendMessage(ChatColor.RED + "У тебя нет прав на эту команду.");
                return true;
            }
            String crateIdForBlock = args[1];
            if (getConfig().getConfigurationSection("crates." + crateIdForBlock) == null) {
                player.sendMessage(ChatColor.RED + "Кейс '" + crateIdForBlock + "' не найден в config.yml.");
                return true;
            }
            Block target = player.getTargetBlockExact(6);
            if (target == null || target.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "Посмотри прямо на блок (не дальше 6 блоков) и повтори команду.");
                return true;
            }
            String blockKey = locationKey(target.getLocation());
            crateBlocks.put(blockKey, crateIdForBlock);
            saveCrateBlocks();
            player.sendMessage(ChatColor.GREEN + "Блок " + target.getType() + " теперь открывает кейс '" + crateIdForBlock + "' при клике с ключом в руке.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("open")) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /crate open <кейс> или /crate setblock <кейс>");
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
        List<?> rawRewards = crateSection.getList("rewards");
        if (rawRewards == null || rawRewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "У этого кейса нет наград (проверь config.yml).");
            return;
        }

        List<java.util.Map<?, ?>> rewardMaps = new ArrayList<>();
        int totalWeight = 0;
        for (Object obj : rawRewards) {
            if (obj instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                rewardMaps.add(map);
                Object w = map.get("weight");
                totalWeight += (w instanceof Number) ? ((Number) w).intValue() : 1;
            }
        }

        if (totalWeight <= 0 || rewardMaps.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Ошибка чтения наград кейса (пустой список).");
            return;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        java.util.Map<?, ?> chosen = rewardMaps.get(0);
        for (java.util.Map<?, ?> map : rewardMaps) {
            Object w = map.get("weight");
            int weight = (w instanceof Number) ? ((Number) w).intValue() : 1;
            cumulative += weight;
            if (roll < cumulative) {
                chosen = map;
                break;
            }
        }

        String rewardName = ChatColor.translateAlternateColorCodes('&', String.valueOf(chosen.getOrDefault("name", "Награда")));
        String type = String.valueOf(chosen.getOrDefault("type", "ITEM"));

        if (type.equalsIgnoreCase("ITEM")) {
            Material mat = Material.matchMaterial(String.valueOf(chosen.getOrDefault("material", "STONE")));
            Object amtObj = chosen.get("amount");
            int amount = (amtObj instanceof Number) ? ((Number) amtObj).intValue() : 1;
            if (mat != null) {
                player.getInventory().addItem(new ItemStack(mat, amount));
            }
        } else if (type.equalsIgnoreCase("COMMAND")) {
            String cmd = String.valueOf(chosen.getOrDefault("command", "")).replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        player.sendMessage(ChatColor.GOLD + "Ты открыл кейс и получил: " + ChatColor.WHITE + rewardName);
        spawnRewardFirework(player.getLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        if (getConfig().getBoolean("broadcast-on-open", true)) {
            String displayName = ChatColor.translateAlternateColorCodes('&', crateSection.getString("display-name", crateId));
            String format = getConfig().getString("broadcast-format", "&e%player% &7открыл &6%crate%");
            format = format.replace("%player%", player.getName())
                    .replace("%crate%", displayName)
                    .replace("%reward%", rewardName);
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', format));
        }
    }

    private void spawnRewardFirework(Location loc) {
        Firework firework = (Firework) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE)
                .withFade(org.bukkit.Color.RED)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build();
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
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

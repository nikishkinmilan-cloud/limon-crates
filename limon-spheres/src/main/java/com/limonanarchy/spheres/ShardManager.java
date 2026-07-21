package com.limonanarchy.spheres;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Осколки сфер - обычные предметы (см. ShardItemFactory), эти методы
 * считают/списывают/выдают их прямо из инвентаря игрока.
 */
public class ShardManager {

    private final ShardItemFactory shardItemFactory;

    public ShardManager(LimonSpheresPlugin plugin) {
        this.shardItemFactory = plugin.getShardItemFactory();
    }

    public int getShards(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (shardItemFactory.isShard(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public void addShards(Player player, int amount) {
        if (amount <= 0) return;
        PlayerInventory inv = player.getInventory();
        ItemStack shardStack = shardItemFactory.create(amount);
        var leftover = inv.addItem(shardStack);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    public boolean takeShards(Player player, int amount) {
        if (getShards(player) < amount) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (!shardItemFactory.isShard(item)) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
        player.updateInventory();
        return true;
    }
}

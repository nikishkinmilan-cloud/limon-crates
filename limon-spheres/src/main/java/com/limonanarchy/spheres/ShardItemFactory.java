package com.limonanarchy.spheres;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

/**
 * Осколок сферы - обычный предмет-валюта, получаемый переплавкой сфер в печи,
 * обменивается у Сфероманта на новые сферы.
 */
public class ShardItemFactory {

    public static final Material MATERIAL = Material.PRISMARINE_SHARD;

    private final NamespacedKey shardKey;

    public ShardItemFactory(LimonSpheresPlugin plugin) {
        this.shardKey = new NamespacedKey(plugin, "is_sphere_shard");
    }

    public ItemStack create(int amount) {
        ItemStack item = new ItemStack(MATERIAL, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Осколок сферы");
        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Получен переплавкой сферы в печи.",
                ChatColor.GRAY + "Обменивается у Сфероманта (/spheromant)."
        );
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(shardKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isShard(ItemStack item) {
        if (item == null || item.getType() != MATERIAL || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(shardKey, PersistentDataType.BYTE);
    }
}

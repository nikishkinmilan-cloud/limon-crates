package com.limonanarchy.spheres.gui;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import com.limonanarchy.spheres.SphereTier;
import com.limonanarchy.spheres.SphereTypeData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI-меню Сфероманта: ряд типов сфер x ряд уровней, оплата осколками (см. config.yml -> spheromant.prices).
 */
public class SpheromantGUI {

    private final LimonSpheresPlugin plugin;
    private final NamespacedKey offerTypeKey;
    private final NamespacedKey offerTierKey;

    public SpheromantGUI(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
        this.offerTypeKey = new NamespacedKey(plugin, "offer_type");
        this.offerTierKey = new NamespacedKey(plugin, "offer_tier");
    }

    public NamespacedKey getOfferTypeKey() {
        return offerTypeKey;
    }

    public NamespacedKey getOfferTierKey() {
        return offerTierKey;
    }

    public String getTitle() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("spheromant.gui-title", "&5&lСферомант"));
    }

    public void open(Player player) {
        Map<String, SphereTypeData> types = plugin.getSphereRegistry().all();
        int rows = Math.min(6, Math.max(3, ((types.size() - 1) / 7 + 1) + 2));
        Inventory inv = plugin.getServer().createInventory(null, rows * 9, getTitle());

        int slot = 0;
        for (SphereTypeData type : types.values()) {
            for (SphereTier tier : new SphereTier[]{SphereTier.NORMAL, SphereTier.EPIC, SphereTier.LEGENDARY}) {
                if (slot >= inv.getSize()) break;
                inv.setItem(slot, buildOffer(type, tier));
                slot++;
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildOffer(SphereTypeData type, SphereTier tier) {
        int price = plugin.getConfig().getInt("spheromant.prices." + tier.name(), 20);

        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(tier.getColor() + type.getDisplay() + " (" + tier.getDisplay() + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Цена: " + ChatColor.LIGHT_PURPLE + price + " осколков");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Нажмите, чтобы купить");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(offerTypeKey, PersistentDataType.STRING, type.getKey());
        meta.getPersistentDataContainer().set(offerTierKey, PersistentDataType.STRING, tier.name());

        item.setItemMeta(meta);
        return item;
    }
}

package com.limonanarchy.spheres.listeners;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import com.limonanarchy.spheres.SphereTier;
import com.limonanarchy.spheres.gui.SpheromantGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SpheromantGUIListener implements Listener {

    private final LimonSpheresPlugin plugin;

    public SpheromantGUIListener(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        SpheromantGUI gui = plugin.getSpheromantGUI();
        if (!event.getView().getTitle().equals(gui.getTitle())) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        var pdc = clicked.getItemMeta().getPersistentDataContainer();
        String type = pdc.get(gui.getOfferTypeKey(), PersistentDataType.STRING);
        String tierName = pdc.get(gui.getOfferTierKey(), PersistentDataType.STRING);
        if (type == null || tierName == null) return;

        SphereTier tier;
        try {
            tier = SphereTier.valueOf(tierName);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int price = plugin.getConfig().getInt("spheromant.prices." + tier.name(), 20);

        if (!plugin.getShardManager().takeShards(player, price)) {
            player.sendMessage(ChatColor.RED + "Недостаточно осколков сфер! Нужно: " + price);
            return;
        }

        ItemStack sphere = plugin.getSphereManager().createSphere(type, tier);
        var leftover = player.getInventory().addItem(sphere);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        player.sendMessage(ChatColor.GREEN + "Вы купили сферу за " + price + " осколков!");
        player.closeInventory();
    }
}

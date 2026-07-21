package com.limonanarchy.spheres.listeners;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import com.limonanarchy.spheres.SphereItemFactory;
import com.limonanarchy.spheres.SphereTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;

/**
 * Переплавка сферы в печи -> осколки сферы (см. spheres.yml/config.yml -> smelting.shards).
 * Также регистрирует техрецепт печи под PLAYER_HEAD (без него печь не примет сферу),
 * и отменяет переплавку обычных (не сфера) голов игроков, чтобы не ломать ванильное поведение.
 */
public class FurnaceSmeltListener implements Listener {

    private final LimonSpheresPlugin plugin;

    public FurnaceSmeltListener(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    public static void registerRecipe(LimonSpheresPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "sphere_smelt_recipe");
        if (Bukkit.getRecipe(key) != null) return;
        // Заглушка результата - реальный результат подставляется в onFurnaceSmelt
        ItemStack placeholderResult = plugin.getShardItemFactory().create(1);
        FurnaceRecipe recipe = new FurnaceRecipe(key, placeholderResult, Material.PLAYER_HEAD, 0f, 200);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        if (source == null || source.getType() != Material.PLAYER_HEAD) return;

        SphereItemFactory factory = plugin.getSphereItemFactory();
        if (!plugin.getConfig().getBoolean("smelting.enabled", true) || !factory.isSphere(source)) {
            // Обычная (не наша) голова игрока - у ванили нет рецепта переплавки головы,
            // отменяем, чтобы техрецепт не переплавлял чужие головы в осколки.
            event.setCancelled(true);
            return;
        }

        SphereTier tier = factory.getTier(source);
        int shards = plugin.getSphereManager().getSmeltShards(tier);
        event.setResult(plugin.getShardItemFactory().create(shards));
    }
}

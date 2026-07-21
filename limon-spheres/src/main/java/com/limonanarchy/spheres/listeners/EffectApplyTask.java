package com.limonanarchy.spheres.listeners;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import com.limonanarchy.spheres.SphereItemFactory;
import com.limonanarchy.spheres.SphereTypeData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Каждые N тиков проверяет офф-руку (40-й слот) каждого онлайн игрока
 * и, если там сфера, накладывает её эффекты (без иконок в инвентаре, ambient/without particles).
 */
public class EffectApplyTask extends BukkitRunnable {

    private static final int DURATION_TICKS = 100; // немного больше интервала, чтобы не мигало

    private final LimonSpheresPlugin plugin;

    public EffectApplyTask(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        SphereItemFactory factory = plugin.getSphereItemFactory();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (!factory.isSphere(offhand)) continue;

            Map<String, Integer> entries = factory.getEntries(offhand);
            for (Map.Entry<String, Integer> e : entries.entrySet()) {
                SphereTypeData typeData = plugin.getSphereRegistry().get(e.getKey());
                if (typeData == null) continue;
                int amplifier = typeData.getAmplifier(e.getValue());
                player.addPotionEffect(new PotionEffect(
                        typeData.getEffectType(), DURATION_TICKS, amplifier, true, false, false));
            }
        }
    }
}

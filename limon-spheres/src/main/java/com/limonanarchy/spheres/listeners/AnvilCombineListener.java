package com.limonanarchy.spheres.listeners;

import com.limonanarchy.spheres.LimonSpheresPlugin;
import com.limonanarchy.spheres.SphereItemFactory;
import com.limonanarchy.spheres.SphereManager;
import com.limonanarchy.spheres.SphereTier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Объединение двух сфер одного типа/уровня в наковальне -> случайная сфера следующего уровня
 * (NORMAL+NORMAL -> EPIC, EPIC+EPIC -> LEGENDARY).
 * Мифик-крафт (EPIC+LEGENDARY разных типов) обрабатывается отдельно тем же слушателем.
 */
public class AnvilCombineListener implements Listener {

    private final LimonSpheresPlugin plugin;

    public AnvilCombineListener(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) return;

        SphereItemFactory factory = plugin.getSphereItemFactory();
        if (!factory.isSphere(left) || !factory.isSphere(right)) return;

        SphereManager manager = plugin.getSphereManager();

        // 1) Обычное улучшение: одинаковый тип и уровень
        ItemStack upgraded = manager.tryAnvilUpgrade(left, right);
        if (upgraded != null) {
            if (!plugin.getConfig().getBoolean("anvil-upgrade.enabled", true)) return;
            event.setResult(upgraded);
            applyXpCost(event, upgradeCostKey(factory.getTier(left)));
            return;
        }

        // 2) Мифик-крафт: EPIC + LEGENDARY
        ItemStack mythic = manager.tryMythicCraft(left, right);
        if (mythic != null) {
            if (!plugin.getConfig().getBoolean("mythic-craft.enabled", true)) return;
            event.setResult(mythic);
            applyXpCost(event, "mythic-craft.xp-cost");
        }
    }

    private String upgradeCostKey(SphereTier fromTier) {
        return fromTier == SphereTier.NORMAL
                ? "anvil-upgrade.xp-cost.NORMAL_TO_EPIC"
                : "anvil-upgrade.xp-cost.EPIC_TO_LEGENDARY";
    }

    private void applyXpCost(PrepareAnvilEvent event, String path) {
        int cost = plugin.getConfig().getInt(path, 20);
        event.getInventory().setRepairCost(cost);
    }
}

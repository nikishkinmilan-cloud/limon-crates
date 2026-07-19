package com.limonanarchy.anticheat.checks;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import com.limonanarchy.anticheat.ViolationManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FastBreak (instamine): чит ломает блоки быстрее, чем позволяет формула ванильной
 * скорости добычи (твёрдость блока, правильный инструмент, Efficiency, Haste/Усталость,
 * нахождение в воде/в воздухе). Считаем ПРИМЕРНОЕ ожидаемое время в тиках по той же
 * логике, что использует ванильный клиент, и если реальное время развалилось намного
 * меньше ожидаемого - копим нарушение. Это эвристика (не точная копия NMS-расчёта),
 * поэтому намеренно даём щедрый запас (leniency) на неточность.
 */
public class FastBreakCheck implements Listener {

    private final AntiCheatPlugin plugin;
    private final ViolationManager violationManager;

    private final Map<UUID, Long> breakStartedAt = new ConcurrentHashMap<>();

    public FastBreakCheck(AntiCheatPlugin plugin, ViolationManager violationManager) {
        this.plugin = plugin;
        this.violationManager = violationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(BlockDamageEvent event) {
        breakStartedAt.putIfAbsent(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("fastbreak.enabled", true)) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Long startedAt = breakStartedAt.remove(id);

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.hasPermission("anticheat.bypass") || ExemptionTracker.isExempt(player)) return;
        if (startedAt == null) return;

        long actualMs = System.currentTimeMillis() - startedAt;
        long expectedMs = expectedBreakTimeMs(player, event.getBlock());

        // Слишком твёрдые/бесконечные блоки (bedrock и т.п.) считаем отдельно не имеет смысла
        if (expectedMs <= 0) return;

        double leniency = plugin.getConfig().getDouble("fastbreak.leniency-multiplier", 0.5);
        long minAcceptableMs = Math.round(expectedMs * leniency);

        if (actualMs < minAcceptableMs) {
            violationManager.flag(player, "FastBreakCheck", plugin.getConfig().getInt("fastbreak.violation-weight", 2));
        }
    }

    /**
     * Грубая аппроксимация ванильной формулы скорости добычи блока:
     * damagePerTick = (correctTool ? 1/(hardness*1.5) : 1/(hardness*5)) * toolMultiplier * hasteMultiplier / fatigueMultiplier
     * с доп. делением на 5, если игрок не на земле, и на 5, если в воде без Aqua Affinity.
     * Время в тиках = ceil(1 / damagePerTick), переводим в мс (1 тик = 50мс).
     */
    private long expectedBreakTimeMs(Player player, Block block) {
        float hardness = block.getType().getHardness();
        if (hardness < 0) return -1; // unbreakable (bedrock и т.п.)
        if (hardness == 0f) return 0; // мгновенно ломающиеся блоки (трава, факел...)

        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean correctTool = block.isPreferredTool(tool);

        double multiplier = 1.0;
        if (correctTool) {
            multiplier = toolTierMultiplier(tool.getType());
            int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (efficiencyLevel > 0) {
                multiplier += (double) (efficiencyLevel * efficiencyLevel) + 1;
            }
        }

        PotionEffect haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste != null) {
            multiplier *= 1.0 + (haste.getAmplifier() + 1) * 0.2;
        }
        PotionEffect fatigue = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (fatigue != null) {
            int amplifier = Math.min(fatigue.getAmplifier() + 1, 4);
            multiplier *= Math.pow(0.3, amplifier);
        }

        double damagePerTick = correctTool ? multiplier / (hardness * 1.5) : multiplier / (hardness * 5.0);

        if (!player.isOnGround()) {
            damagePerTick /= 5.0;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        boolean hasAquaAffinity = tool.getEnchantmentLevel(Enchantment.AQUA_AFFINITY) > 0
                || (helmet != null && helmet.getEnchantmentLevel(Enchantment.AQUA_AFFINITY) > 0);
        if (player.isInWater() && !hasAquaAffinity) {
            damagePerTick /= 5.0;
        }

        if (damagePerTick <= 0) return -1;

        double ticks = Math.ceil(1.0 / damagePerTick);
        return Math.round(ticks * 50.0); // 1 тик = 50мс
    }

    private double toolTierMultiplier(Material toolType) {
        String name = toolType.name();
        if (name.startsWith("NETHERITE_")) return 9.0;
        if (name.startsWith("DIAMOND_")) return 8.0;
        if (name.startsWith("IRON_")) return 6.0;
        if (name.startsWith("STONE_")) return 4.0;
        if (name.startsWith("GOLDEN_")) return 12.0;
        if (name.startsWith("WOODEN_")) return 2.0;
        return 1.0;
    }
}

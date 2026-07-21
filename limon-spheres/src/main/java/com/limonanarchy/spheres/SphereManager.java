package com.limonanarchy.spheres;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Логика улучшения/крафта сфер: наковальня (объединение) и мифик-крафт.
 */
public class SphereManager {

    private final LimonSpheresPlugin plugin;
    private final Random random = new Random();

    public SphereManager(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
    }

    /** Создать сферу базового уровня (NORMAL) заданного типа. */
    public ItemStack createBaseSphere(String type) {
        Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put(type.toUpperCase(), 1);
        return plugin.getSphereItemFactory().create(SphereTier.NORMAL, entries);
    }

    /** Создать сферу произвольного уровня одного типа (NORMAL/EPIC/LEGENDARY). */
    public ItemStack createSphere(String type, SphereTier tier) {
        if (tier == SphereTier.MYTHIC) {
            throw new IllegalArgumentException("Мифическая сфера создаётся через createMythicSphere(type1, type2)");
        }
        Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put(type.toUpperCase(), tier.getEffectLevel());
        return plugin.getSphereItemFactory().create(tier, entries);
    }

    /** Мифическая сфера: level2 от первого типа + level3 от второго типа. */
    public ItemStack createMythicSphere(String type1, String type2) {
        Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put(type1.toUpperCase(), 2);
        entries.put(type2.toUpperCase(), 3);
        return plugin.getSphereItemFactory().create(SphereTier.MYTHIC, entries);
    }

    /**
     * Попытка объединить две сферы ОДНОГО типа и уровня в наковальне.
     * Возвращает результат сферу более высокого уровня, либо null если объединение невозможно
     * (разные типы/уровни или уже MYTHIC).
     */
    public ItemStack tryAnvilUpgrade(ItemStack left, ItemStack right) {
        SphereItemFactory factory = plugin.getSphereItemFactory();
        if (!factory.isSphere(left) || !factory.isSphere(right)) return null;

        SphereTier leftTier = factory.getTier(left);
        SphereTier rightTier = factory.getTier(right);
        if (leftTier == null || leftTier != rightTier) return null;
        if (leftTier == SphereTier.LEGENDARY || leftTier == SphereTier.MYTHIC) return null; // легендарки объединяются только через мифик-крафт с эпиком

        Map<String, Integer> leftEntries = factory.getEntries(left);
        Map<String, Integer> rightEntries = factory.getEntries(right);
        if (!leftEntries.equals(rightEntries)) return null;
        if (leftEntries.size() != 1) return null;

        String type = leftEntries.keySet().iterator().next();
        SphereTier nextTier = leftTier.next();
        return createSphere(type, nextTier);
    }

    /**
     * Мифик-крафт: EPIC сфера одного типа + LEGENDARY сфера другого типа -> MYTHIC.
     * Порядок: epicItem должен быть EPIC, legendaryItem должен быть LEGENDARY.
     */
    public ItemStack tryMythicCraft(ItemStack epicItem, ItemStack legendaryItem) {
        SphereItemFactory factory = plugin.getSphereItemFactory();
        if (!factory.isSphere(epicItem) || !factory.isSphere(legendaryItem)) return null;

        if (factory.getTier(epicItem) != SphereTier.EPIC) return null;
        if (factory.getTier(legendaryItem) != SphereTier.LEGENDARY) return null;

        Map<String, Integer> epicEntries = factory.getEntries(epicItem);
        Map<String, Integer> legendaryEntries = factory.getEntries(legendaryItem);
        if (epicEntries.size() != 1 || legendaryEntries.size() != 1) return null;

        String epicType = epicEntries.keySet().iterator().next();
        String legendaryType = legendaryEntries.keySet().iterator().next();

        return createMythicSphere(epicType, legendaryType);
    }

    /** Случайный тип сферы (для выдач с ивентов/сокровищниц/донат-кейсов). */
    public String randomType() {
        List<String> keys = plugin.getSphereRegistry().getKeys();
        if (keys.isEmpty()) return null;
        return keys.get(random.nextInt(keys.size()));
    }

    /** Случайная сфера заданного уровня (для лут-таблиц ивентов/сокровищниц). */
    public ItemStack randomSphere(SphereTier tier) {
        String type = randomType();
        if (type == null) return null;
        if (tier == SphereTier.MYTHIC) {
            String type2 = randomType();
            int guard = 0;
            while (type2 != null && type2.equals(type) && guard++ < 10) {
                type2 = randomType();
            }
            return createMythicSphere(type, type2);
        }
        return createSphere(type, tier);
    }

    public int getSmeltShards(SphereTier tier) {
        return plugin.getConfig().getInt("smelting.shards." + tier.name(), 1);
    }
}

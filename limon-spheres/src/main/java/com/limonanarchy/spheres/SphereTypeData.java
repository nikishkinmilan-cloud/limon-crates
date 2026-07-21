package com.limonanarchy.spheres;

import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Описание одного типа сферы (Урон, Броня, Скорость и т.д.),
 * загружается из spheres.yml.
 */
public class SphereTypeData {

    private final String key;
    private final String display;
    private final String texture;
    private final PotionEffectType effectType;
    private final Map<Integer, Integer> amplifierByLevel; // level(1-3) -> amplifier

    public SphereTypeData(String key, String display, String texture,
                           PotionEffectType effectType, Map<Integer, Integer> amplifierByLevel) {
        this.key = key;
        this.display = display;
        this.texture = texture;
        this.effectType = effectType;
        this.amplifierByLevel = amplifierByLevel;
    }

    public String getKey() {
        return key;
    }

    public String getDisplay() {
        return display;
    }

    public String getTexture() {
        return texture;
    }

    public PotionEffectType getEffectType() {
        return effectType;
    }

    /**
     * @param level условный уровень эффекта: 1 (Normal), 2 (Epic), 3 (Legendary/Mythic-часть)
     */
    public int getAmplifier(int level) {
        return amplifierByLevel.getOrDefault(level, 0);
    }
}

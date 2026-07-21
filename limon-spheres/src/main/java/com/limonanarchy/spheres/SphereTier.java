package com.limonanarchy.spheres;

import org.bukkit.ChatColor;

/**
 * Уровни редкости сферы, как на HolyWorld:
 * Обычная -> Эпическая -> Легендарная -> Мифическая
 */
public enum SphereTier {

    NORMAL("Обычная", ChatColor.WHITE, 1),
    EPIC("Эпическая", ChatColor.LIGHT_PURPLE, 2),
    LEGENDARY("Легендарная", ChatColor.GOLD, 3),
    MYTHIC("Мифическая", ChatColor.RED, 4);

    private final String display;
    private final ChatColor color;
    private final int effectLevel;

    SphereTier(String display, ChatColor color, int effectLevel) {
        this.display = display;
        this.color = color;
        this.effectLevel = effectLevel;
    }

    public String getDisplay() {
        return display;
    }

    public ChatColor getColor() {
        return color;
    }

    /**
     * Условный уровень эффекта (1-3), используется для NORMAL/EPIC/LEGENDARY.
     * У MYTHIC своя логика (два типа сразу), effectLevel не используется напрямую.
     */
    public int getEffectLevel() {
        return effectLevel;
    }

    public SphereTier next() {
        return switch (this) {
            case NORMAL -> EPIC;
            case EPIC -> LEGENDARY;
            case LEGENDARY, MYTHIC -> MYTHIC;
        };
    }

    public String colored() {
        return color + display + ChatColor.RESET;
    }
}

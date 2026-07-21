package com.limonanarchy.spheres;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранит все типы сфер (DAMAGE, ARMOR, SPEED...), загруженные из spheres.yml.
 */
public class SphereRegistry {

    private final Map<String, SphereTypeData> types = new LinkedHashMap<>();

    public void load(File dataFolder) {
        types.clear();
        File file = new File(dataFolder, "spheres.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection typesSection = yaml.getConfigurationSection("types");
        if (typesSection == null) {
            return;
        }

        for (String key : typesSection.getKeys(false)) {
            ConfigurationSection section = typesSection.getConfigurationSection(key);
            if (section == null) continue;

            String display = section.getString("display", key);
            String texture = section.getString("texture", "");
            String effectName = section.getString("effect", "SPEED");
            PotionEffectType effectType = PotionEffectType.getByName(effectName);
            if (effectType == null) {
                effectType = PotionEffectType.SPEED;
            }

            Map<Integer, Integer> levels = new LinkedHashMap<>();
            ConfigurationSection levelsSection = section.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelKey : levelsSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        int amplifier = levelsSection.getInt(levelKey);
                        levels.put(level, amplifier);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            types.put(key.toUpperCase(), new SphereTypeData(key.toUpperCase(), display, texture, effectType, levels));
        }
    }

    public SphereTypeData get(String key) {
        if (key == null) return null;
        return types.get(key.toUpperCase());
    }

    public boolean exists(String key) {
        return get(key) != null;
    }

    public List<String> getKeys() {
        return List.copyOf(types.keySet());
    }

    public Map<String, SphereTypeData> all() {
        return types;
    }
}

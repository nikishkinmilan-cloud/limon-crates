package com.limonanarchy.spheres;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.Bukkit;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Создаёт ItemStack сферы (голова игрока) с нужным лором и метаданными.
 * Данные сферы хранятся в PersistentDataContainer:
 *  - "tier"    -> имя SphereTier
 *  - "entries" -> строка вида "DAMAGE:2;ARMOR:3" (тип:условный_уровень)
 */
public class SphereItemFactory {

    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"(.*?)\"");

    private final LimonSpheresPlugin plugin;
    private final NamespacedKey tierKey;
    private final NamespacedKey entriesKey;
    private final NamespacedKey markerKey;

    public SphereItemFactory(LimonSpheresPlugin plugin) {
        this.plugin = plugin;
        this.tierKey = new NamespacedKey(plugin, "sphere_tier");
        this.entriesKey = new NamespacedKey(plugin, "sphere_entries");
        this.markerKey = new NamespacedKey(plugin, "is_sphere");
    }

    /** entries: список пар "TYPE:level", для NORMAL/EPIC/LEGENDARY обычно одна пара, для MYTHIC - две. */
    public ItemStack create(SphereTier tier, Map<String, Integer> entries) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        StringBuilder nameParts = new StringBuilder();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Сфера " + tier.colored());
        lore.add("");

        String texture = null;
        for (Map.Entry<String, Integer> e : entries.entrySet()) {
            SphereTypeData typeData = plugin.getSphereRegistry().get(e.getKey());
            if (typeData == null) continue;
            if (texture == null) texture = typeData.getTexture();

            int amplifier = typeData.getAmplifier(e.getValue());
            lore.add(ChatColor.YELLOW + typeData.getDisplay() + " " + toRoman(e.getValue())
                    + ChatColor.DARK_GRAY + " (" + typeData.getEffectType().getName() + " " + (amplifier + 1) + ")");

            if (nameParts.length() > 0) nameParts.append(ChatColor.GRAY).append(" + ").append(ChatColor.RESET);
            nameParts.append(typeData.getDisplay());
        }

        meta.setDisplayName(tier.getColor() + "Сфера: " + ChatColor.RESET + nameParts);
        meta.setLore(lore);

        applyTexture(meta, texture);

        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(entriesKey, PersistentDataType.STRING, serializeEntries(entries));

        item.setItemMeta(meta);
        return item;
    }

    public boolean isSphere(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public SphereTier getTier(ItemStack item) {
        if (!isSphere(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return SphereTier.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Map<String, Integer> getEntries(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!isSphere(item)) return result;
        String raw = item.getItemMeta().getPersistentDataContainer().get(entriesKey, PersistentDataType.STRING);
        return deserializeEntries(raw);
    }

    private String serializeEntries(Map<String, Integer> entries) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : entries.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    private Map<String, Integer> deserializeEntries(String raw) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            try {
                result.put(kv[0], Integer.parseInt(kv[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private void applyTexture(SkullMeta meta, String base64Texture) {
        if (base64Texture == null || base64Texture.isBlank()) return;
        try {
            String decoded = new String(Base64.getDecoder().decode(base64Texture), StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(decoded);
            if (!matcher.find()) return;
            String url = matcher.group(1);

            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (IllegalArgumentException | MalformedURLException ex) {
            // Некорректная/placeholder-текстура из spheres.yml - используем стандартную голову без скина.
            plugin.getLogger().warning("Не удалось применить текстуру сферы, используется голова по умолчанию: " + ex.getMessage());
        }
    }

    private String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(level);
        };
    }
}

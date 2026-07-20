package com.limonanarchy.anticheat.review;

import com.limonanarchy.anticheat.AntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Комната проверки" для /acprov: изолируем подозреваемого от сервера, телепортируем
 * его и проверяющего в специально подготовленную камеру (стафф строит её сам в игровом
 * мире и один раз настраивает командой /acprov setroom), показываем табличку
 * "проверка через AnyDesk" и запоминаем, кто именно кого проверяет - это нужно,
 * чтобы весь чат подозреваемого шёл лично проверяющему (см. ReviewChatListener).
 */
public class ReviewManager {

    private final AntiCheatPlugin plugin;

    // подозреваемый -> кто его проверяет
    private final Map<UUID, UUID> activeReviews = new ConcurrentHashMap<>();
    // подозреваемый -> где он был до вызова в камеру (чтобы вернуть обратно после проверки)
    private final Map<UUID, Location> previousLocation = new ConcurrentHashMap<>();

    public ReviewManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isUnderReview(UUID suspectId) {
        return activeReviews.containsKey(suspectId);
    }

    public Player getReviewer(UUID suspectId) {
        UUID reviewerId = activeReviews.get(suspectId);
        return reviewerId == null ? null : Bukkit.getPlayer(reviewerId);
    }

    public boolean isRoomConfigured() {
        return getSavedLocation("target") != null && getSavedLocation("reviewer") != null;
    }

    /**
     * Сохраняет текущую позицию как одну из двух точек камеры (target/reviewer)
     * прямо в config.yml, чтобы настройка не терялась при перезапуске сервера.
     */
    public void saveRoomSpot(String key, Location location) {
        String path = "review.chamber." + key;
        plugin.getConfig().set(path + ".world", location.getWorld().getName());
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getSavedLocation(String key) {
        String path = "review.chamber." + key;
        String worldName = plugin.getConfig().getString(path + ".world", "");
        if (worldName == null || worldName.isEmpty()) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getConfig().getDouble(path + ".yaw");
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Запускает проверку: телепортирует подозреваемого и проверяющего в камеру,
     * показывает title и шлёт подозреваемому сообщение с просьбой написать AnyDesk.
     * Возвращает false, если камера ещё не настроена через /acprov setroom.
     */
    public boolean startReview(Player suspect, Player reviewer) {
        Location suspectSpot = getSavedLocation("target");
        Location reviewerSpot = getSavedLocation("reviewer");
        if (suspectSpot == null || reviewerSpot == null) {
            return false;
        }

        previousLocation.put(suspect.getUniqueId(), suspect.getLocation());
        activeReviews.put(suspect.getUniqueId(), reviewer.getUniqueId());

        suspect.teleport(suspectSpot);
        reviewer.teleport(reviewerSpot);

        String title = plugin.getConfig().getString("review.title", "&c&lПРОВЕРКА").replace('&', '§');
        String subtitle = plugin.getConfig().getString("review.subtitle", "&7через AnyDesk").replace('&', '§');
        int fadeIn = plugin.getConfig().getInt("review.title-fade-in-ticks", 10);
        int stay = plugin.getConfig().getInt("review.title-stay-ticks", 100);
        int fadeOut = plugin.getConfig().getInt("review.title-fade-out-ticks", 20);

        suspect.sendTitle(title, subtitle, fadeIn, stay, fadeOut);

        String chatPrompt = plugin.getConfig().getString("review.chat-prompt-message",
                "&eДля проверки на читы напишите свой AnyDesk ID в чат.").replace('&', '§');
        suspect.sendMessage(chatPrompt);

        reviewer.sendMessage("§aНачал проверку §f" + suspect.getName()
                + "§a. Весь его чат теперь приходит тебе в личку. §7/acprov " + suspect.getName()
                + " off §aчтобы закончить.");

        return true;
    }

    /**
     * Завершает проверку и возвращает подозреваемого туда, где он был до вызова в камеру.
     */
    public void endReview(Player suspect, Player reviewer) {
        activeReviews.remove(suspect.getUniqueId());
        Location back = previousLocation.remove(suspect.getUniqueId());

        if (back != null) {
            suspect.teleport(back);
        }

        suspect.sendMessage("§aПроверка окончена.");
        if (reviewer != null) {
            reviewer.sendMessage("§aПроверка §f" + suspect.getName() + " §aзавершена.");
        }
    }

    /**
     * На случай если проверяющий вышел с сервера, не закрыв проверку - подозреваемого
     * не оставляем висеть в камере навсегда.
     */
    public void handleReviewerQuit(UUID reviewerId) {
        activeReviews.entrySet().removeIf(entry -> {
            if (!entry.getValue().equals(reviewerId)) return false;

            Player suspect = Bukkit.getPlayer(entry.getKey());
            Location back = previousLocation.remove(entry.getKey());
            if (suspect != null) {
                if (back != null) suspect.teleport(back);
                suspect.sendMessage("§eПроверка прервана (проверяющий вышел с сервера).");
            }
            return true;
        });
    }

    /**
     * Чистит состояние, если сам подозреваемый вышел с сервера во время проверки.
     */
    public void clearSuspect(UUID suspectId) {
        activeReviews.remove(suspectId);
        previousLocation.remove(suspectId);
    }
}

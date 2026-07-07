package com.limonanarchy.anticheat;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Мягкая интеграция с ViaVersion БЕЗ жёсткой зависимости в pom.xml.
 * Если ViaVersion не установлен - весь класс просто тихо возвращает "обычный клиент"
 * и ничего не ломается. Если установлен - через reflection узнаём протокол-версию
 * игрока и можем определить, что он зашёл со старой версии (1.16.5-1.20), где
 * физика движения/комбата немного отличается от актуальной версии сервера.
 */
public class ClientVersionUtil {

    private static Boolean viaVersionPresent = null;
    private static Object viaApiInstance = null;
    private static Method getPlayerVersionMethod = null;

    private ClientVersionUtil() {
    }

    private static void init() {
        if (viaVersionPresent != null) return;

        try {
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
            Method getApiMethod = viaClass.getMethod("getAPI");
            viaApiInstance = getApiMethod.invoke(null);
            getPlayerVersionMethod = viaApiInstance.getClass().getMethod("getPlayerVersion", java.util.UUID.class);
            viaVersionPresent = true;
        } catch (Throwable t) {
            // ViaVersion не установлен или другая версия API - просто отключаем интеграцию
            viaVersionPresent = false;
        }
    }

    /**
     * Возвращает protocol version игрока (например 767 = 1.21), либо -1 если
     * ViaVersion не установлен / не удалось определить (тогда считаем что клиент обычный).
     */
    public static int getProtocolVersion(Player player) {
        init();
        if (!viaVersionPresent) return -1;

        try {
            Object result = getPlayerVersionMethod.invoke(viaApiInstance, player.getUniqueId());
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
            // если что-то пошло не так - просто не считаем клиента "старым"
        }
        return -1;
    }

    /**
     * Является ли клиент "старым" относительно порога, заданного в конфиге.
     * Если ViaVersion не установлен - всегда false (нет смысла в смягчении, все на одной версии).
     */
    public static boolean isLegacyClient(Player player, int legacyProtocolThreshold) {
        int version = getProtocolVersion(player);
        return version != -1 && version < legacyProtocolThreshold;
    }
}

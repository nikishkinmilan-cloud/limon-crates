package com.limonanarchy.anticheat.bans;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanLoginListener implements Listener {

    private final BanManager banManager;

    public BanLoginListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        BanEntry ban = banManager.getBan(event.getName());
        if (ban == null) return;

        event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                banManager.buildKickScreen(ban)
        );
    }
}

package com.limonanarchy.anticheat.checks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.TimeUnit;

public class ExemptionListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Нокбек после урона может дать резкий скачок скорости/высоты - это нормально
            ExemptionTracker.exempt(player, TimeUnit.MILLISECONDS.toMillis(800));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // Телепорт (эндер-жемчуг, /tpa, RTP, варпы) даёт мгновенный скачок координат
        ExemptionTracker.exempt(event.getPlayer(), TimeUnit.SECONDS.toMillis(1));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        ExemptionTracker.exempt(event.getPlayer(), TimeUnit.SECONDS.toMillis(1));
    }
}

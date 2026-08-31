package net.duelarena.listener;

import net.duelarena.duel.DuelManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DuelPlayerListener implements Listener {

    private final DuelManager duelManager;

    public DuelPlayerListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        duelManager.handleDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        duelManager.handleQuit(event.getPlayer());
    }
}

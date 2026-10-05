package net.duelarena.listener;

import net.duelarena.arena.ArenaManager;
import net.duelarena.duel.DuelManager;
import net.duelarena.util.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DuelPlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;
    private final DuelManager duelManager;
    private final MessageManager messages;

    public DuelPlayerListener(JavaPlugin plugin, ArenaManager arenaManager,
                              DuelManager duelManager, MessageManager messages) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.duelManager = duelManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        duelManager.handleDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        duelManager.handleQuit(event.getPlayer());
    }

    /**
     * 紫頌果傳送限制(仍會回復飽食度):
     * 1. 決鬥中(含贏家整理期間)的玩家不會傳送。
     * 2. 傳送起點或終點在任何場地範圍內也一律取消,避免場外的人傳進場地。
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChorusTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        if (duelManager.isInDuel(event.getPlayer().getUniqueId())
                || arenaManager.findArenaAt(event.getFrom()) != null
                || (event.getTo() != null && arenaManager.findArenaAt(event.getTo()) != null)) {
            event.setCancelled(true);
        }
    }

    /** 決鬥中(含贏家整理期間)禁止使用 config.yml 黑名單內的指令。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!duelManager.isInDuel(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("duelarena.admin.bypass")) {
            return;
        }
        String typed = normalizeCommand(event.getMessage());
        if (typed.isEmpty()) {
            return;
        }
        for (String entry : plugin.getConfig().getStringList("settings.blocked-commands")) {
            String blocked = normalizeCommand(entry);
            if (blocked.isEmpty()) {
                continue;
            }
            if (typed.equals(blocked) || typed.startsWith(blocked + " ")) {
                event.setCancelled(true);
                messages.send(player, "duel.command-blocked");
                return;
            }
        }
    }

    /** 去掉開頭的 /、轉小寫、合併空白,並移除第一個字的前綴(例如 minecraft:tp -> tp)。 */
    private String normalizeCommand(String raw) {
        String line = raw.trim();
        while (line.startsWith("/")) {
            line = line.substring(1);
        }
        line = line.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (line.isEmpty()) {
            return line;
        }
        int space = line.indexOf(' ');
        String label = space < 0 ? line : line.substring(0, space);
        String rest = space < 0 ? "" : line.substring(space);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        return label + rest;
    }
}

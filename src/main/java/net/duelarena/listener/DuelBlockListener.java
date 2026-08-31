package net.duelarena.listener;

import net.duelarena.arena.Arena;
import net.duelarena.arena.ArenaManager;
import net.duelarena.duel.Duel;
import net.duelarena.duel.DuelManager;
import net.duelarena.util.LocationUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class DuelBlockListener implements Listener {

    private final ArenaManager arenaManager;
    private final DuelManager duelManager;

    public DuelBlockListener(ArenaManager arenaManager, DuelManager duelManager) {
        this.arenaManager = arenaManager;
        this.duelManager = duelManager;
    }

    private boolean bypass(Player p) {
        return p.hasPermission("duelarena.admin.bypass");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (bypass(player)) return;

        Arena arena = arenaManager.findArenaAt(event.getBlock().getLocation());
        if (arena == null) return;

        Duel duel = duelManager.getDuelByArena(arena);
        boolean allowed = duel != null && duel.canBuild(player.getUniqueId());

        if (!allowed) {
            event.setCancelled(true);
            player.sendMessage("§c這個場地目前不允許放置方塊。");
            return;
        }

        long key = LocationUtil.packBlockKey(event.getBlock());
        duelManager.trackPlacedBlock(duel, key);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (bypass(player)) return;

        Arena arena = arenaManager.findArenaAt(event.getBlock().getLocation());
        if (arena == null) return;

        Duel duel = duelManager.getDuelByArena(arena);
        long key = LocationUtil.packBlockKey(event.getBlock());

        boolean isPlacedBlock = duel != null && duel.getPlacedBlocks().contains(key);
        boolean allowedToBreak = duel != null && duel.canBuild(player.getUniqueId()) && isPlacedBlock;

        if (!allowedToBreak) {
            event.setCancelled(true);
            player.sendMessage("§c這個場地的地板/牆壁無法破壞。");
            return;
        }

        duelManager.untrackPlacedBlock(duel, key);
    }
}

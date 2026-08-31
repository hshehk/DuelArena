package net.duelarena.listener;

import net.duelarena.arena.Arena;
import net.duelarena.arena.ArenaManager;
import net.duelarena.duel.Duel;
import net.duelarena.duel.DuelManager;
import net.duelarena.util.LocationUtil;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;

public class DuelExplosionListener implements Listener {

    private final ArenaManager arenaManager;
    private final DuelManager duelManager;

    public DuelExplosionListener(ArenaManager arenaManager, DuelManager duelManager) {
        this.arenaManager = arenaManager;
        this.duelManager = duelManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        filterBlockList(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        filterBlockList(event.blockList());
    }

    /**
     * 移除爆炸清單中屬於場地「原本地板/牆壁」的方塊,只留下決鬥中玩家自己放置、且該場地
     * 目前允許破壞的方塊,這樣爆炸就不會傷到地板牆壁本體。
     */
    private void filterBlockList(java.util.List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Arena arena = arenaManager.findArenaAt(block.getLocation());
            if (arena == null) {
                continue; // 場地外,不管
            }
            Duel duel = duelManager.getDuelByArena(arena);
            long key = LocationUtil.packBlockKey(block);
            boolean destroyable = duel != null && duel.getPlacedBlocks().contains(key);
            if (!destroyable) {
                it.remove();
            } else {
                duelManager.untrackPlacedBlock(duel, key);
            }
        }
    }
}

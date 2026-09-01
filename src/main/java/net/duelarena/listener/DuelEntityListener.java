package net.duelarena.listener;

import net.duelarena.arena.Arena;
import net.duelarena.arena.ArenaManager;
import net.duelarena.arena.ArenaType;
import net.duelarena.util.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 打刀場(BLADE)禁止放置/召喚:水晶、重生錨、TNT 礦車。
 * 爆炸場(EXPLOSIVE)不受此限制。
 */
public class DuelEntityListener implements Listener {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;
    private final MessageManager messages;

    public DuelEntityListener(JavaPlugin plugin, ArenaManager arenaManager, MessageManager messages) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    private Set<Material> bannedBlocks() {
        List<String> names = plugin.getConfig().getStringList("settings.blade-banned-blocks");
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String n : names) {
            Material m = Material.matchMaterial(n);
            if (m != null) set.add(m);
        }
        return set;
    }

    private Set<EntityType> bannedEntities() {
        List<String> names = plugin.getConfig().getStringList("settings.blade-banned-entities");
        Set<EntityType> set = EnumSet.noneOf(EntityType.class);
        for (String n : names) {
            try {
                set.add(EntityType.valueOf(n));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("設定檔中未知的 EntityType: " + n);
            }
        }
        return set;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Arena arena = arenaManager.findArenaAt(event.getBlock().getLocation());
        if (arena == null || arena.getType() != ArenaType.BLADE) {
            return;
        }
        if (bannedBlocks().contains(event.getBlock().getType())) {
            event.setCancelled(true);
            Player p = event.getPlayer();
            messages.send(p, "entity.blade-block-denied");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        Arena arena = arenaManager.findArenaAt(event.getEntity().getLocation());
        if (arena == null || arena.getType() != ArenaType.BLADE) {
            return;
        }
        if (bannedEntities().contains(event.getEntity().getType())) {
            event.setCancelled(true);
            if (event.getPlayer() != null) {
                messages.send(event.getPlayer(), "entity.blade-entity-denied");
            }
        }
    }
}

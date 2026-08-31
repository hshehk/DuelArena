package net.duelarena.duel;

import net.duelarena.arena.Arena;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class DuelInvite {

    private final UUID from;
    private final UUID target;
    private final Arena arena;
    private BukkitTask timeoutTask;

    public DuelInvite(UUID from, UUID target, Arena arena) {
        this.from = from;
        this.target = target;
        this.arena = arena;
    }

    public UUID getFrom() {
        return from;
    }

    public UUID getTarget() {
        return target;
    }

    public Arena getArena() {
        return arena;
    }

    public BukkitTask getTimeoutTask() {
        return timeoutTask;
    }

    public void setTimeoutTask(BukkitTask timeoutTask) {
        this.timeoutTask = timeoutTask;
    }
}

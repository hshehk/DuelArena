package net.duelarena.duel;

import net.duelarena.arena.Arena;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 代表一場正在進行(或整理中)的決鬥。
 */
public class Duel {

    private final Arena arena;
    private final UUID player1;
    private final UUID player2;

    private final Location returnLoc1;
    private final Location returnLoc2;

    private DuelState state = DuelState.ACTIVE;

    private UUID winner;
    private UUID loser;

    /** 決鬥期間(含整理階段)玩家自己放置、目前還沒被回收的方塊座標。 */
    private final Set<Long> placedBlocks = new HashSet<>();

    private BukkitTask cleanupTask;

    public Duel(Arena arena, Player p1, Player p2, Location returnLoc1, Location returnLoc2) {
        this.arena = arena;
        this.player1 = p1.getUniqueId();
        this.player2 = p2.getUniqueId();
        this.returnLoc1 = returnLoc1;
        this.returnLoc2 = returnLoc2;
    }

    public Arena getArena() {
        return arena;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public boolean isParticipant(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1.equals(uuid)) return player2;
        if (player2.equals(uuid)) return player1;
        return null;
    }

    public Location getReturnLoc(UUID uuid) {
        if (player1.equals(uuid)) return returnLoc1;
        if (player2.equals(uuid)) return returnLoc2;
        return null;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public UUID getWinner() {
        return winner;
    }

    public void setWinner(UUID winner) {
        this.winner = winner;
    }

    public UUID getLoser() {
        return loser;
    }

    public void setLoser(UUID loser) {
        this.loser = loser;
    }

    public Set<Long> getPlacedBlocks() {
        return placedBlocks;
    }

    public BukkitTask getCleanupTask() {
        return cleanupTask;
    }

    public void setCleanupTask(BukkitTask cleanupTask) {
        this.cleanupTask = cleanupTask;
    }

    /** 這場決鬥目前是否還允許此玩家在場地內放置/破壞方塊。 */
    public boolean canBuild(UUID uuid) {
        if (state == DuelState.ACTIVE) {
            return isParticipant(uuid);
        }
        // CLEANUP 階段只有贏家能繼續打掉自己放的方塊回收
        return uuid.equals(winner);
    }
}

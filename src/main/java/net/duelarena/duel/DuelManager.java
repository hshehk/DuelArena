package net.duelarena.duel;

import net.duelarena.arena.Arena;
import net.duelarena.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelManager {

    private final JavaPlugin plugin;
    private final MessageManager messages;

    /** target uuid -> 邀請 */
    private final Map<UUID, DuelInvite> invites = new HashMap<>();
    /** 場地名稱(小寫) -> 進行中的決鬥 */
    private final Map<String, Duel> activeByArena = new HashMap<>();
    /** 玩家 uuid -> 決鬥(雙方在 ACTIVE 階段都有,CLEANUP 階段只有贏家有) */
    private final Map<UUID, Duel> byPlayer = new HashMap<>();

    public DuelManager(JavaPlugin plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    private int cleanupSeconds() {
        return plugin.getConfig().getInt("settings.cleanup-seconds", 60);
    }

    private int inviteTimeoutSeconds() {
        return plugin.getConfig().getInt("settings.invite-timeout-seconds", 30);
    }

    private String typeName(net.duelarena.arena.ArenaType type) {
        return type == net.duelarena.arena.ArenaType.EXPLOSIVE
                ? messages.get("type.explosive")
                : messages.get("type.blade");
    }

    // ---------------- 邀請流程 ----------------

    public boolean isArenaBusy(Arena arena) {
        return activeByArena.containsKey(arena.getName().toLowerCase());
    }

    public boolean isInDuel(UUID uuid) {
        return byPlayer.containsKey(uuid);
    }

    public DuelInvite getInvite(UUID target) {
        return invites.get(target);
    }

    public String sendInvite(Player from, Player target, Arena arena) {
        if (from.getUniqueId().equals(target.getUniqueId())) {
            return messages.get("duel.cannot-invite-self");
        }
        if (!arena.isFullyConfigured()) {
            return messages.get("duel.arena-not-configured", "arena", arena.getName());
        }
        if (isArenaBusy(arena)) {
            return messages.get("duel.arena-busy", "arena", arena.getName());
        }
        if (isInDuel(from.getUniqueId())) {
            return messages.get("duel.self-in-duel");
        }
        if (isInDuel(target.getUniqueId())) {
            return messages.get("duel.target-in-duel", "target", target.getName());
        }
        if (invites.containsKey(target.getUniqueId())) {
            return messages.get("duel.target-has-pending-invite", "target", target.getName());
        }

        DuelInvite invite = new DuelInvite(from.getUniqueId(), target.getUniqueId(), arena);
        invites.put(target.getUniqueId(), invite);

        int timeout = inviteTimeoutSeconds();
        invite.setTimeoutTask(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelInvite cur = invites.get(target.getUniqueId());
            if (cur == invite) {
                invites.remove(target.getUniqueId());
                Player f = Bukkit.getPlayer(from.getUniqueId());
                Player t = Bukkit.getPlayer(target.getUniqueId());
                if (f != null) messages.send(f, "duel.invite-timeout-sender", "target", target.getName());
                if (t != null) messages.send(t, "duel.invite-timeout-target", "sender", from.getName());
            }
        }, timeout * 20L));

        messages.send(target, "duel.invite-received",
                "sender", from.getName(), "arena", arena.getName(), "type", typeName(arena.getType()));
        messages.send(from, "duel.invite-sent", "target", target.getName());
        return null;
    }

    public String cancelInvite(Player from) {
        for (Map.Entry<UUID, DuelInvite> e : invites.entrySet()) {
            if (e.getValue().getFrom().equals(from.getUniqueId())) {
                invites.remove(e.getKey());
                e.getValue().getTimeoutTask().cancel();
                Player target = Bukkit.getPlayer(e.getKey());
                if (target != null) {
                    messages.send(target, "duel.invite-cancelled-target", "sender", from.getName());
                }
                return null;
            }
        }
        return messages.get("duel.no-pending-invite-sent");
    }

    public String denyInvite(Player target) {
        DuelInvite invite = invites.remove(target.getUniqueId());
        if (invite == null) {
            return messages.get("duel.no-pending-invite-received");
        }
        invite.getTimeoutTask().cancel();
        Player from = Bukkit.getPlayer(invite.getFrom());
        if (from != null) {
            messages.send(from, "duel.invite-denied-sender", "target", target.getName());
        }
        messages.send(target, "duel.invite-denied-self");
        return null;
    }

    public String acceptInvite(Player target) {
        DuelInvite invite = invites.remove(target.getUniqueId());
        if (invite == null) {
            return messages.get("duel.no-pending-invite-received");
        }
        invite.getTimeoutTask().cancel();

        Player from = Bukkit.getPlayer(invite.getFrom());
        if (from == null || !from.isOnline()) {
            return messages.get("duel.inviter-offline");
        }
        Arena arena = invite.getArena();
        if (isArenaBusy(arena)) {
            return messages.get("duel.arena-taken");
        }
        if (isInDuel(from.getUniqueId()) || isInDuel(target.getUniqueId())) {
            return messages.get("duel.already-in-duel");
        }

        startDuel(from, target, arena);
        return null;
    }

    // ---------------- 決鬥流程 ----------------

    private void startDuel(Player p1, Player p2, Arena arena) {
        Duel duel = new Duel(arena, p1, p2, p1.getLocation().clone(), p2.getLocation().clone());
        activeByArena.put(arena.getName().toLowerCase(), duel);
        byPlayer.put(p1.getUniqueId(), duel);
        byPlayer.put(p2.getUniqueId(), duel);

        resetForFight(p1);
        resetForFight(p2);

        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        String typeName = typeName(arena.getType());
        messages.send(p1, "duel.start", "opponent", p2.getName(), "type", typeName);
        messages.send(p2, "duel.start", "opponent", p1.getName(), "type", typeName);
    }

    private void resetForFight(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) {
            // 不強制動旁觀/創造模式玩家的模式,避免干擾伺服器其他設計
        }
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
    }

    /** 追蹤某個場地座標(long key)是決鬥中玩家放置的方塊。 */
    public void trackPlacedBlock(Duel duel, long key) {
        duel.getPlacedBlocks().add(key);
    }

    public void untrackPlacedBlock(Duel duel, long key) {
        duel.getPlacedBlocks().remove(key);
    }

    public Duel getDuel(UUID uuid) {
        return byPlayer.get(uuid);
    }

    public Duel getDuelByArena(Arena arena) {
        return activeByArena.get(arena.getName().toLowerCase());
    }

    /** 有玩家死亡,判定對手獲勝,進入整理階段。 */
    public void handleDeath(Player victim) {
        Duel duel = byPlayer.get(victim.getUniqueId());
        if (duel == null || duel.getState() != DuelState.ACTIVE) {
            return;
        }
        UUID winnerUuid = duel.getOpponent(victim.getUniqueId());
        endFight(duel, winnerUuid, victim.getUniqueId(), false);
    }

    /** 玩家離線視同棄權。 */
    public void handleQuit(Player p) {
        UUID uuid = p.getUniqueId();

        // 若有送出中的邀請,取消
        invites.entrySet().removeIf(e -> {
            if (e.getValue().getFrom().equals(uuid)) {
                e.getValue().getTimeoutTask().cancel();
                return true;
            }
            return false;
        });
        DuelInvite receivedInvite = invites.remove(uuid);
        if (receivedInvite != null) {
            receivedInvite.getTimeoutTask().cancel();
        }

        Duel duel = byPlayer.get(uuid);
        if (duel == null) {
            return;
        }
        if (duel.getState() == DuelState.ACTIVE) {
            UUID winnerUuid = duel.getOpponent(uuid);
            endFight(duel, winnerUuid, uuid, true);
        } else if (uuid.equals(duel.getWinner())) {
            // 贏家在整理階段離線,直接結束整理
            finishCleanup(duel, true);
        }
    }

    private void endFight(Duel duel, UUID winnerUuid, UUID loserUuid, boolean loserOffline) {
        duel.setState(DuelState.CLEANUP);
        duel.setWinner(winnerUuid);
        duel.setLoser(loserUuid);

        byPlayer.remove(loserUuid);

        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);

        if (!loserOffline && loser != null) {
            Location back = duel.getReturnLoc(loserUuid);
            if (back != null) {
                loser.teleport(back);
            }
            messages.send(loser, "duel.lost");
        }

        int seconds = cleanupSeconds();
        if (winner != null) {
            messages.send(winner, "duel.won", "seconds", seconds);
        }

        duel.setCleanupTask(Bukkit.getScheduler().runTaskLater(plugin,
                () -> finishCleanup(duel, false), seconds * 20L));
    }

    /** 贏家主動要求提早結束整理時間。 */
    public String requestLeave(Player winner) {
        Duel duel = byPlayer.get(winner.getUniqueId());
        if (duel == null || duel.getState() != DuelState.CLEANUP || !winner.getUniqueId().equals(duel.getWinner())) {
            return messages.get("duel.no-cleanup-pending");
        }
        finishCleanup(duel, false);
        return null;
    }

    private void finishCleanup(Duel duel, boolean winnerAlreadyOffline) {
        if (duel.getCleanupTask() != null) {
            duel.getCleanupTask().cancel();
        }

        Arena arena = duel.getArena();

        // 清掉場上沒被回收的方塊(直接消失,不掉落物品)
        if (arena.getWorld() != null) {
            org.bukkit.World world = Bukkit.getWorld(arena.getWorld());
            if (world != null) {
                Integer[] p1 = arena.getPos1();
                Integer[] p2 = arena.getPos2();
                if (p1[0] != null && p2[0] != null) {
                    int minX = Math.min(p1[0], p2[0]), maxX = Math.max(p1[0], p2[0]);
                    int minY = Math.min(p1[1], p2[1]), maxY = Math.max(p1[1], p2[1]);
                    int minZ = Math.min(p1[2], p2[2]), maxZ = Math.max(p1[2], p2[2]);

                    for (long key : duel.getPlacedBlocks()) {
                        int[] xyz = unpack(key);
                        if (xyz[0] >= minX && xyz[0] <= maxX
                                && xyz[1] >= minY && xyz[1] <= maxY
                                && xyz[2] >= minZ && xyz[2] <= maxZ) {
                            world.getBlockAt(xyz[0], xyz[1], xyz[2]).setType(org.bukkit.Material.AIR);
                        }
                    }

                    // 清掉場上殘留的掉落物
                    for (Item item : world.getEntitiesByClass(Item.class)) {
                        if (arena.contains(item.getLocation())) {
                            item.remove();
                        }
                    }
                }
            }
        }
        duel.getPlacedBlocks().clear();

        activeByArena.remove(arena.getName().toLowerCase());
        byPlayer.remove(duel.getWinner());

        if (!winnerAlreadyOffline) {
            Player winner = Bukkit.getPlayer(duel.getWinner());
            if (winner != null) {
                Location back = duel.getReturnLoc(duel.getWinner());
                if (back != null) {
                    winner.teleport(back);
                }
                messages.send(winner, "duel.cleanup-finished");
            }
        }
    }

    private int[] unpack(long key) {
        int z = (int) (key & 0x3FFFFFFL);
        int y = (int) ((key >> 27) & 0x7FFL) - 512;
        int x = (int) ((key >> 38) & 0x3FFFFFFL);
        // 處理負數(封裝時只取低位,這裡還原符號)
        if (x >= (1 << 25)) x -= (1 << 26);
        if (z >= (1 << 25)) z -= (1 << 26);
        return new int[]{x, y, z};
    }
}

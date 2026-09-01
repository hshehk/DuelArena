package net.duelarena.command;

import net.duelarena.arena.Arena;
import net.duelarena.arena.ArenaManager;
import net.duelarena.arena.ArenaType;
import net.duelarena.duel.DuelManager;
import net.duelarena.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;
    private final DuelManager duelManager;
    private final MessageManager messages;

    public DuelCommand(ArenaManager arenaManager, DuelManager duelManager, MessageManager messages) {
        this.arenaManager = arenaManager;
        this.duelManager = duelManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "duel.player-only");
            return true;
        }

        if (args.length == 0) {
            messages.send(player, "duel.usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept" -> {
                String err = duelManager.acceptInvite(player);
                if (err != null) player.sendMessage("§c" + err);
            }
            case "deny" -> {
                String err = duelManager.denyInvite(player);
                if (err != null) player.sendMessage("§c" + err);
            }
            case "cancel" -> {
                String err = duelManager.cancelInvite(player);
                if (err != null) player.sendMessage("§c" + err);
            }
            case "leave" -> {
                String err = duelManager.requestLeave(player);
                if (err != null) player.sendMessage("§c" + err);
            }
            default -> {
                if (args.length < 2) {
                    messages.send(player, "duel.usage-invite");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    messages.send(player, "duel.player-not-found", "player", args[0]);
                    return true;
                }
                ArenaType type;
                try {
                    type = parseType(args[1]);
                } catch (IllegalArgumentException ex) {
                    messages.send(player, "duel.invalid-type");
                    return true;
                }
                Arena arena = findArenaOfType(type);
                if (arena == null) {
                    messages.send(player, "duel.arena-not-ready", "type", args[1]);
                    return true;
                }
                String err = duelManager.sendInvite(player, target, arena);
                if (err != null) player.sendMessage("§c" + err);
            }
        }
        return true;
    }

    private ArenaType parseType(String s) {
        String lower = s.toLowerCase();
        if (lower.startsWith("exp")) return ArenaType.EXPLOSIVE;
        if (lower.startsWith("bla")) return ArenaType.BLADE;
        return ArenaType.valueOf(s.toUpperCase());
    }

    /** 找出第一個符合類型且已完整設定的場地(依專案設計,每種類型固定一個場地)。 */
    private Arena findArenaOfType(ArenaType type) {
        for (Arena arena : arenaManager.getArenas().values()) {
            if (arena.getType() == type) {
                return arena;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("accept", "deny", "cancel", "leave"));
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            return filter(List.of("explosive", "blade"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}

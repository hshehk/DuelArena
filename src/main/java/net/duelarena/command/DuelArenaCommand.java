package net.duelarena.command;

import net.duelarena.arena.Arena;
import net.duelarena.arena.ArenaManager;
import net.duelarena.arena.ArenaType;
import net.duelarena.util.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuelArenaCommand implements CommandExecutor, TabCompleter {

    private final ArenaManager arenaManager;
    private final MessageManager messages;

    public DuelArenaCommand(ArenaManager arenaManager, MessageManager messages) {
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            if (arenaManager.getArenas().isEmpty()) {
                messages.send(sender, "arena.none");
                return true;
            }
            for (Arena a : arenaManager.getArenas().values()) {
                String status = a.isFullyConfigured()
                        ? messages.get("arena.status-configured")
                        : messages.get("arena.status-not-configured");
                messages.send(sender, "arena.list-entry",
                        "name", a.getName(), "type", a.getType(), "status", status);
            }
            return true;
        }

        if (sub.equals("reload")) {
            messages.reload();
            messages.send(sender, "general.reload");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }
        String name = args[1];

        switch (sub) {
            case "create" -> {
                if (args.length < 3) {
                    messages.send(sender, "arena.usage-create");
                    return true;
                }
                if (arenaManager.getArena(name) != null) {
                    messages.send(sender, "arena.already-exists");
                    return true;
                }
                ArenaType type;
                try {
                    type = ArenaType.valueOf(args[2].toUpperCase().startsWith("EXP") ? "EXPLOSIVE" : "BLADE");
                } catch (IllegalArgumentException ex) {
                    messages.send(sender, "arena.invalid-type");
                    return true;
                }
                arenaManager.createArena(name, type);
                arenaManager.save();
                messages.send(sender, "arena.created", "name", name, "type", type);
            }
            case "delete" -> {
                if (arenaManager.deleteArena(name)) {
                    arenaManager.save();
                    messages.send(sender, "arena.deleted", "name", name);
                } else {
                    messages.send(sender, "arena.not-found", "name", name);
                }
            }
            case "info" -> {
                Arena a = arenaManager.getArena(name);
                if (a == null) {
                    messages.send(sender, "arena.not-found", "name", name);
                    return true;
                }
                messages.send(sender, "arena.info-header", "name", a.getName());
                messages.send(sender, "arena.info-type", "type", a.getType());
                messages.send(sender, "arena.info-world", "world", a.getWorld());
                messages.send(sender, "arena.info-pos1", "pos", java.util.Arrays.toString(a.getPos1()));
                messages.send(sender, "arena.info-pos2", "pos", java.util.Arrays.toString(a.getPos2()));
                messages.send(sender, "arena.info-spawn1", "status",
                        a.getSpawn1() != null ? messages.get("arena.spawn-set") : messages.get("arena.spawn-not-set"));
                messages.send(sender, "arena.info-spawn2", "status",
                        a.getSpawn2() != null ? messages.get("arena.spawn-set") : messages.get("arena.spawn-not-set"));
            }
            case "setpos1", "setpos2", "setspawn1", "setspawn2" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "arena.player-only");
                    return true;
                }
                Arena a = arenaManager.getArena(name);
                if (a == null) {
                    messages.send(sender, "arena.not-found-create-first", "name", name);
                    return true;
                }
                switch (sub) {
                    case "setpos1" -> a.setPos1(player.getLocation());
                    case "setpos2" -> a.setPos2(player.getLocation());
                    case "setspawn1" -> a.setSpawn1(player.getLocation().clone());
                    case "setspawn2" -> a.setSpawn2(player.getLocation().clone());
                }
                arenaManager.save();
                messages.send(sender, "arena.position-set", "sub", sub, "name", name);
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        messages.sendList(sender, "arena.usage");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "delete", "info", "list", "reload",
                    "setpos1", "setpos2", "setspawn1", "setspawn2"), args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            return filter(new ArrayList<>(arenaManager.getArenas().keySet()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return filter(List.of("explosive", "blade"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}

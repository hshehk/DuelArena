package net.duelarena.command;

import net.duelarena.arena.Arena;
import net.duelarena.arena.ArenaManager;
import net.duelarena.arena.ArenaType;
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

    public DuelArenaCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
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
                sender.sendMessage("§e目前沒有任何場地。");
                return true;
            }
            for (Arena a : arenaManager.getArenas().values()) {
                sender.sendMessage("§7- §f" + a.getName() + " §7(" + a.getType() + ") "
                        + (a.isFullyConfigured() ? "§a[已完成設定]" : "§c[尚未設定完成]"));
            }
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
                    sender.sendMessage("§c用法: /duelarena create <名稱> <explosive|blade>");
                    return true;
                }
                if (arenaManager.getArena(name) != null) {
                    sender.sendMessage("§c已經有同名場地了。");
                    return true;
                }
                ArenaType type;
                try {
                    type = ArenaType.valueOf(args[2].toUpperCase().startsWith("EXP") ? "EXPLOSIVE" : "BLADE");
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("§c類型必須是 explosive 或 blade。");
                    return true;
                }
                arenaManager.createArena(name, type);
                arenaManager.save();
                sender.sendMessage("§a已建立場地 " + name + "(" + type + "),接著請設定 setpos1/setpos2/setspawn1/setspawn2。");
            }
            case "delete" -> {
                if (arenaManager.deleteArena(name)) {
                    arenaManager.save();
                    sender.sendMessage("§a已刪除場地 " + name);
                } else {
                    sender.sendMessage("§c找不到場地 " + name);
                }
            }
            case "info" -> {
                Arena a = arenaManager.getArena(name);
                if (a == null) {
                    sender.sendMessage("§c找不到場地 " + name);
                    return true;
                }
                sender.sendMessage("§6場地: " + a.getName());
                sender.sendMessage("§7類型: " + a.getType());
                sender.sendMessage("§7世界: " + a.getWorld());
                sender.sendMessage("§7Pos1: " + java.util.Arrays.toString(a.getPos1()));
                sender.sendMessage("§7Pos2: " + java.util.Arrays.toString(a.getPos2()));
                sender.sendMessage("§7Spawn1: " + (a.getSpawn1() != null ? "已設定" : "未設定"));
                sender.sendMessage("§7Spawn2: " + (a.getSpawn2() != null ? "已設定" : "未設定"));
            }
            case "setpos1", "setpos2", "setspawn1", "setspawn2" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c此指令只能由玩家使用(需要你目前站的位置)。");
                    return true;
                }
                Arena a = arenaManager.getArena(name);
                if (a == null) {
                    sender.sendMessage("§c找不到場地 " + name + ",請先用 create 建立。");
                    return true;
                }
                switch (sub) {
                    case "setpos1" -> a.setPos1(player.getLocation());
                    case "setpos2" -> a.setPos2(player.getLocation());
                    case "setspawn1" -> a.setSpawn1(player.getLocation().clone());
                    case "setspawn2" -> a.setSpawn2(player.getLocation().clone());
                }
                arenaManager.save();
                sender.sendMessage("§a已設定 " + sub + " -> " + name);
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/duelarena create <名稱> <explosive|blade>");
        sender.sendMessage("§e/duelarena setpos1|setpos2|setspawn1|setspawn2 <名稱>");
        sender.sendMessage("§e/duelarena delete <名稱>");
        sender.sendMessage("§e/duelarena list");
        sender.sendMessage("§e/duelarena info <名稱>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "delete", "info", "list", "setpos1", "setpos2", "setspawn1", "setspawn2"), args[0]);
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

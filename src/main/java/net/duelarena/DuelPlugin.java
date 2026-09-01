package net.duelarena;

import net.duelarena.arena.ArenaManager;
import net.duelarena.command.DuelArenaCommand;
import net.duelarena.command.DuelCommand;
import net.duelarena.duel.DuelManager;
import net.duelarena.listener.DuelBlockListener;
import net.duelarena.listener.DuelEntityListener;
import net.duelarena.listener.DuelExplosionListener;
import net.duelarena.listener.DuelPlayerListener;
import net.duelarena.util.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DuelPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageManager = new MessageManager(this);
        this.arenaManager = new ArenaManager(this);
        this.duelManager = new DuelManager(this, messageManager);

        getServer().getPluginManager().registerEvents(
                new DuelBlockListener(arenaManager, duelManager, messageManager), this);
        getServer().getPluginManager().registerEvents(
                new DuelExplosionListener(arenaManager, duelManager), this);
        getServer().getPluginManager().registerEvents(
                new DuelEntityListener(this, arenaManager, messageManager), this);
        getServer().getPluginManager().registerEvents(
                new DuelPlayerListener(duelManager), this);

        var duelCmd = getCommand("duel");
        if (duelCmd != null) {
            DuelCommand executor = new DuelCommand(arenaManager, duelManager, messageManager);
            duelCmd.setExecutor(executor);
            duelCmd.setTabCompleter(executor);
        }
        var duelArenaCmd = getCommand("duelarena");
        if (duelArenaCmd != null) {
            DuelArenaCommand executor = new DuelArenaCommand(arenaManager, messageManager);
            duelArenaCmd.setExecutor(executor);
            duelArenaCmd.setTabCompleter(executor);
        }

        getLogger().info("DuelArena 已啟用。");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.save();
        }
        getLogger().info("DuelArena 已停用。");
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}

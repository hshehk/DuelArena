package net.duelarena;

import net.duelarena.arena.ArenaManager;
import net.duelarena.command.DuelArenaCommand;
import net.duelarena.command.DuelCommand;
import net.duelarena.duel.DuelManager;
import net.duelarena.listener.DuelBlockListener;
import net.duelarena.listener.DuelEntityListener;
import net.duelarena.listener.DuelExplosionListener;
import net.duelarena.listener.DuelPlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DuelPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private DuelManager duelManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.duelManager = new DuelManager(this);

        getServer().getPluginManager().registerEvents(
                new DuelBlockListener(arenaManager, duelManager), this);
        getServer().getPluginManager().registerEvents(
                new DuelExplosionListener(arenaManager, duelManager), this);
        getServer().getPluginManager().registerEvents(
                new DuelEntityListener(this, arenaManager), this);
        getServer().getPluginManager().registerEvents(
                new DuelPlayerListener(duelManager), this);

        var duelCmd = getCommand("duel");
        if (duelCmd != null) {
            DuelCommand executor = new DuelCommand(arenaManager, duelManager);
            duelCmd.setExecutor(executor);
            duelCmd.setTabCompleter(executor);
        }
        var duelArenaCmd = getCommand("duelarena");
        if (duelArenaCmd != null) {
            DuelArenaCommand executor = new DuelArenaCommand(arenaManager);
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
}

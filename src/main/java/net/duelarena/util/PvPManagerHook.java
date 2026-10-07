package net.duelarena.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * 以反射方式對接 PvPManager(不需要編譯期依賴),用來判斷玩家是否還在新手保護中。
 * 同時支援 PvPManager 4.x(me.chancesd.pvpmanager.player.CombatPlayer)
 * 與 3.x(me.NoChance.PvPManager.PvPlayer)。
 * 找不到 PvPManager 或 API 失敗時一律視為「不是新手」,不會影響決鬥功能。
 */
public class PvPManagerHook {

    private static final String[] CLASS_NAMES = {
            "me.chancesd.pvpmanager.player.CombatPlayer",
            "me.NoChance.PvPManager.PvPlayer"
    };

    private final JavaPlugin plugin;
    private boolean resolved;
    private Method getMethod;
    private Method isNewbieMethod;
    private boolean warned;

    public PvPManagerHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isNewbie(Player player) {
        if (!plugin.getConfig().getBoolean("settings.block-newbie-protected", true)) {
            return false;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PvPManager")) {
            return false;
        }
        try {
            resolve();
            if (getMethod == null || isNewbieMethod == null) {
                return false;
            }
            Object combatPlayer = getMethod.invoke(null, player);
            if (combatPlayer == null) {
                return false;
            }
            Object result = isNewbieMethod.invoke(combatPlayer);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            warnOnce("讀取 PvPManager 新手保護狀態失敗: " + t);
            return false;
        }
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        for (String className : CLASS_NAMES) {
            try {
                Class<?> clazz = Class.forName(className);
                getMethod = clazz.getMethod("get", Player.class);
                isNewbieMethod = clazz.getMethod("isNewbie");
                return;
            } catch (Throwable ignored) {
                // 試下一個
            }
        }
        warnOnce("偵測到 PvPManager,但找不到可用的 API,新手保護檢查已停用。");
    }

    private void warnOnce(String msg) {
        if (!warned) {
            warned = true;
            plugin.getLogger().warning(msg);
        }
    }
}

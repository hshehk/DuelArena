package net.duelarena.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 負責讀取 messages.yml、替換 %placeholder% 與 &顏色代碼,並提供發送訊息的輔助方法。
 * 找不到的 key 會 fallback 回 jar 內建的預設值,即使玩家刪掉某一行也不會噴例外。
 */
public class MessageManager {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        reload();
    }

    /** 重新從硬碟載入 messages.yml,並用 jar 內建的預設值補齊缺漏的 key。 */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);

        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("載入 messages.yml 預設值失敗: " + ex.getMessage());
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String prefix() {
        return color(config.getString("prefix", ""));
    }

    /** 取得訊息(已上色、已替換 %key% -> value,不含 prefix)。placeholders 需成對傳入,例如 "name", arenaName。 */
    public String get(String key, Object... placeholders) {
        String value = config.getString(key);
        if (value == null) {
            return "§c[缺少訊息: " + key + "]";
        }
        String msg = color(value);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
        }
        return msg;
    }

    /** 取得訊息並加上 prefix。 */
    public String getWithPrefix(String key, Object... placeholders) {
        return prefix() + get(key, placeholders);
    }

    /** 取得字串清單(用於 usage 這類多行訊息),已上色,不含 prefix。 */
    public List<String> getList(String key) {
        return config.getStringList(key).stream().map(this::color).toList();
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public void sendWithPrefix(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(getWithPrefix(key, placeholders));
    }

    public void sendList(CommandSender sender, String key) {
        for (String line : getList(key)) {
            sender.sendMessage(line);
        }
    }
}

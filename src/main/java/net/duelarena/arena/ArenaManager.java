package net.duelarena.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 場地資料獨立存在 plugins/DuelArena/arenas.yml,不再和 config.yml 混在一起。
 * 若 arenas.yml 不存在但舊版 config.yml 裡有 arenas 區塊,會自動搬移一次。
 */
public class ArenaManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        load();
    }

    public void load() {
        arenas.clear();

        ConfigurationSection root;
        boolean migrate = false;
        if (file.exists()) {
            root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("arenas");
        } else {
            // 舊版資料(存在 config.yml)自動搬移到 arenas.yml
            root = plugin.getConfig().getConfigurationSection("arenas");
            migrate = root != null && !root.getKeys(false).isEmpty();
        }
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(name);
            if (sec == null) continue;
            try {
                ArenaType type = ArenaType.valueOf(sec.getString("type", "BLADE"));
                Arena arena = new Arena(name, type);
                String world = sec.getString("world");
                if (world != null) {
                    if (sec.contains("x1")) {
                        arena.setRawPos1(world, sec.getInt("x1"), sec.getInt("y1"), sec.getInt("z1"));
                    }
                    if (sec.contains("x2")) {
                        arena.setRawPos2(world, sec.getInt("x2"), sec.getInt("y2"), sec.getInt("z2"));
                    }
                }
                arena.setSpawn1Raw(sec.getString("spawn1"));
                arena.setSpawn2Raw(sec.getString("spawn2"));
                arenas.put(name.toLowerCase(), arena);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("場地 " + name + " 設定有誤,略過:" + ex.getMessage());
            }
        }
        if (migrate) {
            save();
            plugin.getLogger().info("已將舊版 config.yml 的場地資料搬移到 arenas.yml。");
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.getName();
            yaml.set(base + ".type", arena.getType().name());
            if (arena.getWorld() != null) {
                yaml.set(base + ".world", arena.getWorld());
                Integer[] p1 = arena.getPos1();
                Integer[] p2 = arena.getPos2();
                if (p1[0] != null) {
                    yaml.set(base + ".x1", p1[0]);
                    yaml.set(base + ".y1", p1[1]);
                    yaml.set(base + ".z1", p1[2]);
                }
                if (p2[0] != null) {
                    yaml.set(base + ".x2", p2[0]);
                    yaml.set(base + ".y2", p2[1]);
                    yaml.set(base + ".z2", p2[2]);
                }
            }
            if (arena.hasSpawn1()) {
                yaml.set(base + ".spawn1", arena.getSpawn1Raw());
            }
            if (arena.hasSpawn2()) {
                yaml.set(base + ".spawn2", arena.getSpawn2Raw());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("儲存 arenas.yml 失敗: " + ex.getMessage());
        }
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Arena createArena(String name, ArenaType type) {
        Arena arena = new Arena(name, type);
        arenas.put(name.toLowerCase(), arena);
        return arena;
    }

    public boolean deleteArena(String name) {
        return arenas.remove(name.toLowerCase()) != null;
    }

    public Map<String, Arena> getArenas() {
        return arenas;
    }

    /** 找出某個座標所在的場地(若有的話)。 */
    public Arena findArenaAt(Location loc) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(loc)) {
                return arena;
            }
        }
        return null;
    }
}

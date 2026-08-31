package net.duelarena.arena;

import net.duelarena.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArenaManager {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        arenas.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("arenas");
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
                Location s1 = LocationUtil.deserialize(sec.getString("spawn1"));
                Location s2 = LocationUtil.deserialize(sec.getString("spawn2"));
                arena.setSpawn1(s1);
                arena.setSpawn2(s2);
                arenas.put(name.toLowerCase(), arena);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("場地 " + name + " 設定有誤,略過:" + ex.getMessage());
            }
        }
    }

    public void save() {
        plugin.getConfig().set("arenas", null);
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.getName();
            plugin.getConfig().set(base + ".type", arena.getType().name());
            if (arena.getWorld() != null) {
                plugin.getConfig().set(base + ".world", arena.getWorld());
                Integer[] p1 = arena.getPos1();
                Integer[] p2 = arena.getPos2();
                if (p1[0] != null) {
                    plugin.getConfig().set(base + ".x1", p1[0]);
                    plugin.getConfig().set(base + ".y1", p1[1]);
                    plugin.getConfig().set(base + ".z1", p1[2]);
                }
                if (p2[0] != null) {
                    plugin.getConfig().set(base + ".x2", p2[0]);
                    plugin.getConfig().set(base + ".y2", p2[1]);
                    plugin.getConfig().set(base + ".z2", p2[2]);
                }
            }
            if (arena.getSpawn1() != null) {
                plugin.getConfig().set(base + ".spawn1", LocationUtil.serialize(arena.getSpawn1()));
            }
            if (arena.getSpawn2() != null) {
                plugin.getConfig().set(base + ".spawn2", LocationUtil.serialize(arena.getSpawn2()));
            }
        }
        plugin.saveConfig();
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

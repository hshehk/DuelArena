package net.duelarena.arena;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * 一個決鬥場地。用兩個對角座標(pos1/pos2)框出整個場地範圍(不管場地形狀是圓的、方的都沒差,
 * 只要框住整個建築範圍即可),再加上兩個玩家的傳送點。
 */
public class Arena {

    private final String name;
    private ArenaType type;
    private String world;

    private Integer x1, y1, z1;
    private Integer x2, y2, z2;

    private Location spawn1;
    private Location spawn2;

    public Arena(String name, ArenaType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ArenaType getType() {
        return type;
    }

    public void setType(ArenaType type) {
        this.type = type;
    }

    public String getWorld() {
        return world;
    }

    public void setPos1(Location loc) {
        this.world = loc.getWorld().getName();
        this.x1 = loc.getBlockX();
        this.y1 = loc.getBlockY();
        this.z1 = loc.getBlockZ();
    }

    public void setPos2(Location loc) {
        this.world = loc.getWorld().getName();
        this.x2 = loc.getBlockX();
        this.y2 = loc.getBlockY();
        this.z2 = loc.getBlockZ();
    }

    public void setRawPos1(String world, int x, int y, int z) {
        this.world = world;
        this.x1 = x;
        this.y1 = y;
        this.z1 = z;
    }

    public void setRawPos2(String world, int x, int y, int z) {
        this.world = world;
        this.x2 = x;
        this.y2 = y;
        this.z2 = z;
    }

    public Integer[] getPos1() {
        return new Integer[]{x1, y1, z1};
    }

    public Integer[] getPos2() {
        return new Integer[]{x2, y2, z2};
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public boolean isFullyConfigured() {
        return world != null && x1 != null && x2 != null
                && spawn1 != null && spawn2 != null;
    }

    /** 是否位於此場地的框選範圍內(同世界 + 座標在 min/max 之間)。 */
    public boolean contains(Location loc) {
        if (world == null || x1 == null || x2 == null) {
            return false;
        }
        World w = loc.getWorld();
        if (w == null || !w.getName().equals(world)) {
            return false;
        }
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        return bx >= minX && bx <= maxX
                && by >= minY && by <= maxY
                && bz >= minZ && bz <= maxZ;
    }
}

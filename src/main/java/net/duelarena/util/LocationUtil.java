package net.duelarena.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

/**
 * 座標與字串互相轉換的小工具。
 * 格式:世界名,x,y,z,yaw,pitch
 */
public final class LocationUtil {

    private LocationUtil() {
    }

    public static String serialize(Location loc) {
        return String.format(Locale.ROOT, "%s,%f,%f,%f,%f,%f",
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch());
    }

    public static Location deserialize(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        String[] parts = s.split(",");
        if (parts.length < 6) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }

    /** 把座標壓成一個 long,用來當作 HashSet/HashMap 的 key(同世界內使用)。 */
    public static long packBlockKey(int x, int y, int z) {
        // x, z 用 27 bits(可容納 -67M ~ 67M),y 用 int 前段位置足夠(世界高度有限)
        long lx = ((long) x) & 0x3FFFFFFL;
        long ly = ((long) (y + 512)) & 0x7FFL; // 世界高度範圍足夠
        long lz = ((long) z) & 0x3FFFFFFL;
        return (lx << 38) | (ly << 27) | lz;
    }

    public static long packBlockKey(org.bukkit.block.Block block) {
        return packBlockKey(block.getX(), block.getY(), block.getZ());
    }

    public static long packBlockKey(Location loc) {
        return packBlockKey(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

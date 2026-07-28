package com.shootcraft.plugin.game;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Region cuboide simple definie par deux coins. Utilisee pour la zone de jeu
 * optionnelle (gamezone) d'une arene : quand elle est configuree, un joueur qui
 * en sort (ex: tombe dans le vide) est automatiquement replace sur un spawn.
 */
public class CuboidRegion {

    private final Location corner1;
    private final Location corner2;

    private final double minX, minY, minZ, maxX, maxY, maxZ;
    private final World world;

    public CuboidRegion(Location corner1, Location corner2) {
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.world = corner1.getWorld();
        this.minX = Math.min(corner1.getX(), corner2.getX());
        this.minY = Math.min(corner1.getY(), corner2.getY());
        this.minZ = Math.min(corner1.getZ(), corner2.getZ());
        this.maxX = Math.max(corner1.getX(), corner2.getX());
        this.maxY = Math.max(corner1.getY(), corner2.getY());
        this.maxZ = Math.max(corner1.getZ(), corner2.getZ());
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(world)) {
            return false;
        }
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public double getMinY() {
        return minY;
    }
}

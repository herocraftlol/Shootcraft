package com.shootcraft.plugin.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stocke tous les points importants d'une arene ShootCraft configures par un admin :
 * - le point de lobby (attente avant/entre les parties)
 * - la liste des points de spawn (les joueurs y reapparaissent au hasard apres chaque mort)
 * - une zone de jeu optionnelle (gamezone), utilisee comme garde-fou anti-chute dans le vide
 * - le nombre min/max de joueurs et la duree de partie, surchargeables par arene
 */
public class Arena {

    private Location lobbySpawn;
    private final List<Location> spawns = new ArrayList<>();
    private CuboidRegion gameZone;

    /** -1 = non defini, on utilise alors la valeur globale du config.yml */
    private int maxPlayers = -1;
    private int minPlayers = -1;
    private int gameTimeSeconds = -1;

    /**
     * Une arene est jouable des qu'elle a un lobby et au moins 2 spawns (il en faut au
     * moins 2 pour que le tirage aleatoire des spawns ait un sens en duel).
     */
    public boolean isFullyConfigured() {
        return lobbySpawn != null && spawns.size() >= 2;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public List<Location> getSpawns() {
        return Collections.unmodifiableList(spawns);
    }

    public int getSpawnCount() {
        return spawns.size();
    }

    /**
     * Ajoute un nouveau spawn (a la suite de la liste). Renvoie l'index (1-based) du
     * spawn cree.
     */
    public int addSpawn(Location loc) {
        spawns.add(loc);
        return spawns.size();
    }

    /**
     * Supprime le spawn a l'index donne (1-based). Renvoie false si l'index est invalide.
     */
    public boolean removeSpawn(int index) {
        if (index < 1 || index > spawns.size()) {
            return false;
        }
        spawns.remove(index - 1);
        return true;
    }

    public CuboidRegion getGameZone() {
        return gameZone;
    }

    public void setGameZone(CuboidRegion gameZone) {
        this.gameZone = gameZone;
    }

    public boolean isInGameZone(Location loc) {
        return gameZone != null && gameZone.contains(loc);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers <= 0 ? -1 : maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers <= 0 ? -1 : minPlayers;
    }

    public int getGameTimeSeconds() {
        return gameTimeSeconds;
    }

    public void setGameTimeSeconds(int gameTimeSeconds) {
        this.gameTimeSeconds = gameTimeSeconds <= 0 ? -1 : gameTimeSeconds;
    }

    // ---- Sauvegarde / chargement dans un fichier de config ----

    public void saveToConfig(FileConfiguration config) {
        saveLocation(config, "arena.lobby", lobbySpawn);
        config.set("arena.spawns", null);
        for (int i = 0; i < spawns.size(); i++) {
            saveLocation(config, "arena.spawns." + (i + 1), spawns.get(i));
        }
        saveRegion(config, "arena.gamezone", gameZone);
        config.set("arena.max-players", maxPlayers > 0 ? maxPlayers : null);
        config.set("arena.min-players", minPlayers > 0 ? minPlayers : null);
        config.set("arena.game-time", gameTimeSeconds > 0 ? gameTimeSeconds : null);
    }

    public void loadFromConfig(FileConfiguration config) {
        this.lobbySpawn = loadLocation(config, "arena.lobby");
        spawns.clear();
        int index = 1;
        while (config.isSet("arena.spawns." + index + ".world")) {
            Location loc = loadLocation(config, "arena.spawns." + index);
            if (loc != null) {
                spawns.add(loc);
            }
            index++;
        }
        this.gameZone = loadRegion(config, "arena.gamezone");
        this.maxPlayers = config.isSet("arena.max-players") ? config.getInt("arena.max-players") : -1;
        this.minPlayers = config.isSet("arena.min-players") ? config.getInt("arena.min-players") : -1;
        this.gameTimeSeconds = config.isSet("arena.game-time") ? config.getInt("arena.game-time") : -1;
    }

    private void saveLocation(FileConfiguration config, String path, Location loc) {
        if (loc == null) return;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.isSet(path + ".world")) return null;
        World world = org.bukkit.Bukkit.getWorld(config.getString(path + ".world"));
        if (world == null) return null;
        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }

    private void saveRegion(FileConfiguration config, String path, CuboidRegion region) {
        if (region == null) {
            config.set(path, null);
            return;
        }
        saveLocation(config, path + ".corner1", region.getCorner1());
        saveLocation(config, path + ".corner2", region.getCorner2());
    }

    private CuboidRegion loadRegion(FileConfiguration config, String path) {
        if (config.getConfigurationSection(path) == null) return null;
        Location c1 = loadLocation(config, path + ".corner1");
        Location c2 = loadLocation(config, path + ".corner2");
        if (c1 == null || c2 == null) return null;
        return new CuboidRegion(c1, c2);
    }
}

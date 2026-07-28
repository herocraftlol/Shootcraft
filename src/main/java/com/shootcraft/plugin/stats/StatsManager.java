package com.shootcraft.plugin.stats;

import com.shootcraft.plugin.ShootCraftPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statistiques permanentes des joueurs (kills, morts, parties jouees, victoires),
 * conservees entre les redemarrages du serveur dans stats.yml, independamment des
 * compteurs "en direct" d'une partie (voir GameManager).
 *
 * Alimente le leaderboard holographique (voir hologram.LeaderboardManager) ainsi
 * que d'eventuelles commandes de statistiques.
 */
public class StatsManager {

    private final ShootCraftPlugin plugin;
    private final File statsFile;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();

    public StatsManager(ShootCraftPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        playerStats.clear();
        if (!statsFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "players." + uuidStr;
                PlayerStats stats = new PlayerStats();
                stats.name = config.getString(path + ".name", "???");
                stats.kills = config.getInt(path + ".kills", 0);
                stats.deaths = config.getInt(path + ".deaths", 0);
                stats.gamesPlayed = config.getInt(path + ".games-played", 0);
                stats.gamesWon = config.getInt(path + ".games-won", 0);
                playerStats.put(uuid, stats);
            } catch (IllegalArgumentException ignored) {
                // UUID invalide dans le fichier : on ignore cette entree.
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerStats stats = entry.getValue();
            config.set(path + ".name", stats.name);
            config.set(path + ".kills", stats.kills);
            config.set(path + ".deaths", stats.deaths);
            config.set(path + ".games-played", stats.gamesPlayed);
            config.set(path + ".games-won", stats.gamesWon);
        }
        try {
            File parent = statsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder stats.yml : " + e.getMessage());
        }
    }

    private PlayerStats getOrCreate(UUID uuid, String name) {
        PlayerStats stats = playerStats.computeIfAbsent(uuid, u -> new PlayerStats());
        if (name != null) {
            stats.name = name;
        }
        return stats;
    }

    public void addKill(UUID uuid, String name) {
        getOrCreate(uuid, name).kills++;
    }

    public void addDeath(UUID uuid, String name) {
        getOrCreate(uuid, name).deaths++;
    }

    /**
     * A appeler une fois par joueur a la fin de chaque partie : incremente le
     * nombre de parties jouees, et le nombre de victoires si "won" est vrai.
     */
    public void addGameResult(UUID uuid, String name, boolean won) {
        PlayerStats stats = getOrCreate(uuid, name);
        stats.gamesPlayed++;
        if (won) {
            stats.gamesWon++;
        }
    }

    public PlayerStats getPlayerStats(UUID uuid, String fallbackName) {
        PlayerStats stats = playerStats.get(uuid);
        if (stats == null) {
            PlayerStats empty = new PlayerStats();
            empty.name = fallbackName;
            return empty;
        }
        return stats;
    }

    /**
     * Classement des "limit" meilleurs joueurs selon le comparateur fourni
     * (ordre decroissant, c'est a dire meilleur en premier).
     */
    public List<Map.Entry<UUID, PlayerStats>> getTopPlayers(int limit, Comparator<PlayerStats> comparator) {
        List<Map.Entry<UUID, PlayerStats>> entries = new ArrayList<>(playerStats.entrySet());
        entries.sort((a, b) -> comparator.compare(b.getValue(), a.getValue()));
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    public void resetStats() {
        playerStats.clear();
        save();
    }

    /**
     * Statistiques cumulees d'un joueur. Le K/D est calcule a la volee
     * (kills / max(1, deaths), arrondi a 2 decimales).
     */
    public static class PlayerStats {
        String name = "???";
        int kills;
        int deaths;
        int gamesPlayed;
        int gamesWon;

        public String getName() {
            return name;
        }

        public int getKills() {
            return kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public int getGamesPlayed() {
            return gamesPlayed;
        }

        public int getGamesWon() {
            return gamesWon;
        }

        public double getKD() {
            double ratio = kills / (double) Math.max(1, deaths);
            return Math.round(ratio * 100.0) / 100.0;
        }

        public double getWinRate() {
            if (gamesPlayed == 0) return 0.0;
            return Math.round((gamesWon * 10000.0 / gamesPlayed)) / 100.0;
        }
    }
}

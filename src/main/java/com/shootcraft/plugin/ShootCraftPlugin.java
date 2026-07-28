package com.shootcraft.plugin;

import com.shootcraft.plugin.commands.ShootCraftCommand;
import com.shootcraft.plugin.game.ArenaManager;
import com.shootcraft.plugin.gui.ArenaGUI;
import com.shootcraft.plugin.gui.ArenaGUIListener;
import com.shootcraft.plugin.hologram.LeaderboardManager;
import com.shootcraft.plugin.listeners.ForceStartItemListener;
import com.shootcraft.plugin.listeners.LeaveItemListener;
import com.shootcraft.plugin.listeners.PlayerConnectionListener;
import com.shootcraft.plugin.listeners.PlayerDeathListener;
import com.shootcraft.plugin.listeners.PlayerRespawnListener;
import com.shootcraft.plugin.listeners.ProtectionListener;
import com.shootcraft.plugin.listeners.SpeedBoostListener;
import com.shootcraft.plugin.listeners.WandListener;
import com.shootcraft.plugin.scoreboard.ScoreboardManager;
import com.shootcraft.plugin.stats.StatsManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Point d'entree du plugin ShootCraft : un minijeu de duel ou les joueurs
 * s'affrontent avec un baton magique qui tire un rayon quasi-instantane.
 *
 * Comme HikaBrain, ShootCraft gere plusieurs arenes nommees et independantes,
 * chacune pilotee par sa propre instance de GameManager (voir ArenaManager).
 */
public class ShootCraftPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private ScoreboardManager scoreboardManager;
    private ArenaGUI arenaGUI;
    private StatsManager statsManager;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.arenaGUI = new ArenaGUI(this);
        this.statsManager = new StatsManager(this);
        this.leaderboardManager = new LeaderboardManager(this);

        arenaManager.loadAll();
        statsManager.load();
        leaderboardManager.loadConfig();

        ShootCraftCommand command = new ShootCraftCommand(this);
        getCommand("sc").setExecutor(command);
        getCommand("sc").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new ArenaGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new SpeedBoostListener(this), this);
        getServer().getPluginManager().registerEvents(new LeaveItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ForceStartItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        getLogger().info("ShootCraft active - " + arenaManager.getNames().size() + " arene(s) chargee(s).");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.stopAll();
            arenaManager.saveAll();
        }
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
        if (leaderboardManager != null) {
            leaderboardManager.shutdown();
        }
        if (statsManager != null) {
            statsManager.save();
        }
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ArenaGUI getArenaGUI() {
        return arenaGUI;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}

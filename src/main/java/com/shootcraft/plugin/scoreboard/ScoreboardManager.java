package com.shootcraft.plugin.scoreboard;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gere le sidebar affiche a chaque joueur pendant qu'il est dans une arene
 * ShootCraft : temps restant de la partie + classement (kills), du premier au
 * dernier.
 *
 * Un scoreboard distinct est cree par joueur (necessaire pour avoir un titre/
 * des lignes independantes), mis a jour une fois par seconde.
 */
public class ScoreboardManager {

    private final ShootCraftPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask updateTask;
    private String title;

    private static final char[] INVISIBLE_CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public ScoreboardManager(ShootCraftPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        startUpdateTask();
    }

    public void loadConfig() {
        title = plugin.getConfig().getString("scoreboard.title", "&b&lSHOOT&e&lCRAFT");
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 20L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (UUID uuid : new HashSet<>(boards.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        boards.clear();
    }

    public void show(Player player, GameManager gm) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("shootcraft", Criteria.DUMMY, color(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        applyLines(board, objective, gm);
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void updateAll() {
        boards.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        for (Map.Entry<UUID, Scoreboard> entry : new HashMap<>(boards).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            GameManager gm = plugin.getArenaManager().findArenaOf(player);
            if (gm == null) {
                remove(player);
                continue;
            }
            Scoreboard board = entry.getValue();
            Objective objective = board.getObjective("shootcraft");
            if (objective == null) {
                objective = board.registerNewObjective("shootcraft", Criteria.DUMMY, color(title));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            clearLines(board);
            applyLines(board, objective, gm);
            player.setScoreboard(board);
        }
    }

    private void clearLines(Scoreboard board) {
        Set<org.bukkit.scoreboard.Team> teams = new HashSet<>(board.getTeams());
        for (org.bukkit.scoreboard.Team team : teams) {
            if (team.getName().startsWith("sc_")) {
                for (String entry : team.getEntries()) {
                    board.resetScores(entry);
                }
                team.unregister();
            }
        }
    }

    private void applyLines(Scoreboard board, Objective objective, GameManager gm) {
        List<String> lines = new ArrayList<>();

        String timeStr = formatTime(gm.getTimeLeft());
        if (gm.getState() == GameState.COUNTDOWN) {
            lines.add("&7Debut dans: &e" + timeStr);
        } else if (gm.getState() == GameState.PLAYING) {
            lines.add("&7Temps restant: &e" + timeStr);
        } else {
            lines.add("&7En attente de joueurs...");
        }
        lines.add("&8" + repeat('\u2500', 16));
        lines.add("&f&lClassement:");

        List<UUID> leaderboard = gm.getLeaderboard();
        if (leaderboard.isEmpty()) {
            lines.add("&7Aucun joueur");
        } else {
            int rank = 1;
            for (UUID uuid : leaderboard) {
                if (rank > 5) break;
                Player p = Bukkit.getPlayer(uuid);
                String pname = p != null ? p.getName() : "???";
                String rankColor = rank == 1 ? "&6" : rank == 2 ? "&7" : rank == 3 ? "&c" : "&f";
                lines.add(rankColor + rank + ". &f" + pname + " &7- &a" + gm.getKills(uuid));
                rank++;
            }
        }

        lines.add("&8" + repeat('\u2500', 16));
        lines.add("&7Joueurs: &a" + gm.getPlayerCount() + "&7/&a" + gm.getMaxPlayers());
        lines.add("&7Arene: &b" + gm.getName());

        for (int i = 0; i < lines.size(); i++) {
            String parsed = color(lines.get(i));
            String identifier = invisibleEntry(i);
            String teamName = "sc_" + i;
            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            team.setPrefix(parsed.length() > 64 ? parsed.substring(0, 64) : parsed);
            team.setSuffix("");
            team.addEntry(identifier);
            objective.getScore(identifier).setScore(lines.size() - i);
        }
    }

    private String invisibleEntry(int index) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.COLOR_CHAR).append(INVISIBLE_CODES[index % INVISIBLE_CODES.length]);
        sb.append(ChatColor.COLOR_CHAR).append(INVISIBLE_CODES[(index / INVISIBLE_CODES.length) % INVISIBLE_CODES.length]);
        sb.append(ChatColor.RESET);
        return sb.toString();
    }

    private String repeat(char c, int count) {
        return String.valueOf(c).repeat(Math.max(0, count));
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void reload() {
        plugin.reloadConfig();
        loadConfig();
    }
}

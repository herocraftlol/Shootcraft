package com.shootcraft.plugin.game;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pilote une arene ShootCraft : lobby, compte a rebours, partie en cours (avec
 * respawn aleatoire des joueurs abattus), fin de partie et retour au lobby.
 *
 * Chaque arene nommee possede sa propre instance de GameManager, ce qui permet
 * plusieurs parties ShootCraft simultanees sur le meme serveur.
 */
public class GameManager {

    private final ShootCraftPlugin plugin;
    private final String name;
    private final Arena arena;
    private final File configFile;

    private GameState state = GameState.NOT_CONFIGURED;

    /** Joueurs actuellement lies a cette arene (en lobby ou en partie). */
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> deaths = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new ConcurrentHashMap<>();

    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask endTask;

    private int countdownSecondsLeft;
    private int gameSecondsLeft;

    public GameManager(ShootCraftPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.arena = new Arena();
        File arenasDir = new File(plugin.getDataFolder(), "arenas");
        if (!arenasDir.exists()) {
            arenasDir.mkdirs();
        }
        this.configFile = new File(arenasDir, name + ".yml");
    }

    // ================= GETTERS DE BASE =================

    public String getName() {
        return name;
    }

    public Arena getArena() {
        return arena;
    }

    public GameState getState() {
        return state;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isPlaying(Player player) {
        return players.contains(player.getUniqueId());
    }

    public List<Player> getOnlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) list.add(p);
        }
        return list;
    }

    public int getMaxPlayers() {
        return arena.getMaxPlayers() > 0 ? arena.getMaxPlayers() : plugin.getConfig().getInt("max-players", 12);
    }

    public int getMinPlayers() {
        return arena.getMinPlayers() > 0 ? arena.getMinPlayers() : plugin.getConfig().getInt("min-players", 2);
    }

    public int getGameTimeSeconds() {
        return arena.getGameTimeSeconds() > 0 ? arena.getGameTimeSeconds() : plugin.getConfig().getInt("default-game-time", 180);
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getDeaths(UUID uuid) {
        return deaths.getOrDefault(uuid, 0);
    }

    public int getTimeLeft() {
        return state == GameState.PLAYING ? gameSecondsLeft : (state == GameState.COUNTDOWN ? countdownSecondsLeft : 0);
    }

    /**
     * Classement des joueurs actuellement lies a l'arene, du plus grand nombre de
     * kills au plus petit (egalite -> ordre d'arrivee).
     */
    public List<UUID> getLeaderboard() {
        List<UUID> list = new ArrayList<>(players);
        list.sort(Comparator.comparingInt((UUID u) -> getKills(u)).reversed());
        return list;
    }

    private boolean isJoinableState() {
        return state == GameState.WAITING || state == GameState.COUNTDOWN || state == GameState.PLAYING;
    }

    // ================= REJOINDRE / QUITTER =================

    public boolean join(Player player) {
        GameManager current = plugin.getArenaManager().findArenaOf(player);
        if (current != null) {
            // Une partie terminee ("Rejouer") ne doit pas bloquer une nouvelle jointure :
            // on retire discretement le joueur de l'arene finie avant de continuer.
            if (current == this || current.getState() != GameState.ENDING) {
                MessageUtil.sendKey(plugin, player, "already-in-game");
                return false;
            }
            current.leave(player);
        }
        if (!arena.isFullyConfigured() || state == GameState.NOT_CONFIGURED) {
            MessageUtil.sendKey(plugin, player, "arena-not-ready");
            return false;
        }
        if (!isJoinableState() || players.size() >= getMaxPlayers()) {
            MessageUtil.sendKey(plugin, player, "arena-full");
            return false;
        }

        snapshots.put(player.getUniqueId(), PlayerSnapshot.capture(player));
        players.add(player.getUniqueId());
        kills.put(player.getUniqueId(), 0);
        deaths.put(player.getUniqueId(), 0);

        prepareForLobbyOrGame(player);

        boolean midGameJoin = state == GameState.PLAYING;
        if (midGameJoin) {
            player.teleport(getRandomSpawn());
            MessageUtil.sendKey(plugin, player, "join-ingame");
            broadcastExcept(player, MessageUtil.get(plugin, "join", "player", player.getName(),
                    "current", String.valueOf(players.size()), "max", String.valueOf(getMaxPlayers())));
        } else {
            player.teleport(arena.getLobbySpawn());
            broadcast(MessageUtil.get(plugin, "join", "player", player.getName(),
                    "current", String.valueOf(players.size()), "max", String.valueOf(getMaxPlayers())));
        }

        plugin.getScoreboardManager().show(player, this);

        if (state == GameState.WAITING && players.size() >= getMinPlayers()) {
            startLobbyCountdown();
        } else if (state == GameState.COUNTDOWN && players.size() >= getMaxPlayers()) {
            // Accelere le compte a rebours si le lobby est plein
            restartCountdown(plugin.getConfig().getInt("lobby-countdown-fast", 8));
        }
        return true;
    }

    public void leave(Player player) {
        UUID uuid = player.getUniqueId();
        if (!players.remove(uuid)) {
            return;
        }
        kills.remove(uuid);
        deaths.remove(uuid);
        plugin.getScoreboardManager().remove(player);
        restore(player);

        broadcast(MessageUtil.get(plugin, "leave", "player", player.getName()));

        if (players.isEmpty()) {
            if (state == GameState.COUNTDOWN) {
                cancelCountdown();
                state = GameState.WAITING;
            } else if (state == GameState.PLAYING) {
                endGame();
            }
        } else if (state == GameState.COUNTDOWN && players.size() < getMinPlayers()) {
            cancelCountdown();
            state = GameState.WAITING;
            broadcast(MessageUtil.get(plugin, "countdown-cancelled"));
        }
    }

    /**
     * Retire un joueur sans lui envoyer les messages de depart (utilise en interne,
     * par ex. quand le plugin se desactive ou que l'arene est supprimee).
     */
    public void removeSilently(Player player) {
        UUID uuid = player.getUniqueId();
        if (players.remove(uuid)) {
            kills.remove(uuid);
            deaths.remove(uuid);
            plugin.getScoreboardManager().remove(player);
            restore(player);
        }
    }

    // ================= CYCLE DE VIE DE LA PARTIE =================

    public boolean forceStart(CommandSender sender) {
        if (state != GameState.WAITING && state != GameState.COUNTDOWN) {
            return false;
        }
        if (players.size() < 2) {
            MessageUtil.sendKey(plugin, sender, "not-enough-players");
            return false;
        }
        cancelCountdown();
        startGame();
        return true;
    }

    public void forceStop() {
        cancelCountdown();
        cancelGameTask();
        cancelEndTask();
        for (Player p : getOnlinePlayers()) {
            plugin.getScoreboardManager().remove(p);
            restore(p);
        }
        players.clear();
        kills.clear();
        deaths.clear();
        state = arena.isFullyConfigured() ? GameState.WAITING : GameState.NOT_CONFIGURED;
    }

    private void startLobbyCountdown() {
        if (state == GameState.COUNTDOWN) return;
        state = GameState.COUNTDOWN;
        restartCountdown(plugin.getConfig().getInt("lobby-countdown", 20));
    }

    private void restartCountdown(int seconds) {
        cancelCountdown();
        countdownSecondsLeft = seconds;
        broadcast(MessageUtil.get(plugin, "countdown-start", "time", String.valueOf(seconds)));
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSecondsLeft <= 0) {
                cancelCountdown();
                startGame();
                return;
            }
            if (countdownSecondsLeft <= 5 || countdownSecondsLeft % 10 == 0) {
                broadcast(MessageUtil.get(plugin, "countdown-tick", "time", String.valueOf(countdownSecondsLeft)));
            }
            countdownSecondsLeft--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void startGame() {
        state = GameState.PLAYING;
        gameSecondsLeft = getGameTimeSeconds();
        for (UUID uuid : players) {
            kills.put(uuid, 0);
            deaths.put(uuid, 0);
        }
        for (Player p : getOnlinePlayers()) {
            p.teleport(getRandomSpawn());
            preparePlayerForCombat(p);
        }
        broadcast(MessageUtil.get(plugin, "game-start"));

        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gameSecondsLeft <= 0) {
                endGame();
                return;
            }
            gameSecondsLeft--;
        }, 20L, 20L);
    }

    private void cancelGameTask() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
    }

    private void endGame() {
        if (state == GameState.ENDING) return;
        state = GameState.ENDING;
        cancelGameTask();

        List<UUID> leaderboard = getLeaderboard();
        broadcastEndScreen(leaderboard);

        for (Player p : getOnlinePlayers()) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
        }

        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::resetToWaiting,
                plugin.getConfig().getInt("restart-delay", 12) * 20L);
    }

    private void cancelEndTask() {
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
    }

    /**
     * Renvoie automatiquement au lobby (hors de l'arene) tous les joueurs qui n'ont
     * pas deja clique sur "Rejouer" / "Quitter", puis remet l'arene en attente.
     */
    private void resetToWaiting() {
        for (Player p : getOnlinePlayers()) {
            leave(p);
        }
        state = arena.isFullyConfigured() ? GameState.WAITING : GameState.NOT_CONFIGURED;
    }

    private void broadcastEndScreen(List<UUID> leaderboard) {
        Component header = Component.text("=== Fin de la partie : " + name + " ===", NamedTextColor.GOLD, TextDecoration.BOLD);

        List<Component> lines = new ArrayList<>();
        lines.add(header);
        int rank = 1;
        for (UUID uuid : leaderboard) {
            Player p = Bukkit.getPlayer(uuid);
            String pname = p != null ? p.getName() : "???";
            NamedTextColor color = rank == 1 ? NamedTextColor.GOLD : rank == 2 ? NamedTextColor.GRAY
                    : rank == 3 ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
            lines.add(Component.text("#" + rank + " ", color, TextDecoration.BOLD)
                    .append(Component.text(pname + " ", NamedTextColor.WHITE))
                    .append(Component.text("- " + getKills(uuid) + " kills", NamedTextColor.GRAY)));
            rank++;
            if (rank > 10) break;
        }

        Component replay = Component.text("[Rejouer]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/sc joinrandom"))
                .hoverEvent(Component.text("Rejoindre une nouvelle arene", NamedTextColor.GREEN).asHoverEvent());
        Component separator = Component.text("  /  ", NamedTextColor.DARK_GRAY);
        Component quit = Component.text("[Quitter]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/sc leave"))
                .hoverEvent(Component.text("Retourner au lobby", NamedTextColor.RED).asHoverEvent());
        lines.add(replay.append(separator).append(quit));

        for (Player p : getOnlinePlayers()) {
            for (Component line : lines) {
                p.sendMessage(line);
            }
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    // ================= SPAWNS =================

    /**
     * Choisit un spawn au hasard parmi ceux configures, en essayant d'en trouver un
     * suffisamment eloigne de tous les autres joueurs actuellement dans l'arene, afin
     * d'eviter de faire reapparaitre deux joueurs au meme endroit.
     */
    public Location getRandomSpawn() {
        List<Location> spawns = arena.getSpawns();
        if (spawns.isEmpty()) {
            return arena.getLobbySpawn();
        }
        double safeRadius = plugin.getConfig().getInt("spawn-safe-radius", 4);
        List<Location> shuffled = new ArrayList<>(spawns);
        java.util.Collections.shuffle(shuffled, ThreadLocalRandom.current());

        for (Location candidate : shuffled) {
            boolean safe = true;
            for (Player p : getOnlinePlayers()) {
                if (p.getLocation().getWorld().equals(candidate.getWorld())
                        && p.getLocation().distanceSquared(candidate) < safeRadius * safeRadius) {
                    safe = false;
                    break;
                }
            }
            if (safe) {
                return candidate;
            }
        }
        // Aucun spawn n'est totalement libre : on en prend un au hasard quand meme.
        return shuffled.get(0);
    }

    // ================= COMBAT =================

    /**
     * Enregistre un kill : incremente les compteurs, diffuse le message et replace
     * la victime sur un nouveau spawn aleatoire.
     */
    public void registerKill(Player killer, Player victim) {
        if (killer != null && !killer.equals(victim)) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);
        }
        deaths.merge(victim.getUniqueId(), 1, Integer::sum);

        String killerName = killer != null ? killer.getName() : victim.getName();
        broadcast(MessageUtil.get(plugin, "kill", "killer", killerName, "victim", victim.getName(),
                "kills", String.valueOf(killer != null ? getKills(killer.getUniqueId()) : 0)));
    }

    // ================= PREPARATION DES JOUEURS =================

    private void prepareForLobbyOrGame(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.getActivePotionEffects().forEach(eff -> player.removePotionEffect(eff.getType()));
        if (state == GameState.PLAYING) {
            preparePlayerForCombat(player);
        }
    }

    public void preparePlayerForCombat(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.getActivePotionEffects().forEach(eff -> player.removePotionEffect(eff.getType()));
        equipKit(player);
    }

    public void equipKit(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setItem(0, com.shootcraft.plugin.util.ItemUtil.buildWand());
        inv.setItem(1, com.shootcraft.plugin.util.ItemUtil.buildSpeedBoostItem());
        inv.setItem(8, com.shootcraft.plugin.util.ItemUtil.buildLeaveItem());
    }

    private void restore(Player player) {
        PlayerSnapshot snap = snapshots.remove(player.getUniqueId());
        if (snap != null) {
            snap.restore(player);
        } else {
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void broadcast(String message) {
        for (Player p : getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }

    private void broadcastExcept(Player except, String message) {
        for (Player p : getOnlinePlayers()) {
            if (!p.equals(except)) {
                p.sendMessage(message);
            }
        }
    }

    // ================= PERSISTANCE =================

    public void saveArenaConfig() {
        FileConfiguration config = new YamlConfiguration();
        arena.saveToConfig(config);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder l'arene '" + name + "' : " + e.getMessage());
        }
    }

    public void loadArenaConfig() {
        if (!configFile.exists()) {
            state = GameState.NOT_CONFIGURED;
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        arena.loadFromConfig(config);
        state = arena.isFullyConfigured() ? GameState.WAITING : GameState.NOT_CONFIGURED;
    }

    /**
     * A appeler apres toute modification manuelle (setlobby, setspawn, ...) pour que
     * l'etat de l'arene reflete immediatement sa configuration.
     */
    public void refreshState() {
        if (state == GameState.NOT_CONFIGURED && arena.isFullyConfigured()) {
            state = GameState.WAITING;
        } else if (state == GameState.WAITING && !arena.isFullyConfigured()) {
            state = GameState.NOT_CONFIGURED;
        }
        saveArenaConfig();
    }

    /**
     * Petite structure interne : snapshot de l'etat d'un joueur avant qu'il ne
     * rejoigne l'arene, pour tout lui restaurer proprement a la sortie.
     */
    private static class PlayerSnapshot {
        Location location;
        ItemStack[] contents;
        ItemStack[] armor;
        GameMode gameMode;
        double health;
        int food;

        static PlayerSnapshot capture(Player p) {
            PlayerSnapshot s = new PlayerSnapshot();
            s.location = p.getLocation().clone();
            s.contents = p.getInventory().getContents().clone();
            s.armor = p.getInventory().getArmorContents().clone();
            s.gameMode = p.getGameMode();
            s.health = p.getHealth();
            s.food = p.getFoodLevel();
            return s;
        }

        void restore(Player p) {
            p.getInventory().clear();
            p.getInventory().setContents(contents);
            p.getInventory().setArmorContents(armor);
            p.setGameMode(gameMode);
            p.teleport(location);
            double maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            p.setHealth(Math.min(health, maxHealth));
            p.setFoodLevel(food);
            p.getActivePotionEffects().forEach(eff -> p.removePotionEffect(eff.getType()));
        }
    }
}

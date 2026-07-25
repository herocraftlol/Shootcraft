package com.shootcraft.plugin.commands;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.Arena;
import com.shootcraft.plugin.game.ArenaManager;
import com.shootcraft.plugin.game.CuboidRegion;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gere la commande /sc et tous ses sous-arguments.
 *
 *   /sc create <nom>                       - cree une nouvelle arene vide
 *   /sc delete <nom>                       - supprime une arene
 *   /sc list                               - liste toutes les arenes
 *   /sc arenas                             - ouvre le GUI de selection d'arene
 *   /sc setlobby <nom>                     - definit le lobby (a ta position)
 *   /sc addspawn <nom>                     - ajoute un spawn a ta position
 *   /sc delspawn <nom> <index>             - supprime le spawn #index
 *   /sc setgamezone <nom> <pos1|pos2>      - definit la zone de jeu (garde-fou anti-vide)
 *   /sc removegamezone <nom>               - retire la zone de jeu
 *   /sc setminplayers <nom> <nombre>       - min de joueurs (0 = valeur globale)
 *   /sc setmaxplayers <nom> <nombre>       - max de joueurs (0 = valeur globale)
 *   /sc settime <nom> <secondes>           - duree de partie (0 = valeur globale)
 *   /sc join <nom>                         - rejoindre une arene
 *   /sc joinrandom                         - rejoindre une arene au hasard
 *   /sc leave                              - quitter l'arene en cours
 *   /sc start <nom>                        - forcer le demarrage
 *   /sc stop <nom>                         - forcer l'arret
 *   /sc info <nom>                         - infos sur une arene
 */
public class ShootCraftCommand implements CommandExecutor, TabCompleter {

    private final ShootCraftPlugin plugin;

    /** Coin 1 en attente pour la zone de jeu, par nom d'arene. */
    private final Map<String, Location> pendingGameZoneCorner1 = new HashMap<>();

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "list", "arenas", "setlobby", "addspawn", "delspawn",
            "setgamezone", "removegamezone", "setminplayers", "setmaxplayers", "settime",
            "join", "joinrandom", "leave", "start", "stop", "info", "help"
    );

    public ShootCraftCommand(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "arenas" -> handleArenasGui(sender);
            case "setlobby" -> handleSetLobby(sender, args);
            case "addspawn" -> handleAddSpawn(sender, args);
            case "delspawn" -> handleDelSpawn(sender, args);
            case "setgamezone" -> handleSetGameZone(sender, args);
            case "removegamezone" -> handleRemoveGameZone(sender, args);
            case "setminplayers" -> handleSetMinPlayers(sender, args);
            case "setmaxplayers" -> handleSetMaxPlayers(sender, args);
            case "settime" -> handleSetTime(sender, args);
            case "join" -> handleJoin(sender, args);
            case "joinrandom" -> handleJoinRandom(sender);
            case "leave" -> handleLeave(sender);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    // ================= GESTION DES ARENES (ADMIN) =================

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc create <nom>");
            return;
        }
        boolean created = plugin.getArenaManager().create(args[1]);
        MessageUtil.send(sender, created
                ? "&aArene '" + args[1] + "' creee. Configure-la avec /sc setlobby " + args[1] + ", etc."
                : "&cUne arene '" + args[1] + "' existe deja.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc delete <nom>");
            return;
        }
        boolean deleted = plugin.getArenaManager().delete(args[1]);
        MessageUtil.send(sender, deleted ? "&aArene '" + args[1] + "' supprimee." : "&cAucune arene '" + args[1] + "' trouvee.");
    }

    private void handleList(CommandSender sender) {
        ArenaManager am = plugin.getArenaManager();
        Set<String> names = am.getNames();
        if (names.isEmpty()) {
            MessageUtil.send(sender, "&7Aucune arene configuree. Utilise /sc create <nom> pour en creer une.");
            return;
        }
        MessageUtil.send(sender, "&8&m----------&r &bArenes ShootCraft &8&m----------");
        for (String name : names) {
            GameManager gm = am.get(name);
            MessageUtil.send(sender, "&e" + name + " &7- Etat: &f" + gm.getState()
                    + " &7- Joueurs: &f" + gm.getPlayerCount() + "/" + gm.getMaxPlayers()
                    + " &7- Configuree: " + (gm.getArena().isFullyConfigured() ? "&aOui" : "&cNon"));
        }
    }

    private void handleArenasGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cCette commande est reservee aux joueurs.");
            return;
        }
        plugin.getArenaGUI().open(player);
    }

    // ================= SETUP (ADMIN) =================

    private GameManager resolveArena(CommandSender sender, String[] args, int nameIndex) {
        if (args.length <= nameIndex) {
            return null;
        }
        GameManager gm = plugin.getArenaManager().get(args[nameIndex]);
        if (gm == null) {
            MessageUtil.sendKey(plugin, sender, "arena-not-found", "arena", args[nameIndex]);
        }
        return gm;
    }

    private void handleSetLobby(CommandSender sender, String[] args) {
        if (!requireAdminPlayer(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc setlobby <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        Player player = (Player) sender;
        gm.getArena().setLobbySpawn(player.getLocation());
        gm.refreshState();
        MessageUtil.send(sender, "&aLobby de l'arene '" + args[1] + "' defini a ta position.");
    }

    private void handleAddSpawn(CommandSender sender, String[] args) {
        if (!requireAdminPlayer(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc addspawn <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        Player player = (Player) sender;
        int index = gm.getArena().addSpawn(player.getLocation());
        gm.refreshState();
        MessageUtil.send(sender, "&aSpawn #" + index + " ajoute a l'arene '" + args[1] + "' (a ta position).");
    }

    private void handleDelSpawn(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            MessageUtil.send(sender, "&cUsage: /sc delspawn <nom> <index>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        int index;
        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cIndex invalide.");
            return;
        }
        boolean removed = gm.getArena().removeSpawn(index);
        gm.refreshState();
        MessageUtil.send(sender, removed ? "&aSpawn #" + index + " supprime." : "&cAucun spawn #" + index + " sur cette arene.");
    }

    private void handleSetGameZone(CommandSender sender, String[] args) {
        if (!requireAdminPlayer(sender)) return;
        if (args.length < 3) {
            MessageUtil.send(sender, "&cUsage: /sc setgamezone <nom> <pos1|pos2>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        Player player = (Player) sender;
        String point = args[2].toLowerCase(Locale.ROOT);
        String key = args[1].toLowerCase(Locale.ROOT);

        if (point.equals("pos1")) {
            pendingGameZoneCorner1.put(key, player.getLocation());
            MessageUtil.send(sender, "&aCoin 1 de la zone de jeu enregistre. Utilise /sc setgamezone " + args[1] + " pos2 pour l'autre coin.");
        } else if (point.equals("pos2")) {
            Location c1 = pendingGameZoneCorner1.get(key);
            if (c1 == null) {
                MessageUtil.send(sender, "&cDefinis d'abord le coin 1 avec /sc setgamezone " + args[1] + " pos1.");
                return;
            }
            gm.getArena().setGameZone(new CuboidRegion(c1, player.getLocation()));
            pendingGameZoneCorner1.remove(key);
            gm.refreshState();
            MessageUtil.send(sender, "&aZone de jeu definie pour l'arene '" + args[1] + "'.");
        } else {
            MessageUtil.send(sender, "&cUsage: /sc setgamezone <nom> <pos1|pos2>");
        }
    }

    private void handleRemoveGameZone(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc removegamezone <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        gm.getArena().setGameZone(null);
        gm.refreshState();
        MessageUtil.send(sender, "&aZone de jeu retiree pour l'arene '" + args[1] + "'.");
    }

    private void handleSetMinPlayers(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            MessageUtil.send(sender, "&cUsage: /sc setminplayers <nom> <nombre>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        int value = parseIntOrDefault(args[2], -1);
        if (value < 0) {
            MessageUtil.send(sender, "&cNombre invalide.");
            return;
        }
        gm.getArena().setMinPlayers(value);
        gm.refreshState();
        MessageUtil.send(sender, "&aMin de joueurs pour '" + args[1] + "' : " + (value == 0 ? "valeur globale" : value));
    }

    private void handleSetMaxPlayers(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            MessageUtil.send(sender, "&cUsage: /sc setmaxplayers <nom> <nombre>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        int value = parseIntOrDefault(args[2], -1);
        if (value < 0) {
            MessageUtil.send(sender, "&cNombre invalide.");
            return;
        }
        gm.getArena().setMaxPlayers(value);
        gm.refreshState();
        MessageUtil.send(sender, "&aMax de joueurs pour '" + args[1] + "' : " + (value == 0 ? "valeur globale" : value));
    }

    private void handleSetTime(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            MessageUtil.send(sender, "&cUsage: /sc settime <nom> <secondes>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        int value = parseIntOrDefault(args[2], -1);
        if (value < 0) {
            MessageUtil.send(sender, "&cNombre invalide.");
            return;
        }
        gm.getArena().setGameTimeSeconds(value);
        gm.refreshState();
        MessageUtil.send(sender, "&aDuree de partie pour '" + args[1] + "' : " + (value == 0 ? "valeur globale" : value + "s"));
    }

    // ================= JOUER =================

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cCette commande est reservee aux joueurs.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc join <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        gm.join(player);
    }

    private void handleJoinRandom(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cCette commande est reservee aux joueurs.");
            return;
        }
        GameManager gm = plugin.getArenaManager().findBestArenaForRandomJoin();
        if (gm == null) {
            MessageUtil.sendKey(plugin, player, "no-arena-available");
            return;
        }
        gm.join(player);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cCette commande est reservee aux joueurs.");
            return;
        }
        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm == null) {
            MessageUtil.sendKey(plugin, player, "not-in-game");
            return;
        }
        gm.leave(player);
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc start <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        boolean started = gm.forceStart(sender);
        if (started) {
            MessageUtil.send(sender, "&aPartie demarree sur '" + args[1] + "'.");
        }
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc stop <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        gm.forceStop();
        MessageUtil.send(sender, "&aPartie arretee sur '" + args[1] + "'.");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /sc info <nom>");
            return;
        }
        GameManager gm = resolveArena(sender, args, 1);
        if (gm == null) return;
        Arena arena = gm.getArena();
        MessageUtil.send(sender, "&8&m----------&r &b" + gm.getName() + " &8&m----------");
        MessageUtil.send(sender, "&7Etat: &f" + gm.getState());
        MessageUtil.send(sender, "&7Configuree: " + (arena.isFullyConfigured() ? "&aOui" : "&cNon"));
        MessageUtil.send(sender, "&7Lobby: " + (arena.getLobbySpawn() != null ? "&aOK" : "&cNon defini"));
        MessageUtil.send(sender, "&7Spawns: &f" + arena.getSpawnCount());
        MessageUtil.send(sender, "&7Zone de jeu: " + (arena.getGameZone() != null ? "&aOK" : "&7Non definie"));
        MessageUtil.send(sender, "&7Joueurs: &f" + gm.getPlayerCount() + "/" + gm.getMaxPlayers()
                + " &7(min: &f" + gm.getMinPlayers() + "&7)");
        MessageUtil.send(sender, "&7Duree de partie: &f" + gm.getGameTimeSeconds() + "s");
    }

    // ================= UTILITAIRES =================

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("shootcraft.admin")) {
            MessageUtil.sendKey(plugin, sender, "no-permission");
            return false;
        }
        return true;
    }

    private boolean requireAdminPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.send(sender, "&cCette commande est reservee aux joueurs.");
            return false;
        }
        return requireAdmin(sender);
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, "&8&m----------&r &b&lShootCraft &8&m----------");
        MessageUtil.send(sender, "&e/sc arenas &7- Ouvre le menu des arenes");
        MessageUtil.send(sender, "&e/sc join <nom> &7- Rejoindre une arene");
        MessageUtil.send(sender, "&e/sc joinrandom &7- Rejoindre une arene au hasard");
        MessageUtil.send(sender, "&e/sc leave &7- Quitter la partie en cours");
        MessageUtil.send(sender, "&e/sc info <nom> &7- Infos sur une arene");
        if (sender.hasPermission("shootcraft.admin")) {
            MessageUtil.send(sender, "&c/sc create|delete <nom> &7- Gerer les arenes");
            MessageUtil.send(sender, "&c/sc setlobby|addspawn|delspawn <nom> &7- Configurer les points");
            MessageUtil.send(sender, "&c/sc setgamezone|removegamezone <nom> &7- Zone de jeu");
            MessageUtil.send(sender, "&c/sc setminplayers|setmaxplayers|settime <nom> <n> &7- Reglages");
            MessageUtil.send(sender, "&c/sc start|stop <nom> &7- Forcer le demarrage/arret");
        }
    }

    // ================= TAB COMPLETION =================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (List.of("delete", "setlobby", "addspawn", "delspawn", "setgamezone", "removegamezone",
                    "setminplayers", "setmaxplayers", "settime", "join", "start", "stop", "info").contains(sub)) {
                return filter(new ArrayList<>(plugin.getArenaManager().getNames()), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setgamezone")) {
            return filter(List.of("pos1", "pos2"), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }
}

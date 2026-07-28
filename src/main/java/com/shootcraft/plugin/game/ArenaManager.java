package com.shootcraft.plugin.game;

import com.shootcraft.plugin.ShootCraftPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Registre central de toutes les arenes ShootCraft configurees sur le serveur.
 * Chaque arene nommee est pilotee par sa propre instance de GameManager, ce qui
 * permet plusieurs parties independantes et simultanees.
 */
public class ArenaManager {

    private final ShootCraftPlugin plugin;
    private final Map<String, GameManager> arenas = new LinkedHashMap<>();
    private final File registryFile;

    public ArenaManager(ShootCraftPlugin plugin) {
        this.plugin = plugin;
        File arenasDir = new File(plugin.getDataFolder(), "arenas");
        if (!arenasDir.exists()) {
            arenasDir.mkdirs();
        }
        this.registryFile = new File(arenasDir, "arenas.yml");
    }

    public void loadAll() {
        YamlConfiguration registry = YamlConfiguration.loadConfiguration(registryFile);
        List<String> names = registry.getStringList("arenas");
        for (String name : names) {
            createOrLoad(name);
        }
    }

    private void saveRegistry() {
        YamlConfiguration registry = new YamlConfiguration();
        registry.set("arenas", new ArrayList<>(arenas.keySet()));
        try {
            registry.save(registryFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder la liste des arenes : " + e.getMessage());
        }
    }

    public boolean create(String name) {
        String normalized = normalize(name);
        if (arenas.containsKey(normalized)) {
            return false;
        }
        createOrLoad(normalized);
        saveRegistry();
        return true;
    }

    private void createOrLoad(String name) {
        GameManager gm = new GameManager(plugin, name);
        gm.loadArenaConfig();
        arenas.put(name, gm);
    }

    public boolean delete(String name) {
        String normalized = normalize(name);
        GameManager gm = arenas.remove(normalized);
        if (gm == null) {
            return false;
        }
        gm.forceStop();
        saveRegistry();
        return true;
    }

    public GameManager get(String name) {
        if (name == null) return null;
        return arenas.get(normalize(name));
    }

    public boolean exists(String name) {
        return arenas.containsKey(normalize(name));
    }

    public Collection<GameManager> getAll() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(arenas.keySet());
    }

    public void saveAll() {
        for (GameManager gm : arenas.values()) {
            gm.saveArenaConfig();
        }
    }

    public void stopAll() {
        for (GameManager gm : arenas.values()) {
            gm.forceStop();
        }
    }

    public GameManager findArenaOf(Player player) {
        for (GameManager gm : arenas.values()) {
            if (gm.isPlaying(player)) {
                return gm;
            }
        }
        return null;
    }

    /**
     * Selectionne la meilleure arene disponible pour une jointure aleatoire
     * (/sc joinrandom). Priorite aux arenes qui ont deja des joueurs en attente
     * (pour completer une partie plutot que d'en ouvrir une vide), puis a une
     * arene en cours de partie encore ouverte aux nouveaux arrivants, sinon une
     * arene vide au hasard.
     */
    public GameManager findBestArenaForRandomJoin() {
        List<GameManager> waitingWithPlayers = new ArrayList<>();
        List<GameManager> waitingEmpty = new ArrayList<>();
        List<GameManager> playingOpen = new ArrayList<>();

        for (GameManager gm : arenas.values()) {
            if (!gm.getArena().isFullyConfigured()) continue;
            if (gm.getPlayerCount() >= gm.getMaxPlayers()) continue;

            switch (gm.getState()) {
                case WAITING, COUNTDOWN -> {
                    if (gm.getPlayerCount() > 0) waitingWithPlayers.add(gm);
                    else waitingEmpty.add(gm);
                }
                case PLAYING -> playingOpen.add(gm);
                default -> { }
            }
        }

        if (!waitingWithPlayers.isEmpty()) {
            return pickRandom(waitingWithPlayers);
        }
        if (!playingOpen.isEmpty()) {
            return pickRandom(playingOpen);
        }
        if (!waitingEmpty.isEmpty()) {
            return pickRandom(waitingEmpty);
        }
        return null;
    }

    private GameManager pickRandom(List<GameManager> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}

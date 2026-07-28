package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Fait reapparaitre les joueurs abattus sur un point de spawn aleatoire de leur
 * arene (au lieu du point de respawn du monde), puis leur redonne le kit et un
 * etat propre (vie/faim/effets) une fois le respawn effectif.
 */
public class PlayerRespawnListener implements Listener {

    private final ShootCraftPlugin plugin;

    public PlayerRespawnListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm == null) return;

        event.setRespawnLocation(gm.getRandomSpawn());

        Bukkit.getScheduler().runTask(plugin, () -> {
            GameManager stillIn = plugin.getArenaManager().findArenaOf(player);
            if (stillIn != null) {
                stillIn.preparePlayerForCombat(player);
            }
        });
    }
}

package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Si un joueur se deconnecte pendant qu'il est engage dans une arene, on le
 * retire proprement de la partie (sans lui envoyer de message puisqu'il n'est
 * plus la pour le lire).
 */
public class PlayerConnectionListener implements Listener {

    private final ShootCraftPlugin plugin;

    public PlayerConnectionListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm != null) {
            gm.removeSilently(player);
        }
    }
}

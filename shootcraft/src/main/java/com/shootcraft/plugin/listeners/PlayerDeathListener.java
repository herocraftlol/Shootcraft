package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Les kills/points sont deja comptabilises directement par WandListener au
 * moment du tir. Ce listener se contente de "nettoyer" la mort declenchee par
 * le baton : pas de message de mort par defaut, pas de perte d'inventaire/XP,
 * puis respawn instantane (sans passer par l'ecran de mort) sur un nouveau
 * point d'apparition aleatoire.
 */
public class PlayerDeathListener implements Listener {

    private final ShootCraftPlugin plugin;

    public PlayerDeathListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        GameManager gm = plugin.getArenaManager().findArenaOf(victim);
        if (gm == null || gm.getState() != GameState.PLAYING) {
            return;
        }

        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        victim.spigot().respawn();
    }
}

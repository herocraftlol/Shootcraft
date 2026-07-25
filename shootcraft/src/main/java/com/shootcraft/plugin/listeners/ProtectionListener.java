package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.Arena;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Protege les joueurs et la map pendant une partie ShootCraft :
 * - seul le baton magique peut tuer (toute autre source de degats est annulee)
 * - pas de construction/destruction, pas de faim, pas de drop d'item
 * - si une zone de jeu (gamezone) est configuree, un joueur qui en sort est
 *   automatiquement replace sur un spawn (garde-fou anti-chute dans le vide)
 */
public class ProtectionListener implements Listener {

    private final ShootCraftPlugin plugin;

    public ProtectionListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean inArena(Player player) {
        return plugin.getArenaManager().findArenaOf(player) != null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (inArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (inArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (inArena(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (inArena(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (inArena(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (inArena(player) && event.getClickedInventory().equals(player.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm == null || gm.getState() != GameState.PLAYING) return;

        Arena arena = gm.getArena();
        if (arena.getGameZone() == null) return;

        if (!arena.isInGameZone(player.getLocation())) {
            player.teleport(gm.getRandomSpawn());
        }
    }
}

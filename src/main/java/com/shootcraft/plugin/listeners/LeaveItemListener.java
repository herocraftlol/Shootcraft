package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Permet de quitter la partie en cliquant sur l'item dedie du kit (lit rouge).
 */
public class LeaveItemListener implements Listener {

    private final ShootCraftPlugin plugin;

    public LeaveItemListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!ItemUtil.isLeaveItem(event.getItem())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm != null) {
            gm.leave(player);
        }
    }
}

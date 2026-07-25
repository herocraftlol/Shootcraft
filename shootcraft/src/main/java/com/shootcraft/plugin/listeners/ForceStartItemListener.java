package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import com.shootcraft.plugin.util.ItemUtil;
import com.shootcraft.plugin.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Item reserve aux admins, disponible uniquement dans la salle d'attente
 * (diamant en premier slot de la hotbar) : permet de forcer le demarrage de
 * la partie sans attendre le compte a rebours ni le nombre minimum de
 * joueurs configure.
 */
public class ForceStartItemListener implements Listener {

    private final ShootCraftPlugin plugin;

    public ForceStartItemListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!ItemUtil.isForceStartItem(event.getItem())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("shootcraft.admin")) {
            MessageUtil.sendKey(plugin, player, "no-permission");
            return;
        }

        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm == null) {
            return;
        }
        if (gm.getState() != GameState.WAITING && gm.getState() != GameState.COUNTDOWN) {
            // La partie est deja lancee (ou terminee) : l'item n'a plus lieu d'etre.
            return;
        }

        gm.forceStart(player);
    }
}

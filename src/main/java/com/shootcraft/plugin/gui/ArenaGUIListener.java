package com.shootcraft.plugin.gui;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ArenaGUIListener implements Listener {

    private final ShootCraftPlugin plugin;

    public ArenaGUIListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ArenaGUI.GUI_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        player.closeInventory();

        if (ArenaGUI.isRandomButton(slot)) {
            GameManager gm = plugin.getArenaManager().findBestArenaForRandomJoin();
            if (gm == null) {
                MessageUtil.sendKey(plugin, player, "no-arena-available");
                return;
            }
            gm.join(player);
            return;
        }

        String arenaName = plugin.getArenaGUI().getArenaNameAt(slot);
        if (arenaName == null) return;

        GameManager gm = plugin.getArenaManager().get(arenaName);
        if (gm != null) {
            gm.join(player);
        }
    }
}

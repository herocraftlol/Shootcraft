package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import com.shootcraft.plugin.util.ItemUtil;
import com.shootcraft.plugin.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gere le "turbo" : clic droit sur l'item dedie -> boost de vitesse temporaire,
 * rechargeable toutes les X secondes, avec un bruit de lancement de feu
 * d'artifice au moment de l'activation.
 */
public class SpeedBoostListener implements Listener {

    private final ShootCraftPlugin plugin;

    /** Prochain instant (ms) ou le turbo sera de nouveau disponible, par joueur. */
    private final Map<UUID, Long> nextAvailable = new HashMap<>();

    public SpeedBoostListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!ItemUtil.isSpeedBoost(event.getItem())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm == null || gm.getState() != GameState.PLAYING || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        long now = System.currentTimeMillis();
        Long ready = nextAvailable.get(player.getUniqueId());
        if (ready != null && now < ready) {
            long remainingMs = ready - now;
            long remainingSec = (remainingMs + 999) / 1000;
            MessageUtil.sendKey(plugin, player, "speed-boost-cooldown", "time", String.valueOf(remainingSec));
            return;
        }

        int durationSeconds = plugin.getConfig().getInt("speed-boost.duration-seconds", 5);
        int cooldownSeconds = plugin.getConfig().getInt("speed-boost.cooldown-seconds", 10);
        int amplifier = plugin.getConfig().getInt("speed-boost.speed-amplifier", 1);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationSeconds * 20, amplifier, false, true, true));
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        MessageUtil.sendKey(plugin, player, "speed-boost-activate");

        nextAvailable.put(player.getUniqueId(), now + cooldownSeconds * 1000L);
    }
}

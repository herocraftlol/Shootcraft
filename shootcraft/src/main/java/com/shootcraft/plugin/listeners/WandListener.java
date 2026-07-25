package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import com.shootcraft.plugin.util.ItemUtil;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gere le tir du baton magique : clic droit -> rayon quasi-instantane (trace
 * de particules de nuage + son de type "xp"), qui tue et marque un point si il
 * touche un autre joueur de l'arene.
 */
public class WandListener implements Listener {

    private final ShootCraftPlugin plugin;

    /** Anti-spam : dernier tir (ms) par joueur. */
    private final Map<UUID, Long> lastShot = new HashMap<>();

    public WandListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!ItemUtil.isWand(event.getItem())) return;

        event.setCancelled(true);
        Player shooter = event.getPlayer();

        GameManager gm = plugin.getArenaManager().findArenaOf(shooter);
        if (gm == null || gm.getState() != GameState.PLAYING) {
            return;
        }
        if (shooter.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("wand.cooldown-ms", 300);
        long now = System.currentTimeMillis();
        Long last = lastShot.get(shooter.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return;
        }
        lastShot.put(shooter.getUniqueId(), now);

        shoot(shooter, gm);
    }

    private void shoot(Player shooter, GameManager gm) {
        World world = shooter.getWorld();
        Location eye = shooter.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double maxDistance = plugin.getConfig().getDouble("wand.max-distance", 120);

        RayTraceResult entityHit = world.rayTraceEntities(eye, direction, maxDistance, 0.35, entity ->
                entity instanceof Player target
                        && !target.equals(shooter)
                        && gm.isPlaying(target)
                        && target.getGameMode() != GameMode.SPECTATOR);

        RayTraceResult blockHit = world.rayTraceBlocks(eye, direction, maxDistance, FluidCollisionMode.NEVER, true);

        Player victim = null;
        Location endPoint;

        double entityDist = entityHit != null ? entityHit.getHitPosition().distance(eye.toVector()) : Double.MAX_VALUE;
        double blockDist = blockHit != null ? blockHit.getHitPosition().distance(eye.toVector()) : Double.MAX_VALUE;

        if (entityHit != null && entityDist <= blockDist) {
            victim = (Player) entityHit.getHitEntity();
            endPoint = entityHit.getHitPosition().toLocation(world);
        } else if (blockHit != null) {
            endPoint = blockHit.getHitPosition().toLocation(world);
        } else {
            endPoint = eye.clone().add(direction.clone().multiply(maxDistance));
        }

        drawBeam(world, eye, endPoint);
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        if (victim != null) {
            victim.playSound(victim.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.7f);
            world.spawnParticle(Particle.CLOUD, victim.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.02);
            gm.registerKill(shooter, victim);
            victim.setHealth(0.0);
        }
    }

    /**
     * Trace la ligne du rayon d'un seul coup (quasi-instantane du point de vue du
     * joueur) en semant des particules de nuage tout le long du trajet.
     */
    private void drawBeam(World world, Location start, Location end) {
        double step = plugin.getConfig().getDouble("wand.particle-step", 0.35);
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length < 0.01) return;
        direction.normalize();

        for (double travelled = 0; travelled <= length; travelled += step) {
            Location point = start.clone().add(direction.clone().multiply(travelled));
            world.spawnParticle(Particle.CLOUD, point, 1, 0, 0, 0, 0);
        }
    }
}

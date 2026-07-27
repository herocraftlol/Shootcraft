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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere le tir du baton magique : clic droit -> rayon quasi-instantane (trace
 * de particules de nuage + bruit de feu d'artifice au lancement), qui tue et
 * marque un point pour chaque joueur touche. Le rayon traverse les joueurs
 * alignes : plusieurs victimes d'un seul tir declenchent un double/triple/
 * quadruple kill. Un bruit de type "xp" est joue a chaque joueur abattu.
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
        Vector eyeVec = eye.toVector();
        double maxDistance = plugin.getConfig().getDouble("wand.max-distance", 120);
        double hitRadius = plugin.getConfig().getDouble("wand.hit-radius", 0.6);

        // Le bloc solide le plus proche stoppe toujours le rayon.
        RayTraceResult blockHit = world.rayTraceBlocks(eye, direction, maxDistance, FluidCollisionMode.NEVER, true);
        double blockDist = blockHit != null ? blockHit.getHitPosition().distance(eyeVec) : maxDistance;

        // On cherche TOUS les joueurs alignes avec le rayon avant ce point de blocage
        // (permet les double/triple/quadruple kills sur des joueurs en file).
        List<Player> victims = new ArrayList<>();
        for (Player target : gm.getOnlinePlayers()) {
            if (target.equals(shooter) || target.getGameMode() == GameMode.SPECTATOR) continue;

            Vector toTarget = target.getEyeLocation().toVector().subtract(eyeVec);
            double projection = toTarget.dot(direction);
            if (projection < -0.5 || projection > blockDist + 0.5) continue;

            Vector closestPointOnRay = direction.clone().multiply(projection);
            double perpendicularDist = toTarget.clone().subtract(closestPointOnRay).length();
            if (perpendicularDist <= hitRadius) {
                victims.add(target);
            }
        }
        victims.sort(Comparator.comparingDouble(p -> p.getEyeLocation().toVector().subtract(eyeVec).dot(direction)));

        Location endPoint = blockHit != null ? blockHit.getHitPosition().toLocation(world)
                : eye.clone().add(direction.clone().multiply(maxDistance));

        drawBeam(world, eye, endPoint);
        world.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);

        for (Player victim : victims) {
            world.playSound(victim.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            world.spawnParticle(Particle.CLOUD, victim.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.02);
            gm.registerKill(shooter, victim);
            victim.setHealth(0.0);
        }

        if (victims.size() >= 2) {
            gm.announceMultiKill(shooter, victims.size());
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

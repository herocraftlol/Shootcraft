package com.shootcraft.plugin.listeners;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import com.shootcraft.plugin.util.ItemUtil;
import com.shootcraft.plugin.util.MessageUtil;
import org.bukkit.Bukkit;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere le baton magique dans son ensemble :
 *   - Clic droit -> tir d'un rayon quasi-instantane (particules de nuage +
 *     bruit de feu d'artifice au lancement), qui tue et marque un point pour
 *     chaque joueur touche. Le rayon traverse les joueurs alignes : plusieurs
 *     victimes d'un seul tir declenchent un double/triple/quadruple kill. Un
 *     bruit de type "xp" est joue a chaque joueur abattu.
 *   - Clic gauche -> boost de vitesse ephemere, rechargeable, avec un petit
 *     bruit de levier a l'activation.
 *
 * Minecraft ne route pas toujours les clics vers PlayerInteractEvent : viser
 * directement un joueur tres proche declenche PlayerInteractEntityEvent (clic
 * droit) ou EntityDamageByEntityEvent (clic gauche, une "attaque") a la place.
 * On ecoute donc les trois evenements pour que le tir/boost fonctionne de
 * maniere fiable a toute distance, y compris a bout portant.
 */
public class WandListener implements Listener {

    private final ShootCraftPlugin plugin;

    /** Anti-spam : dernier tir (ms) par joueur. */
    private final Map<UUID, Long> lastShot = new HashMap<>();
    /** Prochain instant (ms) ou le boost sera de nouveau disponible, par joueur. */
    private final Map<UUID, Long> nextBoostAvailable = new HashMap<>();

    public WandListener(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ================= DETECTION DES CLICS =================

    /** Clic droit/gauche dans le vide ou sur un bloc. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!rightClick && !leftClick) return;
        if (!ItemUtil.isWand(event.getItem())) return;

        event.setCancelled(true);
        if (rightClick) {
            attemptShoot(event.getPlayer());
        } else {
            attemptBoost(event.getPlayer());
        }
    }

    /** Clic droit directement sur un joueur (Minecraft n'envoie pas de PlayerInteractEvent dans ce cas). */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!ItemUtil.isWand(player.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);
        attemptShoot(player);
    }

    /** Clic gauche (attaque) directement sur un joueur ou une autre entite vivante proche. */
    @EventHandler
    public void onAttackEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!ItemUtil.isWand(player.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);
        attemptBoost(player);
    }

    // ================= TIR =================

    private void attemptShoot(Player shooter) {
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
        double tolerance = plugin.getConfig().getDouble("wand.hitbox-tolerance", 0.15);

        // Le bloc solide le plus proche stoppe toujours le rayon.
        RayTraceResult blockHit = world.rayTraceBlocks(eye, direction, maxDistance, FluidCollisionMode.NEVER, true);
        double blockDist = blockHit != null ? blockHit.getHitPosition().distance(eyeVec) : maxDistance;

        // On teste le rayon contre la hitbox REELLE de chaque joueur (et non plus
        // une simple distance a un point), pour une precision fidele a la visee du
        // joueur, y compris a bout portant (cas ou les yeux du tireur sont deja
        // dans la hitbox de la cible).
        List<Player> victims = new ArrayList<>();
        Map<UUID, Double> hitDistances = new HashMap<>();

        for (Player target : gm.getOnlinePlayers()) {
            if (target.equals(shooter) || target.getGameMode() == GameMode.SPECTATOR) continue;

            BoundingBox box = target.getBoundingBox().expand(tolerance);
            double distance;
            if (box.contains(eyeVec)) {
                distance = 0.0;
            } else {
                RayTraceResult hit = box.rayTrace(eyeVec, direction, blockDist);
                if (hit == null) continue;
                distance = hit.getHitPosition().distance(eyeVec);
            }
            victims.add(target);
            hitDistances.put(target.getUniqueId(), distance);
        }
        victims.sort(Comparator.comparingDouble(p -> hitDistances.get(p.getUniqueId())));

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

    // ================= BOOST DE VITESSE (CLIC GAUCHE) =================

    private void attemptBoost(Player player) {
        GameManager gm = plugin.getArenaManager().findArenaOf(player);
        if (gm == null || gm.getState() != GameState.PLAYING) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        long now = System.currentTimeMillis();
        Long ready = nextBoostAvailable.get(player.getUniqueId());
        if (ready != null && now < ready) {
            long remainingSec = (ready - now + 999) / 1000;
            MessageUtil.sendKey(plugin, player, "speed-boost-cooldown", "time", String.valueOf(remainingSec));
            return;
        }

        int durationSeconds = plugin.getConfig().getInt("speed-boost.duration-seconds", 5);
        int cooldownSeconds = plugin.getConfig().getInt("speed-boost.cooldown-seconds", 10);
        int amplifier = plugin.getConfig().getInt("speed-boost.speed-amplifier", 3);

        // Remplace temporairement la vitesse permanente par le niveau du boost.
        player.removePotionEffect(PotionEffectType.SPEED);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationSeconds * 20, amplifier, false, true, true));
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.7f, 1.4f);
        MessageUtil.sendKey(plugin, player, "speed-boost-activate");

        nextBoostAvailable.put(player.getUniqueId(), now + cooldownSeconds * 1000L);

        // Une fois le boost termine, on revient a la vitesse de base (si la partie
        // est toujours en cours pour ce joueur).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            GameManager stillIn = plugin.getArenaManager().findArenaOf(player);
            if (stillIn != null && stillIn.getState() == GameState.PLAYING) {
                stillIn.applyBaseSpeed(player);
            }
        }, durationSeconds * 20L);
    }
}

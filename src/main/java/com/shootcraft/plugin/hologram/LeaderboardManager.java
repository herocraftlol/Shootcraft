package com.shootcraft.plugin.hologram;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Leaderboards holographiques "summonables" pour ShootCraft, sur le meme
 * principe que le CategoryLeaderboardManager d'HikaBrain : un admin se place
 * ou il veut et invoque un classement top 10 (fait de faux joueurs Armor
 * Stand invisibles empiles verticalement), pour une categorie de statistiques
 * donnee. Le classement se rafraichit automatiquement.
 *
 * Categories disponibles : victoires, kills, kd (ratio kills/morts), parties
 * jouees. Persistance dans leaderboards.yml (position + echelle par categorie).
 */
public class LeaderboardManager {

    private static final int TOP_SIZE = 10;
    private static final double DEFAULT_SCALE = 1.0;
    private static final double LINE_GAP = 0.27;

    private final ShootCraftPlugin plugin;
    private final File cfgFile;
    private final NamespacedKey pdcKey;

    private final Map<Category, Location> locations = new EnumMap<>(Category.class);
    private final Map<Category, List<UUID>> lineEntities = new EnumMap<>(Category.class);
    private final Map<Category, Double> scales = new EnumMap<>(Category.class);

    private BukkitTask refreshTask;

    public LeaderboardManager(ShootCraftPlugin plugin) {
        this.plugin = plugin;
        this.cfgFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        this.pdcKey = new NamespacedKey(plugin, "shootcraft_leaderboard");
    }

    // ================= CHARGEMENT / SAUVEGARDE =================

    public void loadConfig() {
        if (!cfgFile.exists()) {
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        ConfigurationSection root = cfg.getConfigurationSection("locations");
        if (root == null) {
            return;
        }

        for (Category category : Category.values()) {
            ConfigurationSection section = root.getConfigurationSection(category.getKey());
            if (section == null) continue;

            String worldName = section.getString("world");
            World world = worldName != null ? Bukkit.getServer().getWorld(worldName) : null;
            if (world == null) {
                plugin.getLogger().warning("[ShootCraft] Leaderboard '" + category.getKey()
                        + "' : monde '" + worldName + "' introuvable au demarrage.");
                continue;
            }

            purgeOrphanArmorStands(world);

            Location loc = new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
            double scale = section.getDouble("scale", DEFAULT_SCALE);

            locations.put(category, loc);
            scales.put(category, scale);
            forceLoadChunk(loc);
            buildLines(category);
        }

        if (!locations.isEmpty()) {
            startRefreshTask();
        }
    }

    private void saveConfig() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<Category, Location> entry : locations.entrySet()) {
            Category category = entry.getKey();
            Location loc = entry.getValue();
            String path = "locations." + category.getKey();
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getX());
            cfg.set(path + ".y", loc.getY());
            cfg.set(path + ".z", loc.getZ());
            cfg.set(path + ".scale", scales.getOrDefault(category, DEFAULT_SCALE));
        }
        try {
            File parent = cfgFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            cfg.save(cfgFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[ShootCraft] Impossible de sauvegarder leaderboards.yml : " + e.getMessage());
        }
    }

    // ================= API PUBLIQUE =================

    public boolean isSpawned(Category category) {
        return locations.containsKey(category);
    }

    public void spawn(Category category, Location location) {
        despawnEntities(category);
        locations.put(category, location.clone());
        scales.putIfAbsent(category, DEFAULT_SCALE);
        forceLoadChunk(location);
        saveConfig();
        buildLines(category);
        startRefreshTask();
    }

    public void setScale(Category category, double scale) {
        scales.put(category, scale);
        saveConfig();
        if (locations.containsKey(category)) {
            buildLines(category);
        }
    }

    public boolean despawn(Category category) {
        if (!locations.containsKey(category)) {
            return false;
        }
        despawnEntities(category);
        locations.remove(category);
        saveConfig();
        if (locations.isEmpty()) {
            stopRefreshTask();
        }
        return true;
    }

    public void despawnAll() {
        for (Category category : new ArrayList<>(locations.keySet())) {
            despawnEntities(category);
        }
        locations.clear();
        stopRefreshTask();
    }

    public void refreshAll() {
        for (Category category : locations.keySet()) {
            buildLines(category);
        }
    }

    // ================= CONSTRUCTION DES HOLOGRAMMES =================

    private void despawnEntities(Category category) {
        List<UUID> ids = lineEntities.remove(category);
        if (ids == null) return;
        for (UUID uuid : ids) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private void buildLines(Category category) {
        despawnEntities(category);

        Location base = locations.get(category);
        if (base == null) return;
        World world = base.getWorld();
        if (world == null) return;

        double scale = scales.getOrDefault(category, DEFAULT_SCALE);
        StatsManager statsManager = plugin.getStatsManager();
        Comparator<StatsManager.PlayerStats> comparator = category.getComparator();

        List<Map.Entry<UUID, StatsManager.PlayerStats>> top = statsManager.getTopPlayers(TOP_SIZE, comparator);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(category.getTitle(), category.getColor(), TextDecoration.BOLD));

        if (top.isEmpty()) {
            lines.add(Component.text("Aucune donnee pour le moment.", NamedTextColor.GRAY));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, StatsManager.PlayerStats> entry : top) {
                lines.add(buildLine(category, rank, entry.getValue()));
                rank++;
            }
        }

        List<UUID> ids = new ArrayList<>();
        double lineGap = LINE_GAP * scale;
        Location cursor = base.clone();
        for (Component line : lines) {
            ArmorStand stand = spawnStand(world, cursor, line, scale);
            ids.add(stand.getUniqueId());
            cursor = cursor.clone().add(0, -lineGap, 0);
        }
        lineEntities.put(category, ids);
    }

    private Component buildLine(Category category, int rank, StatsManager.PlayerStats stats) {
        Component prefix = rankPrefix(rank);
        Component name = Component.text(stats.getName() + "  ", NamedTextColor.WHITE);
        Component value = switch (category) {
            case KILLS -> Component.text(stats.getKills() + " kills", NamedTextColor.RED);
            case VICTOIRES -> Component.text(stats.getGamesWon() + " victoires", NamedTextColor.YELLOW)
                    .append(Component.text("  (" + stats.getGamesPlayed() + " parties)", NamedTextColor.GRAY));
            case KD -> Component.text("K/D: " + stats.getKD(), NamedTextColor.GREEN)
                    .append(Component.text("  (" + stats.getKills() + "K / " + stats.getDeaths() + "M)", NamedTextColor.GRAY));
            case PARTIES -> Component.text(stats.getGamesPlayed() + " parties", NamedTextColor.AQUA);
        };
        return prefix.append(name).append(value);
    }

    private Component rankPrefix(int rank) {
        NamedTextColor color = switch (rank) {
            case 1 -> NamedTextColor.GOLD;
            case 2 -> NamedTextColor.GRAY;
            case 3 -> NamedTextColor.DARK_RED;
            default -> NamedTextColor.WHITE;
        };
        return Component.text("#" + rank + " ", color, TextDecoration.BOLD);
    }

    private ArmorStand spawnStand(World world, Location loc, Component text, double scale) {
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.customName(text);
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setCollidable(false);
        stand.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, "leaderboard");
        applyScale(stand, scale);
        return stand;
    }

    private void applyScale(LivingEntity entity, double scale) {
        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_SCALE);
        if (attribute != null) {
            attribute.setBaseValue(scale);
        }
    }

    /**
     * Supprime les Armor Stands "orphelins" (marques par notre PersistentDataContainer
     * mais non suivis en memoire, typiquement laisses par une session precedente avant
     * un redemarrage) avant de reconstruire un hologramme propre.
     */
    private void purgeOrphanArmorStands(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getType() == EntityType.ARMOR_STAND
                    && entity.getPersistentDataContainer().has(pdcKey, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    private void forceLoadChunk(Location loc) {
        Chunk chunk = loc.getChunk();
        chunk.addPluginChunkTicket(plugin);
    }

    private void startRefreshTask() {
        if (refreshTask != null) return;
        long intervalTicks = plugin.getConfig().getLong("leaderboard.refresh-interval-seconds", 30) * 20L;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, intervalTicks, intervalTicks);
    }

    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /**
     * A appeler depuis onDisable() : arrete simplement le rafraichissement
     * automatique. Les Armor Stands restent visibles ; ils seront nettoyes et
     * reconstruits proprement au prochain demarrage (voir purgeOrphanArmorStands).
     */
    public void shutdown() {
        stopRefreshTask();
    }

    /**
     * Categories de classement disponibles pour /sc leaderboard.
     */
    public enum Category {
        VICTOIRES("victoires", "TOP VICTOIRES", NamedTextColor.GOLD,
                Comparator.comparingInt(StatsManager.PlayerStats::getGamesWon)),
        KILLS("kills", "TOP KILLS", NamedTextColor.RED,
                Comparator.comparingInt(StatsManager.PlayerStats::getKills)),
        KD("kd", "TOP K/D", NamedTextColor.LIGHT_PURPLE,
                Comparator.comparingDouble(StatsManager.PlayerStats::getKD)),
        PARTIES("parties", "TOP PARTIES JOUEES", NamedTextColor.AQUA,
                Comparator.comparingInt(StatsManager.PlayerStats::getGamesPlayed));

        private final String key;
        private final String title;
        private final NamedTextColor color;
        private final Comparator<StatsManager.PlayerStats> comparator;

        Category(String key, String title, NamedTextColor color, Comparator<StatsManager.PlayerStats> comparator) {
            this.key = key;
            this.title = title;
            this.color = color;
            this.comparator = comparator;
        }

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public NamedTextColor getColor() {
            return color;
        }

        public Comparator<StatsManager.PlayerStats> getComparator() {
            return comparator;
        }

        public static Category fromKey(String key) {
            if (key == null) return null;
            for (Category category : values()) {
                if (category.key.equalsIgnoreCase(key)) {
                    return category;
                }
            }
            return null;
        }
    }
}

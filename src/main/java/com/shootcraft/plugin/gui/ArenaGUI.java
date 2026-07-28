package com.shootcraft.plugin.gui;

import com.shootcraft.plugin.ShootCraftPlugin;
import com.shootcraft.plugin.game.GameManager;
import com.shootcraft.plugin.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GUI d'inventaire listant toutes les arenes ShootCraft disponibles, avec un
 * bouton dedie pour en rejoindre une au hasard.
 *
 * Structure (54 slots) :
 *   - Lignes 1 a 5 : une icone par arene (45 arenes max affichees)
 *   - Ligne 6 entiere : bouton "Rejoindre une arene aleatoire"
 */
public class ArenaGUI {

    public static final String GUI_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "\u2694 Arenes ShootCraft";
    private static final int GUI_SIZE = 54;
    private static final int RANDOM_ROW_START = 45;

    private final ShootCraftPlugin plugin;

    public ArenaGUI(ShootCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(org.bukkit.entity.Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        Collection<GameManager> allArenas = plugin.getArenaManager().getAll();

        int slot = 0;
        for (GameManager gm : allArenas) {
            if (slot >= RANDOM_ROW_START) break;
            inv.setItem(slot, buildArenaItem(gm));
            slot++;
        }

        ItemStack filler = buildFiller();
        for (int i = slot; i < RANDOM_ROW_START; i++) {
            inv.setItem(i, filler);
        }

        ItemStack randomBtn = buildRandomButton(allArenas);
        for (int i = RANDOM_ROW_START; i < GUI_SIZE; i++) {
            inv.setItem(i, randomBtn);
        }

        return inv;
    }

    private ItemStack buildArenaItem(GameManager gm) {
        String name = gm.getName();
        GameState state = gm.getState();
        int current = gm.getPlayerCount();
        int max = gm.getMaxPlayers();

        Material mat;
        String displayName;
        String statusLine;
        ChatColor statusColor;

        if (!gm.getArena().isFullyConfigured() || state == GameState.NOT_CONFIGURED) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            displayName = ChatColor.GRAY + "" + ChatColor.BOLD + "\u2716 " + capitalize(name);
            statusLine = ChatColor.GRAY + "Non configuree";
            statusColor = ChatColor.GRAY;
        } else if (state == GameState.PLAYING) {
            mat = Material.ORANGE_STAINED_GLASS_PANE;
            displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "\u2694 " + capitalize(name);
            statusLine = ChatColor.GOLD + "Partie en cours";
            statusColor = ChatColor.GOLD;
        } else if (state == GameState.ENDING) {
            mat = Material.RED_STAINED_GLASS_PANE;
            displayName = ChatColor.RED + "" + ChatColor.BOLD + "\u2716 " + capitalize(name);
            statusLine = ChatColor.RED + "Partie terminee";
            statusColor = ChatColor.RED;
        } else if (current >= max) {
            mat = Material.ORANGE_STAINED_GLASS_PANE;
            displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "\u26A0 " + capitalize(name);
            statusLine = ChatColor.GOLD + "Pleine";
            statusColor = ChatColor.GOLD;
        } else {
            mat = Material.LIME_STAINED_GLASS_PANE;
            displayName = ChatColor.GREEN + "" + ChatColor.BOLD + "\u2714 " + capitalize(name);
            statusLine = ChatColor.GREEN + "Disponible";
            statusColor = ChatColor.GREEN;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Joueurs : " + statusColor + current + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + max);
        lore.add(ChatColor.GRAY + "Statut  : " + statusLine);
        if (state == GameState.PLAYING) {
            lore.add(ChatColor.GRAY + "Temps restant : " + ChatColor.AQUA + formatTime(gm.getTimeLeft()));
        }
        lore.add("");

        boolean joinable = gm.getArena().isFullyConfigured()
                && (state == GameState.WAITING || state == GameState.COUNTDOWN || state == GameState.PLAYING)
                && current < max;

        lore.add(joinable ? ChatColor.YELLOW + "\u25B6 Clique pour rejoindre !" : ChatColor.RED + "\u2716 Indisponible");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildRandomButton(Collection<GameManager> allArenas) {
        long joinableCount = allArenas.stream()
                .filter(gm -> gm.getArena().isFullyConfigured()
                        && (gm.getState() == GameState.WAITING || gm.getState() == GameState.COUNTDOWN || gm.getState() == GameState.PLAYING)
                        && gm.getPlayerCount() < gm.getMaxPlayers())
                .count();

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "\u2726 Rejoindre une arene aleatoire");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Tu seras envoye dans une arene");
        lore.add(ChatColor.GRAY + "disponible, en priorite celles");
        lore.add(ChatColor.GRAY + "qui ont deja des joueurs.");
        lore.add("");
        if (joinableCount > 0) {
            lore.add(ChatColor.GREEN + "" + joinableCount + " arene(s) disponible(s)");
            lore.add("");
            lore.add(ChatColor.YELLOW + "\u25B6 Clique pour jouer !");
        } else {
            lore.add(ChatColor.RED + "Aucune arene disponible pour le moment.");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public String getArenaNameAt(int slot) {
        if (slot < 0 || slot >= RANDOM_ROW_START) return null;
        List<GameManager> list = new ArrayList<>(plugin.getArenaManager().getAll());
        if (slot >= list.size()) return null;
        return list.get(slot).getName();
    }

    public static boolean isRandomButton(int slot) {
        return slot >= RANDOM_ROW_START && slot < GUI_SIZE;
    }
}

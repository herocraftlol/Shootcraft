package com.shootcraft.plugin.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Construit les objets du kit ShootCraft : le baton magique, l'objet de turbo
 * (boost de vitesse) et l'item pour quitter la partie.
 *
 * On identifie ces items par leur nom affiche + un lore marqueur (pas de NBT
 * custom ici pour rester simple), verifie par WandListener / SpeedBoostListener.
 */
public final class ItemUtil {

    public static final String WAND_NAME = ChatColor.AQUA + "" + ChatColor.BOLD + "Baton Magique";
    public static final String SPEED_NAME = ChatColor.YELLOW + "" + ChatColor.BOLD + "Turbo \u21af";
    public static final String LEAVE_NAME = ChatColor.RED + "" + ChatColor.BOLD + "Quitter la partie";

    private ItemUtil() {
    }

    public static ItemStack buildWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(WAND_NAME);
        meta.setLore(List.of(
                ChatColor.GRAY + "Clic droit pour tirer un rayon",
                ChatColor.GRAY + "magique sur tes adversaires !"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildSpeedBoostItem() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(SPEED_NAME);
        meta.setLore(List.of(
                ChatColor.GRAY + "Clic droit pour activer un boost",
                ChatColor.GRAY + "de vitesse temporaire.",
                ChatColor.DARK_GRAY + "(se recharge automatiquement)"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildLeaveItem() {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(LEAVE_NAME);
        meta.setLore(List.of(ChatColor.GRAY + "Clic droit pour quitter la partie."));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWand(ItemStack item) {
        return hasName(item, WAND_NAME);
    }

    public static boolean isSpeedBoost(ItemStack item) {
        return hasName(item, SPEED_NAME);
    }

    public static boolean isLeaveItem(ItemStack item) {
        return hasName(item, LEAVE_NAME);
    }

    private static boolean hasName(ItemStack item, String name) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && name.equals(meta.getDisplayName());
    }
}

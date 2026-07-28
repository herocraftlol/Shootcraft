package com.shootcraft.plugin.util;

import com.shootcraft.plugin.ShootCraftPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Petite classe utilitaire pour traduire les codes couleur (&) et envoyer des
 * messages formates, en resolvant au passage les cles du fichier config.yml
 * (section "messages") avec remplacement de variables simples (%var%).
 */
public final class MessageUtil {

    private MessageUtil() {
    }

    public static String format(String raw) {
        if (raw == null) return "";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static void send(CommandSender target, String raw) {
        if (raw == null || raw.isEmpty()) return;
        target.sendMessage(format(raw));
    }

    /**
     * Recupere un message dans config.yml sous "messages.<key>", prefixe par
     * "messages.prefix", et remplace les variables %key%=value fournies en paires.
     */
    public static String get(ShootCraftPlugin plugin, String key, String... replacements) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString("messages." + key, key);
        String result = prefix + raw;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return format(result);
    }

    /**
     * Meme chose que get() mais sans le prefixe (utile pour des messages internes
     * a un bloc de texte plus large, comme l'ecran de fin de partie).
     */
    public static String getRaw(ShootCraftPlugin plugin, String key, String... replacements) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        String result = raw;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return format(result);
    }

    public static void sendKey(ShootCraftPlugin plugin, CommandSender target, String key, String... replacements) {
        target.sendMessage(get(plugin, key, replacements));
    }
}

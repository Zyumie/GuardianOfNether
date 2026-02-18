package fr.zyumie.Listener;

import com.google.gson.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionManager implements Listener {

    private static JavaPlugin plugin;
    private static String latestVersion = null;
																// Mettre l'ID  ⬇️⬇️⬇️⬇️
    private static final String URL_API = "https://api.modrinth.com/v2/project/SYzeRHyC/version?version_type=release";

    
    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    
    public static void check(JavaPlugin plugin) {
    	
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
				@SuppressWarnings("deprecation")
				HttpURLConnection con = (HttpURLConnection)
                        new URL(URL_API).openConnection();

                con.setRequestProperty("User-Agent", plugin.getName());

                JsonArray versions = JsonParser
                        .parseReader(new InputStreamReader(con.getInputStream()))
                        .getAsJsonArray();

                if (versions.isEmpty()) return;

                String latest =
                        versions.get(0).getAsJsonObject()
                                .get("version_number").getAsString();

                latestVersion = latest;
                
                String current = plugin.getDescription().getVersion();

                if (!current.equals(latest)) {
                    Bukkit.getLogger().warning(
                            "[GuardianOfNether] Nouvelle version disponible : " +
                                    latest + " (actuelle: " + current + ")"
                    );             
                
                } else {
                    Bukkit.getLogger().info("[GuardienOfNether] Vous utilisez la dernière version !");
                }

            } catch (Exception ignored) {
            }
        });
    }

    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (latestVersion == null) return;

        Player player = event.getPlayer();
        String current = plugin.getDescription().getVersion();

        
        if (player.isOp() && !current.equals(latestVersion)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "[&6Guardien&fOf&4Nether&f] &aNouvelle version disponible : "
                + latestVersion + " (actuelle : " + current + ")\n"
                + "&9https://modrinth.com/plugin/fireballwand"
            ));
        }
    }

	
}

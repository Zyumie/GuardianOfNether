package fr.zyumie.Listener;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionManager {

																// Mettre l'ID  ⬇️⬇️⬇️⬇️
    private static final String URL_API = "https://api.modrinth.com/v2/version/SYzeRHyC";

    public static void check(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
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

                String current = plugin.getDescription().getVersion();

                if (!current.equals(latest)) {
                    Bukkit.getLogger().warning(
                            "[FireBallWand] Nouvelle version disponible : " +
                                    latest + " (actuelle: " + current + ")"
                    );
                } else {
                    Bukkit.getLogger().info("[FireBallWand] Vous utilisez la dernière version de FireBallWand");
                }

            } catch (Exception ignored) {
            }
        });
    }
}

package fr.zyumie.Listener;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

public class VersionManager {

    private static final String PROJECT_ID = "guardianofnether"; // remplacer par ton project Modrinth

    public static String getLatestVersion() {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/" + PROJECT_ID);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            // Retourne la version la plus récente de la release
            return json.get("latest_version").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isUpdateAvailable(String currentVersion) {
        String latest = getLatestVersion();
        if (latest == null) return false;
        return !latest.equals(currentVersion);
    }    
   
    public static void notifyPlayer(Player player, String currentVersion) {
        if (player.hasPermission("guardian.admin") && isUpdateAvailable(currentVersion)) {
            player.sendMessage("§c[GuardianOfNether] Nouvelle version disponible ! §7Actuelle : "
                    + currentVersion + " | Nouvelle : " + getLatestVersion());
            player.sendMessage("§eTélécharge la mise à jour sur ton serveur.");
        }
    }

}

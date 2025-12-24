package fr.zyumie.Listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.entity.Player;

import fr.zyumie.GuardianOfNether.Main;

public class NetherListener implements Listener {

    private final Main plugin;

    public NetherListener(Main plugin) {
        this.plugin = plugin;
    }

    /** Vérifie si le Nether est ouvert */
    private boolean isNetherOpen() {
        if (!plugin.getConfig().getBoolean("Nether.Nether-Close", true)) {
            return true; // Nether-Close = false → toujours ouvert
        }
        return plugin.getConfig().getBoolean("Nether.Boss-Dead", false);
    }

    /** Appelé quand le boss est vaincu */
    public void bossDefeated() {
        plugin.getConfig().set("Nether.Boss-Dead", true);
        plugin.saveConfig();

        Bukkit.broadcastMessage(
            "§6Le Gardien du Nether est vaincu ! Le Nether est maintenant accessible !");
    }

    @EventHandler
    public void onPlayerUsePortal(PlayerPortalEvent event) {
        if (isNetherOpen()) return;

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.sendMessage(
            "§cVous ne pouvez pas entrer dans le Nether tant que le Gardien du Nether n'est pas vaincu !");
    }
}

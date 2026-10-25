package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Bloque l'accès au Nether (portails, véhicules, perles et téléportations)
 * tant que le boss n'a pas été vaincu.
 */
public class NetherListener implements Listener {

    private final Main          plugin;
    private final ConfigManager config;
    private boolean netherUnlocked;

    public NetherListener(Main plugin, ConfigManager config) {
        this.plugin          = plugin;
        this.config          = config;
        this.netherUnlocked  = config.bossDead;
    }

    // ── API publique ─────────────────────────────────────────────

    public void onBossDefeated() {
        netherUnlocked = true;
        config.set("Nether.boss-dead", true);
        Bukkit.broadcastMessage(config.msgNetherOpen);
    }

    public void resetNether() {
        netherUnlocked = false;
        config.set("Nether.boss-dead", false);
        plugin.getLogger().info("[NetherListener] Nether verrouillé à nouveau.");
    }

    public boolean isNetherUnlocked() {
        return netherUnlocked;
    }

    // ── Events de blocage ────────────────────────────────────────

    /**
     * 1. Bloque les portails classiques à pied
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!config.netherClose) return;
        if (netherUnlocked) return;

        Player player = event.getPlayer();
        if (player.hasPermission("guardianofnether.bypass")) return;

        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            player.sendMessage(config.msgNetherBlocked);
        }
    }

    /**
     * 2. Bloque les wagonnets, bateaux ou montures qui entrent dans le Nether avec un joueur
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (!config.netherClose) return;
        if (netherUnlocked) return;

        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {

            // Vérifie si le véhicule transporte des joueurs
            for (Entity passenger : event.getEntity().getPassengers()) {
                if (passenger instanceof Player) {
                    Player player = (Player) passenger;
                    if (!player.hasPermission("guardianofnether.bypass")) {
                        player.sendMessage(config.msgNetherBlocked);
                    }
                }
            }

            // Si c'est le joueur lui-même en tant qu'entité (ex: entité custom)
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (player.hasPermission("guardianofnether.bypass")) return;
                player.sendMessage(config.msgNetherBlocked);
            }

            event.setCancelled(true);
        }
    }

    /**
     * 3. Bloque les perles de l'ender ou téléportations directes vers le Nether
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!config.netherClose) return;
        if (netherUnlocked) return;

        // On cible uniquement les téléportations vers le Nether
        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {

            Player player = event.getPlayer();
            if (player.hasPermission("guardianofnether.bypass")) return;

            // Optionnel : Vous pouvez filtrer par cause si besoin (ex: event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
            // Mais bloquer toute téléportation non autorisée vers le Nether fermé est plus sûr.
            event.setCancelled(true);
            player.sendMessage(config.msgNetherBlocked);
        }
    }
}
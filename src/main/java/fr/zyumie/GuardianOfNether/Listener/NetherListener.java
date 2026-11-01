package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 * tant que le boss n'a pas été vaincu. Réglages dans Nether.yml.
 */
public class NetherListener implements Listener {

    private final Main          plugin;
    private final ConfigManager config;
    private boolean netherUnlocked;

    public NetherListener(Main plugin, ConfigManager config) {
        this.plugin         = plugin;
        this.config         = config;
        this.netherUnlocked = config.bossDead;
    }

    // ── API publique ─────────────────────────────────────────────

    public void onBossDefeated() {
        netherUnlocked = true;
        config.setBossDead(true);
        Bukkit.broadcastMessage(config.msgNetherOpen);
    }

    public void resetNether() {
        netherUnlocked = false;
        config.setBossDead(false);
        plugin.getLogger().info("[NetherListener] Nether verrouillé à nouveau.");
    }

    public boolean isNetherUnlocked() {
        return netherUnlocked;
    }

    // ── Helpers ──────────────────────────────────────────────────

    private boolean isLocked() {
        return config.netherClose && !netherUnlocked;
    }

    private boolean canBypass(Player player) {
        return player.hasPermission(config.netherBypassPermission);
    }

    private boolean leadsToNether(Location to) {
        return to != null
                && to.getWorld() != null
                && to.getWorld().getEnvironment() == World.Environment.NETHER;
    }

    // ── Events de blocage ────────────────────────────────────────

    /** 1. Portails classiques à pied */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!config.netherBlockPortals || !isLocked()) return;

        Player player = event.getPlayer();
        if (canBypass(player)) return;

        if (leadsToNether(event.getTo())) {
            event.setCancelled(true);
            player.sendMessage(config.msgNetherBlocked);
        }
    }

    /** 2. Wagonnets, bateaux, montures (et autres entités) qui entrent dans le Nether */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (!config.netherBlockPortals || !isLocked()) return;
        if (!leadsToNether(event.getTo())) return;

        boolean hasPlayer = false;
        boolean blockedPlayer = false;

        for (Entity passenger : event.getEntity().getPassengers()) {
            if (passenger instanceof Player player) {
                hasPlayer = true;
                if (!canBypass(player)) {
                    blockedPlayer = true;
                    player.sendMessage(config.msgNetherBlocked);
                }
            }
        }

        // Joueur(s) sans bypass à bord → bloqué ; sinon selon block.entities
        if (blockedPlayer || (!hasPlayer && config.netherBlockEntities)) {
            event.setCancelled(true);
        }
    }

    /** 3. Perles de l'ender ou téléportations directes vers le Nether */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!config.netherBlockTeleports || !isLocked()) return;
        if (!leadsToNether(event.getTo())) return;

        Player player = event.getPlayer();
        if (canBypass(player)) return;

        event.setCancelled(true);
        player.sendMessage(config.msgNetherBlocked);
    }
}

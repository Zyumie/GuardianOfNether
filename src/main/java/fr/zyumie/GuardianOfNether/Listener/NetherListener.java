package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * Bloque l'accès au Nether tant que le boss n'a pas été vaincu.
 * L'état est persisté dans config.yml via la clé "Nether.boss-dead".
 */
public class NetherListener implements Listener {

    private final Main          plugin;
    private final ConfigManager config;

    // État en mémoire — évite de lire le fichier à chaque portal
    private boolean netherUnlocked;

    public NetherListener(Main plugin, ConfigManager config) {
        this.plugin          = plugin;
        this.config          = config;
        // On lit l'état persisté au démarrage
        this.netherUnlocked  = config.bossDead;
    }

    // ── API publique ─────────────────────────────────────────────

    /**
     * Appelé par BossListener quand le boss meurt.
     * Déverrouille le Nether et persiste l'état dans config.yml.
     */
    public void onBossDefeated() {
        netherUnlocked = true;
        config.set("Nether.boss-dead", true);
        Bukkit.broadcastMessage(config.msgNetherOpen);
    }

    /**
     * Réinitialise le verrou du Nether (ex: pour un nouveau cycle de jeu).
     * Appelé manuellement via commande admin si besoin.
     */
    public void resetNether() {
        netherUnlocked = false;
        config.set("Nether.boss-dead", false);
        plugin.getLogger().info("[NetherListener] Nether verrouillé à nouveau.");
    }

    public boolean isNetherUnlocked() {
        return netherUnlocked;
    }

    // ── Event ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        // Si le nether-close est désactivé dans la config → on ne bloque rien
        if (!config.netherClose) return;

        // Si le nether est déjà déverrouillé → on laisse passer
        if (netherUnlocked) return;

        // On bloque uniquement les portails vers le Nether
        if (event.getTo() == null) return;
        if (event.getTo().getWorld() == null) return;
        if (event.getTo().getWorld().getEnvironment() != World.Environment.NETHER) return;

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.sendMessage(config.msgNetherBlocked);
    }
}
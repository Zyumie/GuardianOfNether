package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Boss.GuardianBoss;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class BossListener implements Listener {

    private final Main          plugin;
    private final ConfigManager config;
    private final RewardManager rewardManager;
    private final NetherListener netherListener;

    // Référence au boss actif — null si aucun boss n'est spawné
    private GuardianBoss activeBoss;

    public BossListener(Main plugin, ConfigManager config,
                        RewardManager rewardManager, NetherListener netherListener) {
        this.plugin         = plugin;
        this.config         = config;
        this.rewardManager  = rewardManager;
        this.netherListener = netherListener;
    }

    // ── API publique ─────────────────────────────────────────────

    /**
     * Enregistre le boss actif. Appelé depuis SpawnCommand après le spawn.
     */
    public void setActiveBoss(GuardianBoss boss) {
        this.activeBoss = boss;
    }

    public GuardianBoss getActiveBoss() {
        return activeBoss;
    }

    // ── Events ───────────────────────────────────────────────────

    /**
     * Mort du boss — drop, effets, nether, cleanup.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton boss)) return;
        if (!Main.trackedBosses.contains(boss.getUniqueId())) return;

        // On supprime les drops vanilla (os, etc.)
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = boss.getKiller();

        // Drop du plastron au killer
        if (config.giveChestplate && killer != null) {
            ItemStack chestplate = rewardManager.buildChestplate();
            boss.getWorld().dropItemNaturally(boss.getLocation(), chestplate);
            killer.sendMessage(config.msgChestplateReceived);
        }

        // Déblocage du Nether
        if (config.netherClose) {
            netherListener.onBossDefeated();
        }

        // Clear inventaires (tout le monde sauf le killer)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(killer)) continue;
            applyDeathPenalty(p, false);
        }

        // Clear inventaire du killer — on préserve son plastron s'il en a un
        if (killer != null) {
            applyDeathPenalty(killer, true);
        }

        // Suppression des villageois
        if (config.killVillagers) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Villager) entity.remove();
                }
            }
        }

        // Cleanup
        Main.trackedBosses.remove(boss.getUniqueId());
        activeBoss = null;
    }

    /**
     * Quand un joueur frappe le boss, on l'enregistre comme engagé.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHitBoss(EntityDamageByEntityEvent event) {
        if (activeBoss == null) return;
        if (!(event.getEntity() instanceof WitherSkeleton boss)) return;
        if (!Main.trackedBosses.contains(boss.getUniqueId())) return;
        if (!(event.getDamager() instanceof Player player)) return;

        activeBoss.playerEngaged(player);
    }

    /**
     * Quand un joueur rejoint, on l'ajoute à la bossbar si un boss est actif.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (activeBoss != null && activeBoss.isAlive()) {
            activeBoss.addToBossBar(event.getPlayer());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * Applique les pénalités de mort du boss à un joueur.
     * @param preserveChestplate si true, le plastron du Gardien est préservé après le clear
     */
    private void applyDeathPenalty(Player player, boolean preserveChestplate) {
        // On récupère le plastron AVANT de clear si on doit le préserver
        ItemStack chestplate = preserveChestplate
                ? player.getInventory().getChestplate()
                : null;

        if (config.clearInventory) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            // On remet le plastron s'il faut le préserver
            if (preserveChestplate && chestplate != null
                    && rewardManager.isGuardianChestplate(chestplate)) {
                player.getInventory().setChestplate(chestplate);
            }
        }

        if (config.clearEnderChest) {
            player.getEnderChest().clear();
        }
    }
}
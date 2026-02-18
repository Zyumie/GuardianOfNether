package fr.zyumie.Listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.*;

import fr.zyumie.GuardianOfNether.ConfigGuardian;
import fr.zyumie.GuardianOfNether.GuardianOfNether;
import fr.zyumie.GuardianOfNether.Main;

public class BossManager implements Listener {

    private final Main plugin;
    private final GuardianOfNether guardian;
    private final NetherManager netherListener;
    private final ConfigGuardian config;

    public BossManager(Main plugin, GuardianOfNether guardian, NetherManager netherListener) {
        this.plugin = plugin;
        this.guardian = guardian;
        this.netherListener = netherListener;
        this.config = new ConfigGuardian(plugin); // charge toutes les options
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton boss) || !Main.trackedBosses.contains(boss.getUniqueId())) return;

        Player killer = event.getEntity().getKiller();

        // Débloquer le Nether si nécessaire
        if (config.netherClose) {
            netherListener.bossDefeated();
        }

        // Gestion des joueurs
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(killer)) continue;

            if (config.clearInventory) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
            }
            if (config.clearEnderChest) {
                p.getEnderChest().clear();
            }
        }

        if (killer != null) {
            if (config.clearInventory) {
                ItemStack chest = killer.getInventory().getChestplate();
                killer.getInventory().clear();
                killer.getInventory().setArmorContents(null);
                killer.getInventory().setChestplate(chest);
            }

            if (config.clearEnderChest) {
                killer.getEnderChest().clear();
            }

            if (config.givePlastron) {
                guardian.dropChestplateForKiller(boss, killer);
            }
        }

        if (config.killVillagers) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Villager villager) {
                        villager.remove();
                    }
                }
            }
        }

        guardian.announceDeath(boss);
        Main.trackedBosses.remove(boss.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (isMySpecialEntity(entity)) {
            NamespacedKey key = new NamespacedKey(plugin, "nostack");
            entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            entity.setCustomNameVisible(false);
            entity.setCustomName("§r");
            entity.setMetadata("NoStack", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton boss) || !Main.trackedBosses.contains(boss.getUniqueId())) return;
        if (event.getDamager() instanceof Player player) {
            guardian.playerHitBoss(player);
        }
    }

    private boolean isMySpecialEntity(LivingEntity entity) {
        return entity.hasMetadata("GuardianOfNether") || entity.getScoreboardTags().contains("boss")
                || (entity.getCustomName() != null && entity.getCustomName().contains("Boss"));
    }

}



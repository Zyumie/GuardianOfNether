package fr.zyumie.GuardianOfNether;

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

public class BossListener implements Listener {

    private final Main plugin;
    private final GuardianOfNether guardian;

    public BossListener(Main plugin, GuardianOfNether guardian) {
        this.plugin = plugin;
        this.guardian = guardian;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton boss) || !Main.trackedBosses.contains(boss.getUniqueId())) {
			return;
		}

        Player killer = event.getEntity().getKiller();

        if (guardian != null) {
            if (killer != null) {
                guardian.dropChestplateForKiller(boss, killer);
            }

            guardian.announceDeath(boss);
            
            plugin.saveDefaultConfig(); // pour s'assurer que le config est présent
            plugin.reloadConfig();
            
            boolean clearInventory = plugin.getConfig().getBoolean("Clear-Stuff.clear-inventory", true);
            boolean clearEnderChest = plugin.getConfig().getBoolean("Clear-Stuff.clear-enderchest", true);

            for (Player p : Bukkit.getOnlinePlayers()) {


                if (p.equals(killer)) {
					continue;
				}
                if (clearInventory) {
                	p.getInventory().clear();
                	p.getInventory().setArmorContents(null);
                }
                if (clearEnderChest) {
                p.getEnderChest().clear();

                }
            }

            if (killer != null) {
                ItemStack chest = killer.getInventory().getChestplate();

                if (clearInventory) {
                    killer.getInventory().clear();
                    killer.getInventory().setArmorContents(null);
                    killer.getInventory().setChestplate(chest);
                }

                if (clearEnderChest) {
                    killer.getEnderChest().clear();
                }
            }


            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Villager villager) {
                        villager.remove();
                    }
                }
            }
        }

        Main.trackedBosses.remove(boss.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (isMySpecialEntity(entity)) {
            NamespacedKey key = new NamespacedKey(plugin, "nostack");
            entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
            entity.setCustomNameVisible(false);
            entity.setCustomName("§r");
            entity.setMetadata("NoStack", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton boss) || !Main.trackedBosses.contains(boss.getUniqueId())) {
			return;
		}
        if (event.getDamager() instanceof Player player) {
            guardian.playerHitBoss(player);
        }
    }

    private boolean isMySpecialEntity(LivingEntity entity) {
        return entity.hasMetadata("GuardianOfNether") ||
               entity.getScoreboardTags().contains("boss") ||
               (entity.getCustomName() != null && entity.getCustomName().contains("Boss"));
    }
}

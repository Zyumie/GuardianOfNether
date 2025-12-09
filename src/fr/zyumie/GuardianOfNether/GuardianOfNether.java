package fr.zyumie.GuardianOfNether;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GuardianOfNether {

    private final Plugin plugin;
    private final List<WitherSkeleton> minions = new ArrayList<>();
    private final Set<Player> engagedPlayers = new HashSet<>();
    private Location spawnPoint;

    public GuardianOfNether(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawnBoss(Location loc, Player spawnedBy) {
        spawnPoint = loc.clone();
        WitherSkeleton boss = loc.getWorld().spawn(loc, WitherSkeleton.class);
        boss.setCustomName("§4§lGardien du Nether");
        boss.setCustomNameVisible(true);
        boss.setPersistent(true);

        // Stats initiales
        boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(250);
        boss.setHealth(250);
        boss.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(15);
        boss.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(5);
        boss.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);

        Main.trackedBosses.add(boss.getUniqueId());
        giveBossArmor(boss);

        // BossBar
        BossBar bossBar = Bukkit.createBossBar(boss.getCustomName(), BarColor.RED, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!boss.isValid()) { bossBar.removeAll(); cancel(); return; }
                bossBar.setProgress(boss.getHealth() / boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Spawn initial des sbires
        spawnMinions(loc, 5);
        announceSpawn(boss);

        // Scheduler central
        new BukkitRunnable() {
            int tickCounter = 0;
            @Override
            public void run() {
                if (!boss.isValid()) { cancel(); announceDeath(boss); return; }

                // Restriction au rayon de 20 blocs
                if (boss.getLocation().distanceSquared(spawnPoint) > 20*20) {
                    boss.teleport(spawnPoint);
                }

                double health = boss.getHealth();
                double damageMultiplier;
                int regenAmount;
                int regenInterval; // ticks

                if (health > 150) { // Phase 1
                    damageMultiplier = 1.0;
                    regenAmount = 4;
                    regenInterval = 20*10;
                } else if (health > 75) { // Phase 2
                    damageMultiplier = 1.5;
                    regenAmount = 5;
                    regenInterval = 20*10;
                } else { // Phase 3
                    damageMultiplier = 3.0;
                    regenAmount = 4;
                    regenInterval = 20*10;
                }

                tickCounter++;

                if (tickCounter % (regenInterval/20) == 0) {
                    boss.setHealth(Math.min(health + regenAmount, boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue()));
                    boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0,1,0), 5);
                }

                engagedPlayers.removeIf(p -> !p.isOnline() || p.isDead() || p.getLocation().distanceSquared(spawnPoint) > 20*20);

                // Attaques corps à corps + flammes
                for (Player target : engagedPlayers) {
                    if (target.getLocation().distanceSquared(boss.getLocation()) <= 10*10) {
                        target.damage(10*damageMultiplier, boss);
                        target.setFireTicks(60);

                        int points = 16;
                        double radius = 1.5;
                        for (int i = 0; i < points; i++) {
                            double angle = 2*Math.PI*i/points;
                            double x = boss.getLocation().getX() + radius*Math.cos(angle);
                            double z = boss.getLocation().getZ() + radius*Math.sin(angle);
                            Location flameLoc = new Location(boss.getWorld(), x, boss.getLocation().getY()+1, z);
                            boss.getWorld().spawnParticle(Particle.FLAME, flameLoc, 1,0,0,0,0);
                        }
                    }
                }

                // Attaque des sbires
                minions.removeIf(m -> !m.isValid());
                for (WitherSkeleton minion : minions) {
                    if (!minion.isValid()) {
						continue;
					}
                    for (org.bukkit.entity.Entity e : minion.getNearbyEntities(1.5,2,1.5)) {
                        if (e instanceof Player p && minion.getLocation().distance(p.getLocation()) <= 2) {
                            p.damage(minion.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue(), minion);
                            minion.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0,1,0), 5);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin,0L,20L);

        // Spawn automatique des sbires toutes les 60 sec
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!boss.isValid()) {
					return;
				}
                spawnMinions(spawnPoint, 3);
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    public void playerHitBoss(Player player) {
        engagedPlayers.add(player);
    }

    // --- Armure / Minions / Drop / Spawn / Death ---
    private void giveBossArmor(WitherSkeleton boss) {

    	NamespacedKey key = new NamespacedKey("stackmob", "ignore");

        boss.getEquipment().setHelmet(createEnchantedArmor(Material.DIAMOND_HELMET, "§6Casque of GuardianNether"));
        boss.getEquipment().setChestplate(GuardianItems.getGuardianChest()); // même que la commande
        boss.getEquipment().setLeggings(createEnchantedArmor(Material.DIAMOND_LEGGINGS, "§6Jambières of GuardianNether"));
        boss.getEquipment().setBoots(createEnchantedArmor(Material.DIAMOND_BOOTS, "§6Bottes of GuardianNether"));

        boss.getEquipment().setHelmetDropChance(0f);
        boss.getEquipment().setChestplateDropChance(0f);
        boss.getEquipment().setLeggingsDropChance(0f);
        boss.getEquipment().setBootsDropChance(0f);
        boss.addScoreboardTag("no_stack");
        boss.setMetadata("NoStack", new FixedMetadataValue(plugin, true));
        boss.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

    }

    private ItemStack createEnchantedArmor(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.addEnchant(Enchantment.PROTECTION, 10, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private void spawnMinions(Location loc, int count) {

    	NamespacedKey key = new NamespacedKey("stackmob", "ignore");

        for (int i=0;i<count;i++) {
            WitherSkeleton minion = loc.getWorld().spawn(loc.clone().add(Math.random()*5-2.5,0,Math.random()*5-2.5), WitherSkeleton.class);
            minion.setCustomName("§6Minion du Gardien");
            minion.setCustomNameVisible(true);
            minion.setPersistent(true);

            minion.getAttribute(Attribute.MAX_HEALTH).setBaseValue(50);
            minion.setHealth(50);
            minion.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(8);
            minion.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(10);
            minion.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);

            // Armure fer
            minion.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            minion.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            minion.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            minion.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            minion.getEquipment().setHelmetDropChance(0f);
            minion.getEquipment().setChestplateDropChance(0f);
            minion.getEquipment().setLeggingsDropChance(0f);
            minion.getEquipment().setBootsDropChance(0f);
            minion.addScoreboardTag("no_stack");
            minion.setMetadata("NoStack", new FixedMetadataValue(plugin, true));
            minion.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            minions.add(minion);
        }
    }

    public void dropChestplateForKiller(WitherSkeleton boss, Player killer) {
        ItemStack chest = GuardianItems.getGuardianChest(); // même que la commande
        boss.getWorld().dropItemNaturally(boss.getLocation(), chest);
        killer.sendMessage("§6Vous avez reçu la chestplate du Gardien !");
    }

    public void announceSpawn(WitherSkeleton boss) {
        String bossName = boss.getCustomName();
        Bukkit.broadcastMessage("§c" + bossName + " est apparu !");
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(bossName,"§6Préparez-vous...",5,40,5);
            p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,2f,1f);
        }
    }

    public void announceDeath(WitherSkeleton boss) {
        String bossName = boss.getCustomName();
        Bukkit.broadcastMessage("§c" + bossName + "§f: Je vais faire disparaitre tout ce que vous aimer avant de Mourir !");
        Bukkit.broadcastMessage("§c" + bossName + " a été vaincu !");
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(),Sound.ENTITY_WITHER_DEATH,2f,1f);
        }
    }
}

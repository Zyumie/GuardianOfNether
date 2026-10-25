package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Main;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Gère toutes les attaques spéciales épiques du Guardian of the Nether.
 *
 * Phase 1 — Cercle de flammes passif (dans GuardianBoss)
 * Phase 2 — Sol en magma temporaire + shockwave de transition
 * Phase 3 — Pluie d'épées netherite + nova de feu + shockwave de transition
 */
public class AttackManager {

    private final Main plugin;
    private final WitherSkeleton boss;

    // Blocs transformés en magma → sauvegardés pour restauration
    private final Map<Location, BlockData> savedBlocks = new HashMap<>();

    // Cooldowns en ticks
    private static final int MAGMA_COOLDOWN      = 20 * 15; // 15s
    private static final int SWORD_RAIN_COOLDOWN = 20 * 20; // 20s
    private static final int NOVA_COOLDOWN       = 20 * 30; // 30s

    private int magmaTick = 0;
    private int swordTick = 0;
    private int novaTick  = 0;

    private static final double ENGAGE_RADIUS = 60.0;
    private final Random random = new Random();

    public AttackManager(Main plugin, WitherSkeleton boss) {
        this.plugin = plugin;
        this.boss   = boss;
    }

    // ── Tick central ─────────────────────────────────────────────

    /**
     * Appelé à chaque tick du scheduler principal de GuardianBoss.
     */
    public void tick(int currentPhase) {
        if (!boss.isValid()) return;

        magmaTick++;
        swordTick++;
        novaTick++;

        List<Player> nearby = getNearbyPlayers();
        if (nearby.isEmpty()) return;

        switch (currentPhase) {
            case 2 -> {
                if (magmaTick >= MAGMA_COOLDOWN) {
                    magmaTick = 0;
                    magmaFloor();
                }
            }
            case 3 -> {
                if (magmaTick >= MAGMA_COOLDOWN) {
                    magmaTick = 0;
                    magmaFloor();
                }
                if (swordTick >= SWORD_RAIN_COOLDOWN) {
                    swordTick = 0;
                    swordRain(nearby);
                }
                if (novaTick >= NOVA_COOLDOWN) {
                    novaTick = 0;
                    fireNova();
                }
            }
        }
    }

    // ── Shockwave de transition de phase ─────────────────────────

    /**
     * Appelé par PhaseManager lors d'un changement de phase.
     * Explosion visuelle + knockback sur tous les joueurs proches.
     */
    public void phaseShockwave(int newPhase) {
        Location center = boss.getLocation();
        World world = center.getWorld();

        // Explosion de particules centrale
        world.spawnParticle(Particle.EXPLOSION,      center, 5);
        world.spawnParticle(Particle.FLAME,          center, 80, 2, 2, 2, 0.15);
        world.spawnParticle(Particle.LAVA,           center, 40, 3, 1, 3, 0);
        world.spawnParticle(Particle.SMOKE,          center, 60, 2, 2, 2, 0.10);

        // Sons
        world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 2f, 0.5f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,    2f, 0.8f);

        // Titre et knockback sur les joueurs proches
        String title    = newPhase == 2 ? "§6§lPhase 2" : "§4§lPhase 3";
        String subtitle = newPhase == 2 ? "§eLe Gardien s'intensifie..." : "§c§lFuyez !";
        float pitch     = newPhase == 3 ? 0.6f : 0.9f;

        for (Player p : getNearbyPlayers()) {
            Vector dir = p.getLocation().toVector()
                .subtract(center.toVector()).normalize().multiply(1.8);
            dir.setY(0.6);
            p.setVelocity(dir);
            p.sendTitle(title, subtitle, 5, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, pitch);
        }

        // Anneau de particules qui s'expanse
        new BukkitRunnable() {
            double radius = 0.5;
            int step = 0;
            @Override
            public void run() {
                if (step++ > 20 || !boss.isValid()) { cancel(); return; }
                int points = Math.max(8, (int)(radius * 8));
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location ring = new Location(world, x, center.getY() + 0.5, z);
                    world.spawnParticle(Particle.FLAME,           ring, 2, 0, 0.2, 0, 0.02);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, ring, 1, 0, 0.1, 0, 0);
                }
                radius += 1.2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ── Attaque 1 : Sol en magma ─────────────────────────────────

    private void magmaFloor() {
        Location center = boss.getLocation();
        World world = center.getWorld();
        int radius = 10;
        List<Location> toRestore = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;

                Location surface = getSurface(world,
                    (int)center.getX() + x,
                    (int)center.getY(),
                    (int)center.getZ() + z);
                if (surface == null) continue;

                Block block = surface.getBlock();
                if (!block.getType().isSolid())                 continue;
                if (block.getType() == Material.MAGMA_BLOCK)    continue;
                if (block.getType() == Material.BEDROCK)        continue;
                if (block.getType() == Material.OBSIDIAN)       continue;

                savedBlocks.put(block.getLocation().clone(), block.getBlockData().clone());
                block.setType(Material.MAGMA_BLOCK, false);
                toRestore.add(block.getLocation().clone());
            }
        }

        // Effets visuels et sonores
        world.spawnParticle(Particle.LAVA,  center.clone().add(0,1,0), 30, radius, 0.5, radius, 0);
        world.spawnParticle(Particle.FLAME, center.clone().add(0,1,0), 20, radius, 0.5, radius, 0.05);
        world.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 2f, 0.7f);
        world.playSound(center, Sound.BLOCK_NETHERRACK_STEP,   2f, 0.5f);

        Bukkit.broadcastMessage("§c§l[Gardien] §cLe sol se transforme en magma !");

        // Restauration entre 60 et 120 secondes
        long delay = 20L * (60 + random.nextInt(61));
        new BukkitRunnable() {
            @Override
            public void run() {
                restoreBlocks(toRestore, world);
            }
        }.runTaskLater(plugin, delay);
    }

    private void restoreBlocks(List<Location> locs, World world) {
        for (Location loc : locs) {
            BlockData original = savedBlocks.remove(loc);
            if (original == null) continue;
            Block block = world.getBlockAt(loc);
            if (block.getType() == Material.MAGMA_BLOCK) {
                block.setBlockData(original, false);
            }
        }
        world.playSound(boss.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.8f);
    }

    // ── Attaque 2 : Pluie d'épées en netherite ───────────────────

    private void swordRain(List<Player> targets) {
        World world = boss.getWorld();
        Bukkit.broadcastMessage("§4§l[Gardien] §cDes épées tombent du ciel !");
        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2f, 0.4f);

        for (Player target : targets) {
            // 3 épées par joueur avec délai entre chaque
            for (int i = 0; i < 3; i++) {
                final int idx = i;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!target.isOnline() || target.isDead()) return;

                        double offsetX = (random.nextDouble() - 0.5) * 4;
                        double offsetZ = (random.nextDouble() - 0.5) * 4;
                        Location spawnLoc = target.getLocation().clone().add(offsetX, 18, offsetZ);

                        launchSwordProjectile(spawnLoc, target.getLocation().clone());
                    }
                }.runTaskLater(plugin, idx * 8L);
            }
        }
    }

    private void launchSwordProjectile(Location from, Location target) {
        World world = from.getWorld();

        // ArmorStand invisible utilisé comme projectile visuel
        ArmorStand sword = (ArmorStand) world.spawnEntity(from, EntityType.ARMOR_STAND);
        sword.setVisible(false);
        sword.setGravity(true);
        sword.setSmall(true);
        sword.setArms(true);
        sword.setInvulnerable(true);
        sword.setPersistent(false);

        // Épée netherite en main
        ItemStack swordItem = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = swordItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§4Épée du Gardien");
            swordItem.setItemMeta(meta);
        }
        sword.getEquipment().setItemInMainHand(swordItem);

        // Vélocité vers la cible
        Vector velocity = target.toVector()
            .subtract(from.toVector())
            .normalize()
            .multiply(1.4);
        sword.setVelocity(velocity);

        // Traînée de particules + détection d'impact
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!sword.isValid()) { cancel(); return; }
                ticks++;

                Location loc = sword.getLocation();

                // Traînée
                world.spawnParticle(Particle.FLAME,      loc, 3, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.CRIT,       loc, 2, 0.1, 0.1, 0.1, 0);

                // Détection impact sol ou timeout 3s
                boolean hitGround = loc.getBlock().getType().isSolid()
                    || loc.clone().subtract(0, 0.5, 0).getBlock().getType().isSolid();

                if (hitGround || ticks > 60) {
                    // Impact
                    world.spawnParticle(Particle.EXPLOSION, loc, 1);
                    world.spawnParticle(Particle.FLAME,     loc, 15, 0.4, 0.2, 0.4, 0.1);
                    world.spawnParticle(Particle.CRIT,      loc, 10, 0.3, 0.3, 0.3, 0.05);
                    world.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 0.5f);
                    world.playSound(loc, Sound.BLOCK_STONE_HIT,         1f, 0.7f);

                    // Dégâts dans un rayon de 2 blocs
                    for (Entity e : world.getNearbyEntities(loc, 2, 2, 2)) {
                        if (e instanceof Player p) {
                            p.damage(8.0, boss);
                            p.setFireTicks(40);
                        }
                    }

                    sword.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Attaque 3 : Nova de feu ───────────────────────────────────

    private void fireNova() {
        Location center = boss.getLocation().clone().add(0, 0.5, 0);
        World world = center.getWorld();

        Bukkit.broadcastMessage("§4§l[Gardien] §c§lNOVA DE FEU !");
        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT,     2f, 0.3f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,  2f, 0.5f);

        new BukkitRunnable() {
            double radius = 0;
            int step = 0;
            @Override
            public void run() {
                if (step++ > 35 || !boss.isValid()) { cancel(); return; }

                int points = Math.max(8, (int)(radius * 6));
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;

                    // Anneau horizontal
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location ring = new Location(world, x, center.getY(), z);
                    world.spawnParticle(Particle.FLAME,           ring, 2, 0, 0.2, 0, 0.05);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, ring, 1, 0, 0.1, 0, 0.02);
                    world.spawnParticle(Particle.LAVA,            ring, 1, 0, 0,   0, 0);

                    // Anneau sphérique vertical
                    double vy = center.getY() + radius * Math.sin(angle);
                    Location sphere = new Location(world, x, vy, center.getZ() + radius * Math.cos(angle));
                    world.spawnParticle(Particle.FLAME, sphere, 1, 0, 0, 0, 0.03);
                }

                // Dégâts quand la vague de feu touche les joueurs
                for (Player p : getNearbyPlayers()) {
                    double dist = p.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 1.5) {
                        p.damage(6.0, boss);
                        p.setFireTicks(60);
                        Vector knockback = p.getLocation().toVector()
                            .subtract(center.toVector()).normalize().multiply(1.2);
                        knockback.setY(0.4);
                        p.setVelocity(knockback);
                    }
                }

                radius += 0.9;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Helpers ──────────────────────────────────────────────────

    public List<Player> getNearbyPlayers() {
        List<Player> list = new ArrayList<>();
        for (Entity e : boss.getNearbyEntities(ENGAGE_RADIUS, ENGAGE_RADIUS, ENGAGE_RADIUS)) {
            if (e instanceof Player p && !p.isDead() && p.isOnline()) {
                list.add(p);
            }
        }
        return list;
    }

    private Location getSurface(World world, int x, int startY, int z) {
        for (int y = startY + 3; y >= startY - 5; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isSolid() && !b.getType().name().contains("AIR")) {
                return b.getLocation();
            }
        }
        return null;
    }

    /**
     * Restaure tous les blocs magma restants — appelé à la mort du boss.
     */
    public void restoreAllBlocks() {
        for (Map.Entry<Location, BlockData> entry : savedBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            if (block.getType() == Material.MAGMA_BLOCK) {
                block.setBlockData(entry.getValue(), false);
            }
        }
        savedBlocks.clear();
    }
}

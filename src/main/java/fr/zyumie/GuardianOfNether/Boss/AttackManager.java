package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.AttackSettings;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Config.PhaseConfig;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Attaques spéciales du Gardien, entièrement pilotées par Boss.yml.
 *
 * Le boss lance UNE attaque à la fois (pause globale entre deux attaques),
 * choisie au hasard parmi celles activées pour la phase en cours.
 *
 *  - magma-floor    : sol en magma temporaire
 *  - sword-rain     : pluie d'épées netherite
 *  - fire-nova      : onde de feu
 *  - leap-slam      : bond + impact sur un joueur éloigné
 *  - meteor-shower  : météores signalés au sol
 *  - vortex         : aspiration puis explosion
 *  - soul-lightning : foudre des âmes avec zones marquées
 */
public class AttackManager {

    private final Main plugin;
    private final ConfigManager config;
    private final WitherSkeleton boss;

    // Blocs transformés en magma → sauvegardés pour restauration
    private final Map<Location, BlockData> savedBlocks = new HashMap<>();

    // Gestion du temps interne (en ticks)
    private long age       = 0;
    private long busyUntil = 0;
    private long globalReady = 0;
    private final Map<String, Long> nextReady = new HashMap<>();

    private final Random random = new Random();

    public AttackManager(Main plugin, ConfigManager config, WitherSkeleton boss) {
        this.plugin = plugin;
        this.config = config;
        this.boss   = boss;
    }

    // ── Tick central ─────────────────────────────────────────────

    /**
     * Appelé régulièrement par GuardianBoss.
     * @param stepTicks nombre de ticks écoulés depuis le dernier appel
     */
    public void tick(int phase, int stepTicks) {
        if (!boss.isValid()) return;

        age += stepTicks;
        if (age < busyUntil || age < globalReady) return;

        List<Player> targets = getNearbyPlayers();
        if (targets.isEmpty()) return;

        List<String> ready = new ArrayList<>();
        for (Map.Entry<String, AttackSettings> entry : config.attacks.entrySet()) {
            AttackSettings s = entry.getValue();
            if (!s.enabled || !s.activeIn(phase)) continue;
            // Premier lancement : après la moitié du cooldown
            long readyAt = nextReady.computeIfAbsent(entry.getKey(), k -> age + s.cooldownTicks() / 2);
            if (age >= readyAt) ready.add(entry.getKey());
        }
        if (ready.isEmpty()) return;

        Collections.shuffle(ready, random);
        for (String id : ready) {
            AttackSettings s = config.attacks.get(id);
            int duration = cast(id, s, targets);
            if (duration < 0) continue; // conditions non remplies → on essaie la suivante

            nextReady.put(id, age + s.cooldownTicks());
            globalReady = age + config.attackGlobalCooldown * 20L;
            busyUntil   = age + duration;

            if (!s.message.isEmpty()) {
                for (Player p : targets) p.sendMessage(s.message);
            }
            return;
        }
    }

    /** Retourne la durée estimée de l'attaque en ticks, ou -1 si elle ne peut pas être lancée. */
    private int cast(String id, AttackSettings s, List<Player> targets) {
        return switch (id) {
            case "magma-floor"    -> magmaFloor(s);
            case "sword-rain"     -> swordRain(s, targets);
            case "fire-nova"      -> fireNova(s, targets);
            case "leap-slam"      -> leapSlam(s, targets);
            case "meteor-shower"  -> meteorShower(s);
            case "vortex"         -> vortex(s);
            case "soul-lightning" -> soulLightning(s, targets);
            default               -> -1;
        };
    }

    // ── Shockwave de transition de phase ─────────────────────────

    /**
     * Explosion visuelle + knockback sur tous les joueurs proches.
     */
    public void phaseShockwave(int newPhase) {
        Location center = boss.getLocation();
        World world = center.getWorld();
        PhaseConfig pc = config.phase(newPhase);

        world.spawnParticle(Particle.EXPLOSION, center, 5);
        world.spawnParticle(Particle.FLAME,     center, 80, 2, 2, 2, 0.15);
        world.spawnParticle(Particle.LAVA,      center, 40, 3, 1, 3, 0);
        world.spawnParticle(Particle.SMOKE,     center, 60, 2, 2, 2, 0.10);

        world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 2f, 0.5f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,    2f, 0.8f);

        float pitch = newPhase == 3 ? 0.6f : 0.9f;

        for (Player p : getNearbyPlayers()) {
            Vector dir = away(p.getLocation(), center).multiply(1.8);
            dir.setY(0.6);
            p.setVelocity(dir);
            if (!pc.title.isEmpty() || !pc.subtitle.isEmpty()) {
                p.sendTitle(pc.title, pc.subtitle, 5, 50, 10);
            }
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, pitch);
        }

        new BukkitRunnable() {
            double radius = 0.5;
            int step = 0;
            @Override
            public void run() {
                if (step++ > 20 || !boss.isValid()) { cancel(); return; }
                int points = Math.min(48, Math.max(8, (int) (radius * 8)));
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

    // ── Attaque : Sol en magma ───────────────────────────────────

    private int magmaFloor(AttackSettings s) {
        Location center = boss.getLocation();
        World world = center.getWorld();
        int radius = s.i("radius", 10);
        int minDelay = s.i("restore-min", 60);
        int maxDelay = Math.max(minDelay, s.i("restore-max", 120));
        List<Location> toRestore = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;

                Block block = getSurface(world,
                        center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z);
                if (block == null) continue;

                Material type = block.getType();
                if (type == Material.MAGMA_BLOCK) continue;
                if (type.getHardness() < 0) continue;              // bedrock, barrière, cadres de portail...
                if (block.getState() instanceof TileState) continue; // coffres, spawners, etc.

                Location loc = block.getLocation();
                savedBlocks.put(loc, block.getBlockData());
                block.setType(Material.MAGMA_BLOCK, false);
                toRestore.add(loc);
            }
        }

        world.spawnParticle(Particle.LAVA,  center.clone().add(0, 1, 0), 30, radius, 0.5, radius, 0);
        world.spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 20, radius, 0.5, radius, 0.05);
        world.playSound(center, Sound.BLOCK_LAVA_AMBIENT,    2f, 0.7f);
        world.playSound(center, Sound.BLOCK_NETHERRACK_STEP, 2f, 0.5f);

        long delay = 20L * (minDelay + random.nextInt(maxDelay - minDelay + 1));
        Bukkit.getScheduler().runTaskLater(plugin, () -> restoreBlocks(toRestore, world), delay);
        return 40;
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
        if (boss.isValid()) world.playSound(boss.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.8f);
    }

    /**
     * Restaure tous les blocs magma restants — appelé à la mort du boss
     * et à l'arrêt du serveur.
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

    // ── Attaque : Pluie d'épées ──────────────────────────────────

    private int swordRain(AttackSettings s, List<Player> targets) {
        World world = boss.getWorld();
        int swords = Math.max(1, s.i("swords-per-player", 3));
        double damage = s.d("damage", 8.0);

        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2f, 0.4f);

        for (Player target : targets) {
            for (int i = 0; i < swords; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!boss.isValid() || !isTargetable(target)) return;
                    if (!target.getWorld().equals(boss.getWorld())) return;

                    double offsetX = (random.nextDouble() - 0.5) * 4;
                    double offsetZ = (random.nextDouble() - 0.5) * 4;
                    Location spawnLoc = target.getLocation().clone().add(offsetX, 18, offsetZ);
                    launchSwordProjectile(spawnLoc, target.getLocation().clone(), damage);
                }, i * 8L);
            }
        }
        return swords * 8 + 80;
    }

    private void launchSwordProjectile(Location from, Location target, double damage) {
        World world = from.getWorld();

        ArmorStand sword = (ArmorStand) world.spawnEntity(from, EntityType.ARMOR_STAND);
        sword.setVisible(false);
        sword.setGravity(true);
        sword.setSmall(true);
        sword.setArms(true);
        sword.setInvulnerable(true);
        sword.setPersistent(false);

        ItemStack swordItem = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = swordItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§4Épée du Gardien");
            swordItem.setItemMeta(meta);
        }
        sword.getEquipment().setItemInMainHand(swordItem);

        Vector velocity = target.toVector().subtract(from.toVector()).normalize().multiply(1.4);
        sword.setVelocity(velocity);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!sword.isValid()) { cancel(); return; }
                ticks++;

                Location loc = sword.getLocation();
                world.spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.CRIT,  loc, 2, 0.1, 0.1, 0.1, 0);

                boolean hitGround = loc.getBlock().getType().isSolid()
                        || loc.clone().subtract(0, 0.5, 0).getBlock().getType().isSolid();

                if (hitGround || ticks > 60) {
                    world.spawnParticle(Particle.EXPLOSION, loc, 1);
                    world.spawnParticle(Particle.FLAME,     loc, 15, 0.4, 0.2, 0.4, 0.1);
                    world.spawnParticle(Particle.CRIT,      loc, 10, 0.3, 0.3, 0.3, 0.05);
                    world.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 0.5f);
                    world.playSound(loc, Sound.BLOCK_STONE_HIT,         1f, 0.7f);

                    blast(loc, 2.0, damage, 0, 40);

                    sword.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Attaque : Nova de feu ────────────────────────────────────

    private int fireNova(AttackSettings s, List<Player> targets) {
        double maxRadius = Math.max(4, s.d("max-radius", 30.0));
        double damage = s.d("damage", 6.0);
        Location center = boss.getLocation().clone().add(0, 0.5, 0);
        World world = center.getWorld();

        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT,    2f, 0.3f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);

        new BukkitRunnable() {
            double radius = 0;
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (radius > maxRadius || !boss.isValid()) { cancel(); return; }

                // Plafonné à 48 points pour ne pas saturer les clients
                int points = Math.min(48, Math.max(8, (int) (radius * 6)));
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location ring = new Location(world, x, center.getY(), z);
                    world.spawnParticle(Particle.FLAME,           ring, 2, 0, 0.2, 0, 0.05);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, ring, 1, 0, 0.1, 0, 0.02);
                }

                for (Player p : targets) {
                    if (hit.contains(p.getUniqueId()) || !isTargetable(p)) continue;
                    if (!p.getWorld().equals(world)) continue;
                    double dist = p.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 1.5) {
                        hit.add(p.getUniqueId());
                        p.damage(damage, boss);
                        p.setFireTicks(60);
                        Vector kb = away(p.getLocation(), center).multiply(1.2);
                        kb.setY(0.4);
                        p.setVelocity(kb);
                    }
                }

                radius += 0.9;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return (int) (maxRadius / 0.9) + 20;
    }

    // ── Attaque : Bond (leap-slam) ───────────────────────────────

    private int leapSlam(AttackSettings s, List<Player> targets) {
        double minDistance = s.d("min-distance", 7.0);
        double radius = s.d("radius", 5.0);
        double damage = s.d("damage", 10.0);

        List<Player> candidates = new ArrayList<>();
        for (Player p : targets) {
            if (!p.getWorld().equals(boss.getWorld())) continue;
            if (p.getLocation().distance(boss.getLocation()) >= minDistance) candidates.add(p);
        }
        if (candidates.isEmpty()) return -1;

        Player target = candidates.get(random.nextInt(candidates.size()));
        World world = boss.getWorld();

        // Préparation : rugissement + nuage
        world.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.6f);
        world.spawnParticle(Particle.CLOUD, boss.getLocation(), 20, 1, 0.2, 1, 0.05);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!boss.isValid() || !isTargetable(target)) return;
            if (!target.getWorld().equals(boss.getWorld())) return;

            Vector dir = target.getLocation().toVector().subtract(boss.getLocation().toVector());
            double dist = Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ());
            dir.setY(0);
            if (dir.lengthSquared() > 1.0E-4) dir.normalize();

            Vector velocity = dir.multiply(Math.min(1.8, 0.4 + dist * 0.07));
            velocity.setY(0.85);
            boss.setVelocity(velocity);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!boss.isValid()) { cancel(); return; }
                    ticks++;
                    Location loc = boss.getLocation();
                    boolean landed = ticks > 5 && boss.isOnGround();

                    if (landed || ticks > 50) {
                        World w = loc.getWorld();
                        w.spawnParticle(Particle.EXPLOSION, loc, 3, 1, 0.2, 1, 0);
                        w.spawnParticle(Particle.CLOUD,     loc, 40, radius / 2, 0.2, radius / 2, 0.1);
                        ring(loc, radius, Particle.FLAME, 32);
                        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.7f);
                        blast(loc, radius, damage, 1.4, 40);
                        cancel();
                    } else {
                        world.spawnParticle(Particle.FLAME, loc, 6, 0.4, 0.4, 0.4, 0.02);
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }, 12L);

        return 80;
    }

    // ── Attaque : Météores ───────────────────────────────────────

    private int meteorShower(AttackSettings s) {
        int meteors = Math.max(1, s.i("meteors", 8));
        int warning = Math.max(10, s.i("warning-ticks", 30));
        double radius = s.d("radius", 3.0);
        double damage = s.d("damage", 12.0);

        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2f, 0.5f);

        for (int i = 0; i < meteors; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!boss.isValid()) return;
                List<Player> alive = getNearbyPlayers();
                if (alive.isEmpty()) return;

                Player t = alive.get(random.nextInt(alive.size()));
                World world = t.getWorld();
                double gx = t.getLocation().getX() + (random.nextDouble() - 0.5) * 6;
                double gz = t.getLocation().getZ() + (random.nextDouble() - 0.5) * 6;

                Block ground = getSurface(world, (int) Math.floor(gx), t.getLocation().getBlockY(), (int) Math.floor(gz));
                double gy = ground != null ? ground.getY() + 1.05 : t.getLocation().getY();

                launchMeteor(new Location(world, gx, gy, gz), warning, radius, damage);
            }, i * 6L);
        }
        return meteors * 6 + warning + 20;
    }

    private void launchMeteor(Location ground, int warning, double radius, double damage) {
        World world = ground.getWorld();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!boss.isValid()) { cancel(); return; }
                t++;

                if (t <= warning) {
                    // Cercle d'avertissement qui se remplit
                    double progress = t / (double) warning;
                    ring(ground, radius, Particle.FLAME, 16);
                    ring(ground, radius * progress, Particle.SOUL_FIRE_FLAME, 10);
                } else {
                    int fall = t - warning; // 1 → 10
                    double y = ground.getY() + 18 * (1 - fall / 10.0);
                    Location m = new Location(world, ground.getX(), y, ground.getZ());
                    world.spawnParticle(Particle.FLAME, m, 12, 0.3, 0.3, 0.3, 0.02);
                    world.spawnParticle(Particle.SMOKE, m, 6, 0.3, 0.3, 0.3, 0.02);

                    if (fall >= 10) {
                        world.spawnParticle(Particle.EXPLOSION, ground, 2, 0.5, 0.2, 0.5, 0);
                        world.spawnParticle(Particle.FLAME, ground, 40, radius / 2, 0.3, radius / 2, 0.1);
                        world.spawnParticle(Particle.LAVA,  ground, 15, radius / 2, 0.3, radius / 2, 0);
                        world.playSound(ground, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                        blast(ground, radius, damage, 0.8, 60);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Attaque : Vortex ─────────────────────────────────────────

    private int vortex(AttackSettings s) {
        int duration = Math.max(20, s.i("duration-ticks", 60));
        double radius = s.d("radius", 14.0);
        double pull = s.d("pull-strength", 0.28);
        double blastRadius = s.d("blast-radius", 6.0);
        double damage = s.d("damage", 8.0);

        World world = boss.getWorld();
        world.playSound(boss.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 3f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!boss.isValid()) { cancel(); return; }
                t += 2;
                Location c = boss.getLocation();

                // Spirale de particules
                for (int i = 0; i < 12; i++) {
                    double rr = radius * (i + 1) / 12.0;
                    double ang = t * 0.3 + i * 0.6;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            c.getX() + rr * Math.cos(ang), c.getY() + 0.3 + i * 0.05, c.getZ() + rr * Math.sin(ang),
                            1, 0, 0, 0, 0);
                }

                // Aspiration horizontale
                for (Player p : getNearbyPlayers()) {
                    if (!p.getWorld().equals(world)) continue;
                    Vector toBoss = c.toVector().subtract(p.getLocation().toVector());
                    toBoss.setY(0);
                    double dist = toBoss.length();
                    if (dist > radius || dist < 2.5) continue;
                    Vector v = toBoss.normalize().multiply(pull);
                    v.setY(p.getVelocity().getY());
                    p.setVelocity(v);
                }

                if (t >= duration) {
                    world.spawnParticle(Particle.EXPLOSION, c, 5, 1, 0.5, 1, 0);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, c, 80, blastRadius / 2, 0.5, blastRadius / 2, 0.1);
                    ring(c, blastRadius, Particle.SOUL_FIRE_FLAME, 40);
                    world.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
                    blast(c, blastRadius, damage, 1.6, 0);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return duration + 20;
    }

    // ── Attaque : Foudre des âmes ────────────────────────────────

    private int soulLightning(AttackSettings s, List<Player> targets) {
        int strikes = Math.max(1, s.i("strikes-per-player", 2));
        int warning = Math.max(10, s.i("warning-ticks", 24));
        double radius = s.d("radius", 2.5);
        double damage = s.d("damage", 10.0);

        for (Player p : targets) {
            for (int i = 0; i < strikes; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!boss.isValid() || !isTargetable(p)) return;
                    if (!p.getWorld().equals(boss.getWorld())) return;
                    // Zone figée au moment du marquage : le joueur peut l'esquiver
                    strikeLightning(p.getLocation().clone(), warning, radius, damage);
                }, i * 12L);
            }
        }
        return warning + strikes * 12 + 20;
    }

    private void strikeLightning(Location spot, int warning, double radius, double damage) {
        World world = spot.getWorld();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!boss.isValid()) { cancel(); return; }
                t += 2;

                if (t < warning) {
                    ring(spot, radius, Particle.SOUL_FIRE_FLAME, 16);
                } else {
                    world.strikeLightningEffect(spot);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, spot, 40, radius / 2, 1, radius / 2, 0.2);
                    world.spawnParticle(Particle.SOUL, spot, 20, radius / 2, 0.5, radius / 2, 0.05);
                    blast(spot, radius, damage, 0.8, 0);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ── Helpers ──────────────────────────────────────────────────

    public List<Player> getNearbyPlayers() {
        double r = config.attackEngageRadius;
        List<Player> list = new ArrayList<>();
        for (Entity e : boss.getNearbyEntities(r, r, r)) {
            if (e instanceof Player p && isTargetable(p)) list.add(p);
        }
        return list;
    }

    private boolean isTargetable(Player p) {
        if (!p.isOnline() || p.isDead()) return false;
        GameMode gm = p.getGameMode();
        return gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE;
    }

    /** Dégâts + knockback + feu sur les joueurs dans le rayon. */
    private void blast(Location c, double radius, double damage, double knock, int fireTicks) {
        World w = c.getWorld();
        for (Entity e : w.getNearbyEntities(c, radius, radius, radius)) {
            if (!(e instanceof Player p) || !isTargetable(p)) continue;
            if (p.getLocation().distanceSquared(c) > radius * radius) continue;

            p.damage(damage, boss);
            if (fireTicks > 0) p.setFireTicks(fireTicks);
            if (knock > 0) {
                Vector kb = away(p.getLocation(), c).multiply(knock);
                kb.setY(0.45);
                p.setVelocity(kb);
            }
        }
    }

    /** Direction horizontale normalisée « de center vers from » (sans division par zéro). */
    private Vector away(Location from, Location center) {
        Vector v = from.toVector().subtract(center.toVector());
        v.setY(0);
        if (v.lengthSquared() < 1.0E-4) return new Vector(1, 0, 0);
        return v.normalize();
    }

    private void ring(Location c, double r, Particle particle, int points) {
        World w = c.getWorld();
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            w.spawnParticle(particle,
                    c.getX() + r * Math.cos(a), c.getY() + 0.1, c.getZ() + r * Math.sin(a),
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * Premier bloc solide avec de l'air au-dessus, en partant légèrement
     * au-dessus de startY (évite de viser le plafond d'une grotte / du Nether).
     */
    private Block getSurface(World world, int x, int startY, int z) {
        for (int y = startY + 2; y >= startY - 5; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isSolid() && b.getRelative(0, 1, 0).isPassable()) {
                return b;
            }
        }
        return null;
    }
}

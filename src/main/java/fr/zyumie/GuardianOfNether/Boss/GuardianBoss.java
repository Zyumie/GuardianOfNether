package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Config.PhaseConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class GuardianBoss {

    public static final String BOSS_PDC_KEY = "guardian_boss";

    // Le scheduler principal tourne toutes les 10 ticks (2 fois par seconde)
    private static final int TICK_STEP = 10;
    private static final int RUNS_PER_SECOND = 2;

    private final Main          plugin;
    private final ConfigManager config;

    private WitherSkeleton bossEntity;
    private Location       spawnPoint;
    private BossBar        bossBar;
    private PhaseManager   phaseManager;
    private MinionManager  minionManager;
    private AttackManager  attackManager;
    private BossAI         ai;

    private final Set<Player> engagedPlayers = new HashSet<>();

    private BukkitTask mainTask;
    private BukkitTask minionWaveTask;

    private int     runCounter     = 0;
    private boolean deathAnnounced = false;

    public GuardianBoss(Main plugin, ConfigManager config, MinionManager minionManager) {
        this.plugin        = plugin;
        this.config        = config;
        this.minionManager = minionManager;
    }

    // ── Spawn ────────────────────────────────────────────────────

    public void spawn(Location loc) {
        spawnPoint = loc.clone();

        bossEntity = loc.getWorld().spawn(loc, WitherSkeleton.class, entity -> {
            entity.setCustomName(config.bossName);
            entity.setCustomNameVisible(true);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            entity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, BOSS_PDC_KEY),
                PersistentDataType.BYTE, (byte) 1
            );
            entity.addScoreboardTag("guardian_boss");
        });

        setAttribute(bossEntity, Attribute.MAX_HEALTH, config.bossMaxHealth);
        bossEntity.setHealth(config.bossMaxHealth);
        setAttribute(bossEntity, Attribute.FOLLOW_RANGE,
                Math.max(config.leashRadius, config.aiAggroRadius));

        Main.trackedBosses.add(bossEntity.getUniqueId());

        phaseManager  = new PhaseManager(plugin, config, bossEntity);
        attackManager = new AttackManager(plugin, config, bossEntity);
        ai            = new BossAI(config, bossEntity, spawnPoint);
        phaseManager.applyInitialPhase();

        setupBossBar();
        minionManager.spawnWave(loc, config.minionsOnSpawn);
        announceSpawn();
        startMainScheduler();
        startMinionWaveScheduler();
    }

    // ── Schedulers ───────────────────────────────────────────────

    /**
     * Boucle unique (toutes les 10 ticks) : phases, IA, attaques, bossbar.
     * Les tâches « à la seconde » (régén, aura, nettoyage) tournent 1 run sur 2.
     */
    private void startMainScheduler() {
        mainTask = new BukkitRunnable() {
            int lastPhase = 1;

            @Override
            public void run() {
                if (!bossEntity.isValid()) {
                    handleDeath();
                    return;
                }

                runCounter++;
                boolean everySecond = runCounter % RUNS_PER_SECOND == 0;

                // 1. Transition de phase
                phaseManager.tick();
                int currentPhase = phaseManager.getCurrentPhase();
                if (currentPhase != lastPhase) {
                    attackManager.phaseShockwave(currentPhase);
                    lastPhase = currentPhase;
                }
                PhaseConfig pc = phaseManager.getCurrentPhaseConfig();

                // 2. IA puis attaques spéciales
                ai.tick(TICK_STEP);
                attackManager.tick(currentPhase, TICK_STEP);

                // 3. Leash
                double leashSq = (double) config.leashRadius * config.leashRadius;
                if (bossEntity.getLocation().distanceSquared(spawnPoint) > leashSq) {
                    bossEntity.teleport(spawnPoint);
                }

                // 4. BossBar
                updateBossBar(pc);

                // 5. Particules de phase
                spawnPhaseParticles(pc.particle);

                if (!everySecond) return;

                // ── Tâches à la seconde ──

                // 6. Régénération
                int regenRuns = Math.max(1, pc.regenIntervalSeconds) * RUNS_PER_SECOND;
                if (pc.regenAmount > 0 && runCounter % regenRuns == 0) {
                    double newHp = Math.min(bossEntity.getHealth() + pc.regenAmount, maxHealth());
                    bossEntity.setHealth(newHp);
                    bossEntity.getWorld().spawnParticle(
                        Particle.HEART,
                        bossEntity.getLocation().add(0, 1, 0),
                        5
                    );
                }

                // 7. Nettoyage joueurs hors zone
                engagedPlayers.removeIf(p ->
                    !p.isOnline() || p.isDead()
                    || !p.getWorld().equals(spawnPoint.getWorld())
                    || p.getLocation().distanceSquared(spawnPoint) > leashSq
                );

                // 8. Aura de dégâts
                if (config.auraEnabled && pc.damageMultiplier > 0) {
                    double auraSq = config.auraRadius * config.auraRadius;
                    for (Player target : engagedPlayers) {
                        if (!target.getWorld().equals(bossEntity.getWorld())) continue;
                        if (target.getLocation().distanceSquared(bossEntity.getLocation()) <= auraSq) {
                            target.damage(config.auraDamage * pc.damageMultiplier, bossEntity);
                            target.setFireTicks(config.auraFireTicks);
                        }
                    }
                }

                // 9. Nettoyage des sbires morts
                minionManager.tickAndGetAlive();
            }
        }.runTaskTimer(plugin, 0L, TICK_STEP);
    }

    private void startMinionWaveScheduler() {
        long intervalTicks = (long) config.minionsWaveInterval * 20L;
        minionWaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossEntity.isValid()) { cancel(); return; }
                minionManager.spawnWave(spawnPoint, config.minionsWaveCount);
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void cancelTasks() {
        if (mainTask != null)       mainTask.cancel();
        if (minionWaveTask != null) minionWaveTask.cancel();
    }

    // ── BossBar ──────────────────────────────────────────────────

    private void setupBossBar() {
        bossBar = Bukkit.createBossBar(config.bossName, config.phase1.barColor, config.bossBarStyle);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private void updateBossBar(PhaseConfig pc) {
        bossBar.setProgress(Math.max(0, Math.min(1, bossEntity.getHealth() / maxHealth())));
        bossBar.setColor(pc.barColor);
    }

    // ── Mort / arrêt ─────────────────────────────────────────────

    private void handleDeath() {
        if (deathAnnounced) return;
        deathAnnounced = true;

        cancelTasks();
        minionManager.killAll();
        attackManager.restoreAllBlocks(); // restaure les blocs magma
        if (bossBar != null) bossBar.removeAll();
        announceDeath();
    }

    /**
     * Arrêt propre (plugin désactivé / serveur stoppé) : pas d'annonce de mort,
     * les blocs de magma sont restaurés, et le boss est retiré si la config le demande.
     */
    public void shutdown() {
        deathAnnounced = true;

        cancelTasks();
        if (minionManager != null) minionManager.killAll();
        if (attackManager != null) attackManager.restoreAllBlocks();
        if (bossBar != null) bossBar.removeAll();

        if (bossEntity != null) {
            Main.trackedBosses.remove(bossEntity.getUniqueId());
            if (config.removeBossOnDisable && bossEntity.isValid()) bossEntity.remove();
        }
    }

    // ── Annonces ─────────────────────────────────────────────────

    private void announceSpawn() {
        Bukkit.broadcastMessage(config.msgSpawnBroadcast);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(config.msgSpawnTitle, config.msgSpawnSubtitle, 10, 60, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
        }
    }

    private void announceDeath() {
        Bukkit.broadcastMessage(config.msgDeathLine);
        Bukkit.broadcastMessage(config.msgDeathBroadcast);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }
    }

    // ── Particules ───────────────────────────────────────────────

    private void spawnPhaseParticles(Particle particle) {
        Location center = bossEntity.getLocation().add(0, 1, 0);
        int points = 12;
        double radius = 1.2 * phaseManager.getCurrentPhaseConfig().scale;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            center.getWorld().spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
        }
    }

    // ── API publique ─────────────────────────────────────────────

    public void playerEngaged(Player player) {
        engagedPlayers.add(player);
    }

    public void addToBossBar(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public WitherSkeleton getBossEntity() { return bossEntity; }

    public boolean isAlive() {
        return bossEntity != null && bossEntity.isValid();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private double maxHealth() {
        var attr = bossEntity.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getBaseValue() : config.bossMaxHealth;
    }

    private void setAttribute(WitherSkeleton entity, Attribute attr, double value) {
        var instance = entity.getAttribute(attr);
        if (instance != null) instance.setBaseValue(value);
    }
}

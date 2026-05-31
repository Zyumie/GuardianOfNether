package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Config.PhaseConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

/**
 * Orchestre le cycle de vie complet du boss :
 * spawn → scheduler central → phases → leash → regen → mort.
 *
 * PhaseManager  : transitions de phase (vitesse, taille, armure)
 * MinionManager : spawn et tracking des sbires
 * LeashHandler  : maintien dans la zone
 */
public class GuardianBoss {

    // Clé PDC pour identifier le boss de façon fiable
    public static final String BOSS_PDC_KEY = "guardian_boss";

    private final Main          plugin;
    private final ConfigManager config;

    private WitherSkeleton  bossEntity;
    private Location        spawnPoint;
    private BossBar         bossBar;
    private PhaseManager    phaseManager;
    private MinionManager   minionManager;

    // Joueurs ayant frappé le boss — utilisés pour les dégâts de zone
    private final Set<Player> engagedPlayers = new HashSet<>();

    // Tâches Bukkit — stockées pour pouvoir les annuler proprement
    private BukkitTask mainTask;
    private BukkitTask minionWaveTask;
    private BukkitTask bossBarTask;

    // Compteur de ticks pour la régénération
    private int tickCounter = 0;

    // Évite de déclencher announceDeath plusieurs fois
    private boolean deathAnnounced = false;

    public GuardianBoss(Main plugin, ConfigManager config, MinionManager minionManager) {
        this.plugin        = plugin;
        this.config        = config;
        this.minionManager = minionManager;
    }

    // ── Spawn ────────────────────────────────────────────────────

    public void spawn(Location loc) {
        spawnPoint = loc.clone();

        // Spawn de l'entité
        bossEntity = loc.getWorld().spawn(loc, WitherSkeleton.class, entity -> {
            // Configuré dans le consumer avant que les events de spawn ne se déclenchent
            entity.setCustomName(config.bossName);
            entity.setCustomNameVisible(true);
            entity.setPersistent(true);

            // Marque PDC — identifie ce mob comme le boss
            entity.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, BOSS_PDC_KEY),
                    PersistentDataType.BYTE, (byte) 1
            );

            // Tag scoreboard pour StackMob
            entity.addScoreboardTag("guardian_boss");
        });

        // HP max depuis phase 1
        setAttribute(bossEntity, Attribute.MAX_HEALTH, config.phase1.maxHp);
        bossEntity.setHealth(config.phase1.maxHp);
        setAttribute(bossEntity, Attribute.FOLLOW_RANGE, config.leashRadius);

        // Tracking global
        Main.trackedBosses.add(bossEntity.getUniqueId());

        // Phase initiale (armure, vitesse, taille phase 1)
        phaseManager = new PhaseManager(plugin, config, bossEntity);
        phaseManager.applyInitialPhase();

        // BossBar
        setupBossBar();

        // Vague initiale de sbires
        minionManager.spawnWave(loc, config.minionsOnSpawn);

        // Annonce spawn
        announceSpawn();

        // Démarrage des schedulers
        startMainScheduler();
        startMinionWaveScheduler();
    }

    // ── Schedulers ───────────────────────────────────────────────

    private void startMainScheduler() {
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossEntity.isValid()) {
                    cancel();
                    handleDeath();
                    return;
                }

                PhaseConfig pc = phaseManager.getCurrentPhaseConfig();

                // 1. Vérification et transition de phase
                phaseManager.tick();

                // 2. Leash — ramène le boss à son point de spawn si trop loin
                double leashSq = config.leashRadius * config.leashRadius;
                if (bossEntity.getLocation().distanceSquared(spawnPoint) > leashSq) {
                    bossEntity.teleport(spawnPoint);
                }

                // 3. Régénération
                tickCounter++;
                int regenIntervalTicks = pc.regenIntervalSeconds * 20;
                if (pc.regenAmount > 0 && tickCounter % regenIntervalTicks == 0) {
                    double newHp = Math.min(
                            bossEntity.getHealth() + pc.regenAmount,
                            bossEntity.getAttribute(Attribute.MAX_HEALTH).getBaseValue()
                    );
                    bossEntity.setHealth(newHp);
                    bossEntity.getWorld().spawnParticle(
                            Particle.HEART,
                            bossEntity.getLocation().add(0, 1, 0),
                            5
                    );
                }

                // 4. Nettoyage des joueurs hors zone ou déconnectés
                engagedPlayers.removeIf(p ->
                        !p.isOnline() || p.isDead() ||
                                p.getLocation().distanceSquared(spawnPoint) > leashSq
                );

                // 5. Dégâts de zone + particules autour du boss
                for (Player target : engagedPlayers) {
                    double distSq = target.getLocation().distanceSquared(bossEntity.getLocation());
                    if (distSq <= 10 * 10 && pc.damageMultiplier > 0) {
                        target.damage(8 * pc.damageMultiplier, bossEntity);
                        target.setFireTicks(80);
                    }
                }

                // 6. Particules de phase autour du boss
                spawnPhaseParticles(pc.particle);

                // 7. Tick des sbires (nettoyage des morts)
                minionManager.tickAndGetAlive();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startMinionWaveScheduler() {
        long intervalTicks = (long) config.minionsWaveInterval * 20L;
        minionWaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossEntity.isValid()) {
                    cancel();
                    return;
                }
                minionManager.spawnWave(spawnPoint, config.minionsWaveCount);
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    // ── BossBar ──────────────────────────────────────────────────

    private void setupBossBar() {
        bossBar = Bukkit.createBossBar(config.bossName, BarColor.RED, BarStyle.SEGMENTED_10);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        bossBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossEntity.isValid()) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                double maxHp = bossEntity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                bossBar.setProgress(Math.max(0, bossEntity.getHealth() / maxHp));

                // Couleur de la bossbar selon la phase
                bossBar.setColor(switch (phaseManager.getCurrentPhase()) {
                    case 2  -> BarColor.YELLOW;
                    case 3  -> BarColor.RED;
                    default -> BarColor.PURPLE;
                });
            }
        }.runTaskTimer(plugin, 0L, 10L); // Toutes les 0.5s pour être réactif
    }

    // ── Mort ─────────────────────────────────────────────────────

    /**
     * Appelé quand le boss devient invalide (mort détectée par le scheduler).
     * Le BossListener gère le drop et les effets — ici on gère juste le cleanup.
     */
    private void handleDeath() {
        if (deathAnnounced) return;
        deathAnnounced = true;

        // Suppression des sbires restants
        minionManager.killAll();

        // Retrait de la bossbar
        if (bossBar != null) bossBar.removeAll();

        // Annonce mort
        announceDeath();
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
        double radius = 1.2;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location pLoc = new Location(center.getWorld(), x, center.getY(), z);
            center.getWorld().spawnParticle(particle, pLoc, 1, 0, 0, 0, 0);
        }
    }

    // ── API publique ─────────────────────────────────────────────

    /**
     * Enregistre un joueur comme engagé dans le combat
     * (appelé depuis BossListener quand il frappe le boss).
     */
    public void playerEngaged(Player player) {
        engagedPlayers.add(player);
    }

    /**
     * Ajoute un joueur à la bossbar (pour les joueurs qui rejoignent en cours de combat).
     */
    public void addToBossBar(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public WitherSkeleton getBossEntity() {
        return bossEntity;
    }

    public boolean isAlive() {
        return bossEntity != null && bossEntity.isValid();
    }

    // ── Helper ───────────────────────────────────────────────────

    private void setAttribute(WitherSkeleton entity, Attribute attr, double value) {
        var instance = entity.getAttribute(attr);
        if (instance != null) instance.setBaseValue(value);
    }
}
package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * IA du Gardien (paramètres dans Boss.yml, section AI).
 *
 *  - Choix de cible : plus proche, moins de vie ou aléatoire, re-évalué
 *    toutes les X secondes (le boss ne reste plus collé à un seul joueur).
 *  - Anti-cheese : si la cible reste trop loin (pilier, pont, eau, lave...)
 *    le boss se téléporte à côté d'elle.
 *
 * Point d'insertion pour une IA plus poussée : tick() est appelé toutes
 * les 10 ticks par GuardianBoss, juste avant AttackManager.tick().
 */
public class BossAI {

    private final ConfigManager config;
    private final WitherSkeleton boss;
    private final Location spawnPoint;
    private final Random random = new Random();

    private Player target;
    private int sinceRetarget = 0;
    private int farTicks = 0;

    public BossAI(ConfigManager config, WitherSkeleton boss, Location spawnPoint) {
        this.config     = config;
        this.boss       = boss;
        this.spawnPoint = spawnPoint.clone();
    }

    // ── Tick ─────────────────────────────────────────────────────

    /**
     * @param stepTicks nombre de ticks écoulés depuis le dernier appel
     */
    public void tick(int stepTicks) {
        if (!config.aiEnabled || !boss.isValid()) return;

        sinceRetarget += stepTicks;
        if (target == null || !isValidTarget(target) || sinceRetarget >= config.aiRetargetSeconds * 20) {
            target = pickTarget();
            sinceRetarget = 0;
        }

        if (target == null) {
            boss.setTarget(null);
            farTicks = 0;
            return;
        }

        boss.setTarget(target);

        // Anti-cheese : cible hors de portée trop longtemps → blink
        if (config.aiTeleportEnabled) {
            double max = config.aiTeleportDistance;
            boolean sameWorld = target.getWorld().equals(boss.getWorld());
            if (sameWorld && target.getLocation().distanceSquared(boss.getLocation()) > max * max) {
                farTicks += stepTicks;
            } else {
                farTicks = 0;
            }

            if (farTicks >= config.aiTeleportDelaySeconds * 20) {
                blinkNextTo(target);
                farTicks = 0;
            }
        }
    }

    // ── Ciblage ──────────────────────────────────────────────────

    private boolean isValidTarget(Player p) {
        if (!p.isOnline() || p.isDead()) return false;
        GameMode gm = p.getGameMode();
        if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return false;
        return p.getWorld().equals(boss.getWorld());
    }

    private Player pickTarget() {
        double r = config.aiAggroRadius;
        double leash = config.leashRadius + 5.0;
        List<Player> candidates = new ArrayList<>();

        for (Entity e : boss.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Player p) || !isValidTarget(p)) continue;
            if (p.getLocation().distanceSquared(spawnPoint) > leash * leash) continue;
            candidates.add(p);
        }
        if (candidates.isEmpty()) return null;

        return switch (config.aiTargetMode) {
            case "LOWEST_HEALTH" -> candidates.stream()
                    .min(Comparator.comparingDouble((Player p) -> p.getHealth()))
                    .orElse(null);
            case "RANDOM" -> candidates.get(random.nextInt(candidates.size()));
            default -> candidates.stream()
                    .min(Comparator.comparingDouble((Player p) ->
                            p.getLocation().distanceSquared(boss.getLocation())))
                    .orElse(null);
        };
    }

    // ── Téléportation ────────────────────────────────────────────

    private void blinkNextTo(Player t) {
        Location tl = t.getLocation();
        double leash = config.leashRadius;

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            Location dest = tl.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);

            if (dest.distanceSquared(spawnPoint) > leash * leash) continue;
            // Le boss peut faire jusqu'à 3,5 blocs de haut en phase 3
            if (!dest.getBlock().isPassable()
                    || !dest.clone().add(0, 1, 0).getBlock().isPassable()
                    || !dest.clone().add(0, 2, 0).getBlock().isPassable()) continue;

            Location from = boss.getLocation();
            from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.5);
            from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);

            dest.setYaw(from.getYaw());
            boss.teleport(dest);

            dest.getWorld().spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.5);
            dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
            return;
        }
    }
}

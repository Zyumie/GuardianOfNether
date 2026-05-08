package fr.zyumie.GuardianOfNether.Config;

import org.bukkit.Particle;

import java.util.Map;

/**
 * Données immuables d'une phase du boss.
 * Instanciée par ConfigManager, lue partout ailleurs.
 */
public class PhaseConfig {

    public final double maxHp;
    public final double speed;
    public final double scale;
    public final double damageMultiplier;
    public final int regenAmount;
    public final int regenIntervalSeconds;
    public final String armorMaterial;   // "DIAMOND" ou "NETHERITE"
    public final Map<String, Integer> armorEnchants;
    public final Particle particle;
    // Seuil en % de HP pour entrer dans cette phase (0 pour phase 1)
    public final int hpThresholdPercent;

    public PhaseConfig(
            double maxHp,
            double speed,
            double scale,
            double damageMultiplier,
            int regenAmount,
            int regenIntervalSeconds,
            String armorMaterial,
            Map<String, Integer> armorEnchants,
            Particle particle,
            int hpThresholdPercent
    ) {
        this.maxHp = maxHp;
        this.speed = speed;
        this.scale = scale;
        this.damageMultiplier = damageMultiplier;
        this.regenAmount = regenAmount;
        this.regenIntervalSeconds = regenIntervalSeconds;
        this.armorMaterial = armorMaterial;
        this.armorEnchants = armorEnchants;
        this.particle = particle;
        this.hpThresholdPercent = hpThresholdPercent;
    }
}
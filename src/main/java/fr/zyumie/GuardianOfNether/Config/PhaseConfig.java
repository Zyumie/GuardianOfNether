package fr.zyumie.GuardianOfNether.Config;

import org.bukkit.Particle;
import org.bukkit.boss.BarColor;

import java.util.Map;

/**
 * Données immuables d'une phase du boss.
 * Instanciée par ConfigManager (Boss.yml), lue partout ailleurs.
 */
public class PhaseConfig {

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
    public final BarColor barColor;
    public final String title;
    public final String subtitle;

    public PhaseConfig(
            double speed,
            double scale,
            double damageMultiplier,
            int regenAmount,
            int regenIntervalSeconds,
            String armorMaterial,
            Map<String, Integer> armorEnchants,
            Particle particle,
            int hpThresholdPercent,
            BarColor barColor,
            String title,
            String subtitle
    ) {
        this.speed = speed;
        this.scale = scale;
        this.damageMultiplier = damageMultiplier;
        this.regenAmount = regenAmount;
        this.regenIntervalSeconds = regenIntervalSeconds;
        this.armorMaterial = armorMaterial;
        this.armorEnchants = armorEnchants;
        this.particle = particle;
        this.hpThresholdPercent = hpThresholdPercent;
        this.barColor = barColor;
        this.title = title;
        this.subtitle = subtitle;
    }
}

package fr.zyumie.GuardianOfNether.Config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

/**
 * Paramètres d'une attaque lus depuis Boss.yml (section Attacks.<id>).
 * Les valeurs communes sont des champs, les valeurs propres à chaque
 * attaque (damage, radius...) sont lues avec d() / i().
 */
public class AttackSettings {

    public final boolean enabled;
    public final Set<Integer> phases;   // vide = toutes les phases
    public final int cooldownSeconds;
    public final String message;

    private final ConfigurationSection section;

    public AttackSettings(ConfigurationSection section) {
        this.section         = section;
        this.enabled         = section.getBoolean("enabled", true);
        this.phases          = new HashSet<>(section.getIntegerList("phases"));
        this.cooldownSeconds = Math.max(1, section.getInt("cooldown", 20));
        this.message         = ConfigManager.color(section.getString("message", ""));
    }

    public boolean activeIn(int phase) {
        return phases.isEmpty() || phases.contains(phase);
    }

    public long cooldownTicks() {
        return cooldownSeconds * 20L;
    }

    public double d(String key, double def) {
        return section.getDouble(key, def);
    }

    public int i(String key, int def) {
        return section.getInt(key, def);
    }
}

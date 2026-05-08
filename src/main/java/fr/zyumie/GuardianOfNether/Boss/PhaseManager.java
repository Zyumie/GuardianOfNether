package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Config.PhaseConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/**
 * Surveille la vie du boss et applique la bonne PhaseConfig
 * dès qu'un seuil est franchi (vitesse, taille, armure, particules...).
 */
public class PhaseManager {

    private final Main plugin;
    private final ConfigManager config;
    private final WitherSkeleton boss;

    // Phase actuelle (1, 2 ou 3) — on évite de réappliquer inutilement
    private int currentPhase = 1;

    public PhaseManager(Main plugin, ConfigManager config, WitherSkeleton boss) {
        this.plugin = plugin;
        this.config = config;
        this.boss   = boss;
    }

    // ── API publique ─────────────────────────────────────────────

    /**
     * À appeler à chaque tick du scheduler du boss.
     * Vérifie si un changement de phase est nécessaire et l'applique.
     */
    public void tick() {
        if (!boss.isValid()) return;

        double healthPercent = (boss.getHealth() / boss.getAttribute(Attribute.MAX_HEALTH).getBaseValue()) * 100.0;
        int targetPhase = resolvePhase(healthPercent);

        if (targetPhase != currentPhase) {
            applyPhase(targetPhase);
        }
    }

    /**
     * Applique la phase 1 au spawn (appel unique depuis GuardianBoss).
     */
    public void applyInitialPhase() {
        applyPhase(1);
    }

    /**
     * Retourne la PhaseConfig de la phase actuellement active.
     */
    public PhaseConfig getCurrentPhaseConfig() {
        return switch (currentPhase) {
            case 2  -> config.phase2;
            case 3  -> config.phase3;
            default -> config.phase1;
        };
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    // ── Logique interne ──────────────────────────────────────────

    /**
     * Détermine la phase cible selon le % de vie restant.
     */
    private int resolvePhase(double healthPercent) {
        if (healthPercent <= config.phase3.hpThresholdPercent) return 3;
        if (healthPercent <= config.phase2.hpThresholdPercent) return 2;
        return 1;
    }

    /**
     * Applique toutes les modifications liées à un changement de phase :
     * vitesse, taille, armure. Broadcast le message de transition.
     */
    private void applyPhase(int phase) {
        currentPhase = phase;
        PhaseConfig pc = getCurrentPhaseConfig();

        // Vitesse
        var speedAttr = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(pc.speed);

        // Taille (scale) — disponible depuis Paper 1.19.4+
        var scaleAttr = boss.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(pc.scale);

        // Armure
        applyBossArmor(pc);

        // Annonce (sauf phase 1 qui est le spawn initial)
        if (phase == 2) {
            org.bukkit.Bukkit.broadcastMessage(config.msgPhase2);
        } else if (phase == 3) {
            org.bukkit.Bukkit.broadcastMessage(config.msgPhase3);
        }

        plugin.getLogger().info("[GuardianBoss] Phase " + phase + " activée.");
    }

    /**
     * Équipe le boss avec une armure correspondant à la phase.
     * Le matériau (DIAMOND/NETHERITE) et les enchants viennent de la config.
     */
    private void applyBossArmor(PhaseConfig pc) {
        String mat = pc.armorMaterial; // "DIAMOND" ou "NETHERITE"

        Material helmet    = parseMaterial(mat + "_HELMET",    Material.NETHERITE_HELMET);
        Material chestplate= parseMaterial(mat + "_CHESTPLATE",Material.NETHERITE_CHESTPLATE);
        Material leggings  = parseMaterial(mat + "_LEGGINGS",  Material.NETHERITE_LEGGINGS);
        Material boots     = parseMaterial(mat + "_BOOTS",     Material.NETHERITE_BOOTS);

        boss.getEquipment().setHelmet    (buildArmorPiece(helmet,     "§6Casque du Gardien",     pc.armorEnchants));
        boss.getEquipment().setChestplate(buildArmorPiece(chestplate, "§6Plastron du Gardien",   pc.armorEnchants));
        boss.getEquipment().setLeggings  (buildArmorPiece(leggings,   "§6Jambières du Gardien",  pc.armorEnchants));
        boss.getEquipment().setBoots     (buildArmorPiece(boots,      "§6Bottes du Gardien",     pc.armorEnchants));

        // Drop chance à 0 — l'armure ne doit jamais drop
        boss.getEquipment().setHelmetDropChance    (0f);
        boss.getEquipment().setChestplateDropChance(0f);
        boss.getEquipment().setLeggingsDropChance  (0f);
        boss.getEquipment().setBootsDropChance     (0f);
    }

    /**
     * Construit une pièce d'armure avec nom et enchants.
     */
    private ItemStack buildArmorPiece(Material mat, String name, Map<String, Integer> enchants) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        meta.setUnbreakable(true);

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            NamespacedKey key = NamespacedKey.minecraft(entry.getKey().toLowerCase());
            Enchantment enchant = Registry.ENCHANTMENT.get(key);
            if (enchant != null) {
                meta.addEnchant(enchant, entry.getValue(), true);
            } else {
                plugin.getLogger().warning("[PhaseManager] Enchantement inconnu: '" + entry.getKey() + "'");
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Parse un Material avec fallback propre.
     */
    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[PhaseManager] Matériau invalide: '" + name
                    + "', utilisation de " + fallback.name());
            return fallback;
        }
    }
}
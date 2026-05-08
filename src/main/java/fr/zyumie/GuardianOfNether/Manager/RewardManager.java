package fr.zyumie.GuardianOfNether.Manager;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * Construit le plastron du Gardien à partir du ConfigManager.
 * Utilise le PersistentDataContainer pour identifier l'item de façon fiable,
 * contrairement à CustomModelData qui peut entrer en conflit avec d'autres plugins.
 */
public class RewardManager {

    // Clé PDC utilisée pour identifier le plastron — ne jamais la changer
    // après déploiement, sinon les plastrons existants ne seront plus reconnus.
    public static final String PDC_KEY = "guardian_chestplate";

    private final Main plugin;
    private final ConfigManager config;
    private final NamespacedKey chestplateKey;

    public RewardManager(Main plugin, ConfigManager config) {
        this.plugin  = plugin;
        this.config  = config;
        this.chestplateKey = new NamespacedKey(plugin, PDC_KEY);
    }

    // ── Création ────────────────────────────────────────────────

    /**
     * Construit le plastron du Gardien selon la config.
     * Utilisé aussi bien pour le drop à la mort du boss
     * que pour la commande /guardian-items.
     */
    public ItemStack buildChestplate() {
        Material mat = parseMaterial(config.dropMaterial, Material.DIAMOND_CHESTPLATE);
        ItemStack item = new ItemStack(mat);

        // Si le matériau n'est pas une armure, on fallback sur DIAMOND_CHESTPLATE
        if (!(item.getItemMeta() instanceof ArmorMeta)) {
            plugin.getLogger().severe("[RewardManager] Le matériau '" + config.dropMaterial
                    + "' n'est pas une armure valide ! Retour au DIAMOND_CHESTPLATE.");
            item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        }

        ArmorMeta meta = (ArmorMeta) item.getItemMeta();

        // Nom custom
        meta.setDisplayName(config.dropName);

        // Indestructible
        meta.setUnbreakable(config.dropUnbreakable);

        // Enchantements depuis la config
        applyEnchants(meta, config.dropEnchants);

        // Curse of Vanishing
        if (config.dropCurseOfVanishing) {
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        }

        // Armor Trim
        applyTrim(meta, config.dropTrimMaterial, config.dropTrimPattern);

        // Marqueur PDC — c'est ça qui identifie l'item de façon fiable
        meta.getPersistentDataContainer().set(chestplateKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    // ── Identification ───────────────────────────────────────────

    /**
     * Retourne true si l'item est le plastron du Gardien.
     * Vérification via PDC uniquement — pas de CustomModelData, pas de nom.
     */
    public boolean isGuardianChestplate(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(chestplateKey, PersistentDataType.BYTE);
    }

    // ── Helpers privés ───────────────────────────────────────────

    /**
     * Applique les enchantements depuis une Map<nom, niveau>.
     * Les noms correspondent aux clés Minecraft (ex: "protection", "unbreaking").
     */
    private void applyEnchants(org.bukkit.inventory.meta.ItemMeta meta,
                               Map<String, Integer> enchants) {
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            NamespacedKey key = NamespacedKey.minecraft(entry.getKey().toLowerCase());
            Enchantment enchant = Registry.ENCHANTMENT.get(key);
            if (enchant == null) {
                plugin.getLogger().warning("[RewardManager] Enchantement inconnu: '" + entry.getKey() + "'");
                continue;
            }
            meta.addEnchant(enchant, entry.getValue(), true); // true = bypass niveau max
        }
    }

    /**
     * Applique le trim d'armure depuis les noms de matériau et de pattern.
     */
    private void applyTrim(ArmorMeta meta, String materialName, String patternName) {
        TrimMaterial trimMat = Registry.TRIM_MATERIAL.get(
                NamespacedKey.minecraft(materialName.toLowerCase()));
        TrimPattern trimPat = Registry.TRIM_PATTERN.get(
                NamespacedKey.minecraft(patternName.toLowerCase()));

        if (trimMat == null) {
            plugin.getLogger().warning("[RewardManager] Trim material inconnu: '" + materialName + "'");
            return;
        }
        if (trimPat == null) {
            plugin.getLogger().warning("[RewardManager] Trim pattern inconnu: '" + patternName + "'");
            return;
        }

        meta.setTrim(new ArmorTrim(trimMat, trimPat));
    }

    /**
     * Parse un nom de Material avec fallback propre.
     */
    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[RewardManager] Matériau invalide: '" + name
                    + "', utilisation de " + fallback.name());
            return fallback;
        }
    }

    public NamespacedKey getChestplateKey() {
        return chestplateKey;
    }
}
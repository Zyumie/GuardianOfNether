package fr.zyumie.GuardianOfNether.Boss;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gère le cycle de vie complet des sbires :
 * spawn, tracking, attaque de zone, et nettoyage à la mort du boss.
 */
public class MinionManager {

    // Clé PDC pour identifier les sbires — utile pour l'AntiStack
    public static final String MINION_PDC_KEY = "guardian_minion";

    private final Main plugin;
    private final ConfigManager config;
    private final NamespacedKey minionKey;
    private final Random random = new Random();

    // Liste de tous les sbires vivants liés au boss actuel
    private final List<WitherSkeleton> minions = new ArrayList<>();

    public MinionManager(Main plugin, ConfigManager config) {
        this.plugin    = plugin;
        this.config    = config;
        this.minionKey = new NamespacedKey(plugin, MINION_PDC_KEY);
    }

    // ── API publique ─────────────────────────────────────────────

    /**
     * Spawne un certain nombre de sbires autour d'une position.
     */
    public void spawnWave(Location center, int count) {
        for (int i = 0; i < count; i++) {
            // Position aléatoire dans un carré de 5x5 autour du centre
            double offsetX = random.nextDouble() * 5 - 2.5;
            double offsetZ = random.nextDouble() * 5 - 2.5;
            Location loc = center.clone().add(offsetX, 0, offsetZ);

            WitherSkeleton minion = loc.getWorld().spawn(loc, WitherSkeleton.class);
            configureMinion(minion);
            minions.add(minion);
        }
    }

    /**
     * Appelé à chaque tick — nettoie les sbires morts de la liste.
     * Retourne la liste propre pour un éventuel usage externe.
     */
    public List<WitherSkeleton> tickAndGetAlive() {
        minions.removeIf(m -> !m.isValid());
        return minions;
    }

    /**
     * Tue et supprime tous les sbires restants (appelé à la mort du boss).
     */
    public void killAll() {
        for (WitherSkeleton minion : minions) {
            if (minion.isValid()) minion.remove();
        }
        minions.clear();
    }

    /**
     * Retourne true si l'entité est un sbire du Gardien.
     */
    public boolean isMinion(WitherSkeleton entity) {
        return entity.getPersistentDataContainer().has(minionKey, PersistentDataType.BYTE);
    }

    // ── Configuration du sbire ───────────────────────────────────

    private void configureMinion(WitherSkeleton minion) {
        // Identité
        minion.setCustomName(config.minionName);
        minion.setCustomNameVisible(true);
        minion.setPersistent(true);

        // Stats depuis config
        setAttribute(minion, Attribute.MAX_HEALTH,     config.minionHp);
        minion.setHealth(config.minionHp);
        setAttribute(minion, Attribute.ATTACK_DAMAGE,  config.minionDamage);
        setAttribute(minion, Attribute.MOVEMENT_SPEED, config.minionSpeed);
        setAttribute(minion, Attribute.FOLLOW_RANGE,   config.minionFollowRange);

        // Armure depuis config
        applyMinionArmor(minion);

        // Marqueur PDC — identifie ce sbire comme lié au Gardien
        minion.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, (byte) 1);

        // Tag scoreboard pour compatibilité StackMob
        minion.addScoreboardTag("guardian_minion");
    }

    private void applyMinionArmor(WitherSkeleton minion) {
        String mat = config.minionArmorMaterial; // ex: "IRON"

        Material helmet     = parseMaterial(mat + "_HELMET",     Material.IRON_HELMET);
        Material chestplate = parseMaterial(mat + "_CHESTPLATE", Material.IRON_CHESTPLATE);
        Material leggings   = parseMaterial(mat + "_LEGGINGS",   Material.IRON_LEGGINGS);
        Material boots      = parseMaterial(mat + "_BOOTS",      Material.IRON_BOOTS);

        minion.getEquipment().setHelmet    (buildPiece(helmet,     config.minionArmorEnchants));
        minion.getEquipment().setChestplate(buildPiece(chestplate, config.minionArmorEnchants));
        minion.getEquipment().setLeggings  (buildPiece(leggings,   config.minionArmorEnchants));
        minion.getEquipment().setBoots     (buildPiece(boots,      config.minionArmorEnchants));

        // Aucun drop d'armure
        minion.getEquipment().setHelmetDropChance    (0f);
        minion.getEquipment().setChestplateDropChance(0f);
        minion.getEquipment().setLeggingsDropChance  (0f);
        minion.getEquipment().setBootsDropChance     (0f);
    }

    private ItemStack buildPiece(Material mat, Map<String, Integer> enchants) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setUnbreakable(true);

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            NamespacedKey key = NamespacedKey.minecraft(entry.getKey().toLowerCase());
            Enchantment enchant = Registry.ENCHANTMENT.get(key);
            if (enchant != null) {
                meta.addEnchant(enchant, entry.getValue(), true);
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void setAttribute(WitherSkeleton entity, Attribute attr, double value) {
        var instance = entity.getAttribute(attr);
        if (instance != null) instance.setBaseValue(value);
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[MinionManager] Matériau invalide: '" + name
                    + "', utilisation de " + fallback.name());
            return fallback;
        }
    }
}
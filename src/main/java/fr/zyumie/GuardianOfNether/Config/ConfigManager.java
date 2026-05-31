package fr.zyumie.GuardianOfNether.Config;

import fr.zyumie.GuardianOfNether.Main;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Lit le config.yml une seule fois et expose toutes les valeurs
 * sous forme d'objets typés. Appelle reload() pour recharger à chaud.
 */
public class ConfigManager {

    private final Main plugin;

    // ── Boss général ────────────────────────────────────────────
    public String bossName;
    public int    leashRadius;
    public int    minionsOnSpawn;
    public int    minionsWaveCount;
    public int    minionsWaveInterval; // secondes

    // ── Phases ──────────────────────────────────────────────────
    public PhaseConfig phase1;
    public PhaseConfig phase2;
    public PhaseConfig phase3;

    // ── Minions ─────────────────────────────────────────────────
    public String minionName;
    public double minionHp;
    public double minionDamage;
    public double minionSpeed;
    public double minionFollowRange;
    public String minionArmorMaterial;
    public Map<String, Integer> minionArmorEnchants;

    // ── Drop ────────────────────────────────────────────────────
    public String  dropName;
    public String  dropMaterial;
    public boolean dropUnbreakable;
    public boolean dropCurseOfVanishing;
    public String  dropTrimMaterial;
    public String  dropTrimPattern;
    public Map<String, Integer> dropEnchants;

    // ── Comportement plastron ───────────────────────────────────
    public boolean chestGlowing;
    public boolean chestLockOnEquip;
    public boolean chestBlockContainer;

    // ── Nether ──────────────────────────────────────────────────
    public boolean netherClose;
    public boolean bossDead;

    // ── On-Death ────────────────────────────────────────────────
    public boolean clearInventory;
    public boolean clearEnderChest;
    public boolean killVillagers;
    public boolean giveChestplate;

    // ── Messages ────────────────────────────────────────────────
    public String msgSpawnBroadcast;
    public String msgSpawnTitle;
    public String msgSpawnSubtitle;
    public String msgDeathBroadcast;
    public String msgDeathLine;
    public String msgNetherOpen;
    public String msgNetherBlocked;
    public String msgChestplateReceived;
    public String msgPhase2;
    public String msgPhase3;

    // ────────────────────────────────────────────────────────────

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Recharge toutes les valeurs depuis le fichier config.yml.
     * Peut être appelé via une commande reload sans redémarrer le serveur.
     */
    public void reload() {
        plugin.reloadConfig();
        var cfg = plugin.getConfig();

        // ── Boss ──────────────────────────────────────────────
        bossName           = color(cfg.getString("Boss.name", "&4&lGardien du Nether"));
        leashRadius        = cfg.getInt("Boss.leash-radius", 20);
        minionsOnSpawn     = cfg.getInt("Boss.minions-on-spawn", 5);
        minionsWaveCount   = cfg.getInt("Boss.minions-wave-count", 3);
        minionsWaveInterval= cfg.getInt("Boss.minions-wave-interval", 60);

        // ── Phases ────────────────────────────────────────────
        phase1 = loadPhase("Phases.Phase-1", 0);
        phase2 = loadPhase("Phases.Phase-2", cfg.getInt("Phases.Phase-2.hp-threshold-percent", 60));
        phase3 = loadPhase("Phases.Phase-3", cfg.getInt("Phases.Phase-3.hp-threshold-percent", 30));

        // ── Minions ───────────────────────────────────────────
        minionName          = color(cfg.getString("Minions.name", "&6Sbire du Gardien"));
        minionHp            = cfg.getDouble("Minions.hp", 50.0);
        minionDamage        = cfg.getDouble("Minions.damage", 6.0);
        minionSpeed         = cfg.getDouble("Minions.speed", 0.25);
        minionFollowRange   = cfg.getDouble("Minions.follow-range", 16.0);
        minionArmorMaterial = cfg.getString("Minions.armor.material", "IRON").toUpperCase();
        minionArmorEnchants = loadEnchants(cfg.getConfigurationSection("Minions.armor.enchants"));

        // ── Drop ──────────────────────────────────────────────
        dropName             = color(cfg.getString("Drop.Chestplate.name", "&cChestplate of Guardian"));
        dropMaterial         = cfg.getString("Drop.Chestplate.material", "DIAMOND_CHESTPLATE").toUpperCase();
        dropUnbreakable      = cfg.getBoolean("Drop.Chestplate.unbreakable", true);
        dropCurseOfVanishing = cfg.getBoolean("Drop.Chestplate.curse-of-vanishing", true);
        dropTrimMaterial     = cfg.getString("Drop.Chestplate.trim.material", "REDSTONE").toUpperCase();
        dropTrimPattern      = cfg.getString("Drop.Chestplate.trim.pattern", "SENTRY").toUpperCase();
        dropEnchants         = loadEnchants(cfg.getConfigurationSection("Drop.Chestplate.enchants"));

        // ── Comportement plastron ─────────────────────────────
        chestGlowing        = cfg.getBoolean("Chestplate-Behavior.glowing", true);
        chestLockOnEquip    = cfg.getBoolean("Chestplate-Behavior.lock-on-equip", true);
        chestBlockContainer = cfg.getBoolean("Chestplate-Behavior.block-container-storage", true);

        // ── Nether ────────────────────────────────────────────
        netherClose = cfg.getBoolean("Nether.nether-close", true);
        bossDead    = cfg.getBoolean("Nether.boss-dead", false);

        // ── On-Death ──────────────────────────────────────────
        clearInventory  = cfg.getBoolean("On-Death.clear-inventory", false);
        clearEnderChest = cfg.getBoolean("On-Death.clear-enderchest", false);
        killVillagers   = cfg.getBoolean("On-Death.kill-villagers", false);
        giveChestplate  = cfg.getBoolean("On-Death.give-chestplate", true);

        // ── Messages ──────────────────────────────────────────
        msgSpawnBroadcast    = color(cfg.getString("Messages.spawn-broadcast", "&cLe Gardien est apparu !"));
        msgSpawnTitle        = color(cfg.getString("Messages.spawn-title",     "&4&lGardien du Nether"));
        msgSpawnSubtitle     = color(cfg.getString("Messages.spawn-subtitle",  "&6Préparez-vous..."));
        msgDeathBroadcast    = color(cfg.getString("Messages.death-broadcast", "&6Le Gardien a été vaincu !"));
        msgDeathLine         = color(cfg.getString("Messages.death-line",      "&c&lGardien&f: ..."));
        msgNetherOpen        = color(cfg.getString("Messages.nether-open",     "&6Le Nether est accessible !"));
        msgNetherBlocked     = color(cfg.getString("Messages.nether-blocked",  "&cNether bloqué !"));
        msgChestplateReceived= color(cfg.getString("Messages.chestplate-received", "&aVous avez reçu la Chestplate !"));
        msgPhase2            = color(cfg.getString("Messages.phase-2",         "&6Phase 2 !"));
        msgPhase3            = color(cfg.getString("Messages.phase-3",         "&4Phase 3 !"));
    }

    // ── Helpers privés ───────────────────────────────────────────

    /**
     * Charge une PhaseConfig depuis une section du config.
     * @param path            chemin YAML (ex: "Phases.Phase-2")
     * @param hpThreshold     seuil % de vie pour entrer dans cette phase
     */
    private PhaseConfig loadPhase(String path, int hpThreshold) {
        var cfg = plugin.getConfig();

        double maxHp      = cfg.getDouble(path + ".hp", 300.0);
        double speed      = cfg.getDouble(path + ".speed", 0.25);
        double scale      = cfg.getDouble(path + ".scale", 1.0);
        double damageMult = cfg.getDouble(path + ".damage-multiplier", 1.0);
        int    regenAmt   = cfg.getInt   (path + ".regen-amount", 0);
        int    regenIntvl = cfg.getInt   (path + ".regen-interval", 10);
        String armorMat   = cfg.getString(path + ".armor.material", "NETHERITE").toUpperCase();

        Map<String, Integer> enchants =
                loadEnchants(cfg.getConfigurationSection(path + ".armor.enchants"));

        Particle particle = parseParticle(cfg.getString(path + ".particles", "FLAME"));

        return new PhaseConfig(maxHp, speed, scale, damageMult,
                regenAmt, regenIntvl, armorMat,
                enchants, particle, hpThreshold);
    }

    /**
     * Lit une section YAML de type "enchant-name: niveau" et retourne une Map.
     * Retourne une Map vide si la section est absente.
     */
    private Map<String, Integer> loadEnchants(ConfigurationSection section) {
        Map<String, Integer> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key.toLowerCase(), section.getInt(key, 1));
        }
        return map;
    }

    /**
     * Convertit les codes couleur & en codes Minecraft §.
     */
    public static String color(String input) {
        if (input == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Essaie de parser une Particle depuis son nom.
     * Retourne FLAME par défaut si le nom est invalide.
     */
    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Config] Particule invalide: '" + name + "', utilisation de FLAME.");
            return Particle.FLAME;
        }
    }

    /**
     * Sauvegarde une clé dans le config (ex: boss-dead après victoire).
     */
    public void set(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }
}
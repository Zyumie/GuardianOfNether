package fr.zyumie.GuardianOfNether.Config;

import fr.zyumie.GuardianOfNether.Main;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lit les 3 fichiers de configuration et expose les valeurs sous forme
 * de champs typés :
 *   - Config.yml : général, plastron, protections, mort du boss
 *   - Boss.yml   : boss, phases, IA, attaques, sbires, messages du boss
 *   - Nether.yml : verrouillage du Nether
 *
 * Les clés manquantes retombent sur les valeurs par défaut du fichier
 * embarqué dans le jar : une mise à jour du plugin n'oblige pas à
 * régénérer les configs.
 */
public class ConfigManager {

    /** Identifiants des attaques lues dans Boss.yml (section Attacks). */
    private static final List<String> ATTACK_IDS = List.of(
            "magma-floor", "sword-rain", "fire-nova",
            "leap-slam", "meteor-shower", "vortex", "soul-lightning"
    );

    private final Main plugin;

    private YamlConfiguration mainCfg;
    private YamlConfiguration bossCfg;
    private YamlConfiguration netherCfg;
    private File netherFile;

    // ── Config.yml : général ────────────────────────────────────
    public boolean checkUpdates;

    // ── Config.yml : drop plastron ──────────────────────────────
    public String  dropName;
    public String  dropMaterial;
    public boolean dropUnbreakable;
    public boolean dropCurseOfVanishing;
    public String  dropTrimMaterial;
    public String  dropTrimPattern;
    public Map<String, Integer> dropEnchants;

    // ── Config.yml : comportement plastron ──────────────────────
    public boolean chestGlowing;
    public boolean chestLockOnEquip;

    // ── Config.yml : protection conteneurs ──────────────────────
    public boolean containerProtection;
    public boolean containerBlockAll;
    public Set<InventoryType> containerBlockedTypes;
    public boolean containerBlockBundles;
    public boolean containerBlockFrames;

    // ── Config.yml : règles du plastron ─────────────────────────
    public boolean chestEnchantable;
    public boolean chestAllowGrindstone;
    public boolean chestAllowTrim;
    public boolean chestAllowNetherite;

    // ── Config.yml : on-death ───────────────────────────────────
    public boolean clearInventory;
    public boolean clearEnderChest;
    public boolean killVillagers;
    public boolean giveChestplate;

    // ── Config.yml : messages ───────────────────────────────────
    public String msgChestplateReceived;
    public String msgContainerBlocked;
    public String msgRemoveBlocked;

    // ── Boss.yml : boss ─────────────────────────────────────────
    public String   bossName;
    public double   bossMaxHealth;
    public int      leashRadius;
    public boolean  removeBossOnDisable;
    public int      minionsOnSpawn;
    public int      minionsWaveCount;
    public int      minionsWaveInterval; // secondes
    public BarStyle bossBarStyle;
    public boolean  auraEnabled;
    public double   auraRadius;
    public double   auraDamage;
    public int      auraFireTicks;

    // ── Boss.yml : phases ───────────────────────────────────────
    public PhaseConfig phase1;
    public PhaseConfig phase2;
    public PhaseConfig phase3;

    // ── Boss.yml : IA ───────────────────────────────────────────
    public boolean aiEnabled;
    public double  aiAggroRadius;
    public int     aiRetargetSeconds;
    public String  aiTargetMode;
    public boolean aiTeleportEnabled;
    public double  aiTeleportDistance;
    public int     aiTeleportDelaySeconds;

    // ── Boss.yml : attaques ─────────────────────────────────────
    public double attackEngageRadius;
    public int    attackGlobalCooldown; // secondes
    public Map<String, AttackSettings> attacks = new LinkedHashMap<>();

    // ── Boss.yml : sbires ───────────────────────────────────────
    public String minionName;
    public double minionHp;
    public double minionDamage;
    public double minionSpeed;
    public double minionFollowRange;
    public int    minionsMaxAlive;
    public String minionArmorMaterial;
    public Map<String, Integer> minionArmorEnchants;

    // ── Boss.yml : messages ─────────────────────────────────────
    public String msgSpawnBroadcast;
    public String msgSpawnTitle;
    public String msgSpawnSubtitle;
    public String msgDeathBroadcast;
    public String msgDeathLine;
    public String msgPhase2;
    public String msgPhase3;

    // ── Nether.yml ──────────────────────────────────────────────
    public boolean netherClose;
    public boolean bossDead;
    public String  netherBypassPermission;
    public boolean netherBlockPortals;
    public boolean netherBlockEntities;
    public boolean netherBlockTeleports;
    public String  msgNetherOpen;
    public String  msgNetherBlocked;

    // ────────────────────────────────────────────────────────────

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Relit les 3 fichiers. Appelable à chaud via /guardian-of-nether reload.
     */
    public void reload() {
        netherFile = new File(plugin.getDataFolder(), "Nether.yml");

        mainCfg   = load("Config.yml");
        bossCfg   = load("Boss.yml");
        netherCfg = load("Nether.yml");

        readMain();
        readBoss();
        readAi();
        readAttacks();
        readMinions();
        readNether();
    }

    // ── Chargement des fichiers ──────────────────────────────────

    private YamlConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        InputStream res = plugin.getResource(name);
        if (res != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(res, StandardCharsets.UTF_8)));
        }
        return yaml;
    }

    // ── Config.yml ───────────────────────────────────────────────

    private void readMain() {
        var c = mainCfg;

        checkUpdates = c.getBoolean("General.check-updates", true);

        // Drop
        dropName             = color(c.getString("Drop.Chestplate.name", "&cChestplate of Guardian"));
        dropMaterial         = c.getString("Drop.Chestplate.material", "DIAMOND_CHESTPLATE").toUpperCase();
        dropUnbreakable      = c.getBoolean("Drop.Chestplate.unbreakable", true);
        dropCurseOfVanishing = c.getBoolean("Drop.Chestplate.curse-of-vanishing", true);
        dropTrimMaterial     = c.getString("Drop.Chestplate.trim.material", "REDSTONE").toUpperCase();
        dropTrimPattern      = c.getString("Drop.Chestplate.trim.pattern", "SENTRY").toUpperCase();
        dropEnchants         = loadEnchants(c.getConfigurationSection("Drop.Chestplate.enchants"));

        // Comportement
        chestGlowing     = c.getBoolean("Chestplate-Behavior.glowing", true);
        chestLockOnEquip = c.getBoolean("Chestplate-Behavior.lock-on-equip", true);

        // Protection conteneurs
        containerProtection   = c.getBoolean("Container-Protection.enabled", true);
        containerBlockAll     = c.getBoolean("Container-Protection.block-all", false);
        containerBlockBundles = c.getBoolean("Container-Protection.block-bundles", true);
        containerBlockFrames  = c.getBoolean("Container-Protection.block-item-frames", true);

        containerBlockedTypes = EnumSet.noneOf(InventoryType.class);
        for (String raw : c.getStringList("Container-Protection.blocked-types")) {
            try {
                containerBlockedTypes.add(InventoryType.valueOf(raw.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Config] Type d'inventaire inconnu dans blocked-types: '" + raw + "'");
            }
        }

        // Règles
        chestEnchantable     = c.getBoolean("Chestplate-Rules.enchantable", false);
        chestAllowGrindstone = c.getBoolean("Chestplate-Rules.allow-grindstone", false);
        chestAllowTrim       = c.getBoolean("Chestplate-Rules.allow-trim", false);
        chestAllowNetherite  = c.getBoolean("Chestplate-Rules.allow-netherite-upgrade", false);

        // On-Death
        clearInventory  = c.getBoolean("On-Death.clear-inventory", false);
        clearEnderChest = c.getBoolean("On-Death.clear-enderchest", false);
        killVillagers   = c.getBoolean("On-Death.kill-villagers", false);
        giveChestplate  = c.getBoolean("On-Death.give-chestplate", true);

        // Messages
        msgChestplateReceived = color(c.getString("Messages.chestplate-received", "&aVous avez reçu la Chestplate !"));
        msgContainerBlocked   = color(c.getString("Messages.container-blocked", "&cVous ne pouvez pas stocker la Chestplate of Guardian !"));
        msgRemoveBlocked      = color(c.getString("Messages.remove-blocked", "&cVous ne pouvez pas retirer la Chestplate of Guardian !"));
    }

    // ── Boss.yml ─────────────────────────────────────────────────

    private void readBoss() {
        var c = bossCfg;

        bossName            = color(c.getString("Boss.name", "&4&lGardien du Nether"));
        bossMaxHealth       = Math.max(1.0, c.getDouble("Boss.max-health", 300.0));
        leashRadius         = Math.max(1, c.getInt("Boss.leash-radius", 20));
        removeBossOnDisable = c.getBoolean("Boss.remove-on-disable", true);
        minionsOnSpawn      = c.getInt("Boss.minions-on-spawn", 5);
        minionsWaveCount    = c.getInt("Boss.minions-wave-count", 3);
        minionsWaveInterval = Math.max(1, c.getInt("Boss.minions-wave-interval", 60));
        bossBarStyle        = parseBarStyle(c.getString("Boss.bossbar-style", "SEGMENTED_10"));

        auraEnabled   = c.getBoolean("Boss.aura.enabled", true);
        auraRadius    = c.getDouble("Boss.aura.radius", 10.0);
        auraDamage    = c.getDouble("Boss.aura.base-damage", 8.0);
        auraFireTicks = c.getInt("Boss.aura.fire-ticks", 80);

        phase1 = loadPhase("Phases.Phase-1", 0);
        phase2 = loadPhase("Phases.Phase-2", c.getInt("Phases.Phase-2.hp-threshold-percent", 60));
        phase3 = loadPhase("Phases.Phase-3", c.getInt("Phases.Phase-3.hp-threshold-percent", 30));

        msgSpawnBroadcast = color(c.getString("Messages.spawn-broadcast", "&cLe Gardien est apparu !"));
        msgSpawnTitle     = color(c.getString("Messages.spawn-title", "&4&lGardien du Nether"));
        msgSpawnSubtitle  = color(c.getString("Messages.spawn-subtitle", "&6Préparez-vous..."));
        msgDeathBroadcast = color(c.getString("Messages.death-broadcast", "&6Le Gardien a été vaincu !"));
        msgDeathLine      = color(c.getString("Messages.death-line", "&c&lGardien&f: ..."));
        msgPhase2         = color(c.getString("Messages.phase-2", "&6Phase 2 !"));
        msgPhase3         = color(c.getString("Messages.phase-3", "&4Phase 3 !"));
    }

    private void readAi() {
        var c = bossCfg;

        aiEnabled              = c.getBoolean("AI.enabled", true);
        aiAggroRadius          = c.getDouble("AI.aggro-radius", 30.0);
        aiRetargetSeconds      = Math.max(1, c.getInt("AI.retarget-interval", 3));
        aiTargetMode           = c.getString("AI.target-mode", "NEAREST").toUpperCase();
        aiTeleportEnabled      = c.getBoolean("AI.teleport.enabled", true);
        aiTeleportDistance     = c.getDouble("AI.teleport.distance", 12.0);
        aiTeleportDelaySeconds = Math.max(1, c.getInt("AI.teleport.delay", 4));
    }

    private void readAttacks() {
        var c = bossCfg;

        attackEngageRadius   = c.getDouble("Attacks.engage-radius", 40.0);
        attackGlobalCooldown = Math.max(0, c.getInt("Attacks.global-cooldown", 4));

        attacks = new LinkedHashMap<>();
        for (String id : ATTACK_IDS) {
            ConfigurationSection section = c.getConfigurationSection("Attacks." + id);
            if (section != null) attacks.put(id, new AttackSettings(section));
        }
    }

    private void readMinions() {
        var c = bossCfg;

        minionName          = color(c.getString("Minions.name", "&6Sbire du Gardien"));
        minionHp            = c.getDouble("Minions.hp", 50.0);
        minionDamage        = c.getDouble("Minions.damage", 6.0);
        minionSpeed         = c.getDouble("Minions.speed", 0.25);
        minionFollowRange   = c.getDouble("Minions.follow-range", 16.0);
        minionsMaxAlive     = c.getInt("Minions.max-alive", 20);
        minionArmorMaterial = c.getString("Minions.armor.material", "IRON").toUpperCase();
        minionArmorEnchants = loadEnchants(c.getConfigurationSection("Minions.armor.enchants"));
    }

    // ── Nether.yml ───────────────────────────────────────────────

    private void readNether() {
        var c = netherCfg;

        netherClose            = c.getBoolean("nether-close", true);
        bossDead               = c.getBoolean("boss-dead", false);
        netherBypassPermission = c.getString("bypass-permission", "guardianofnether.bypass");
        netherBlockPortals     = c.getBoolean("block.portals", true);
        netherBlockEntities    = c.getBoolean("block.entities", true);
        netherBlockTeleports   = c.getBoolean("block.teleports", true);

        msgNetherOpen    = color(c.getString("Messages.nether-open", "&6Le Nether est accessible !"));
        msgNetherBlocked = color(c.getString("Messages.nether-blocked", "&cNether bloqué !"));
    }

    /**
     * Enregistre l'état « boss vaincu » dans Nether.yml.
     */
    public void setBossDead(boolean value) {
        bossDead = value;
        netherCfg.set("boss-dead", value);
        try {
            netherCfg.save(netherFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Config] Impossible de sauvegarder Nether.yml : " + e.getMessage());
        }
    }

    // ── Accès pratique ───────────────────────────────────────────

    public PhaseConfig phase(int number) {
        return switch (number) {
            case 2  -> phase2;
            case 3  -> phase3;
            default -> phase1;
        };
    }

    // ── Helpers privés ───────────────────────────────────────────

    private PhaseConfig loadPhase(String path, int hpThreshold) {
        var c = bossCfg;

        double speed      = c.getDouble(path + ".speed", 0.25);
        double scale      = c.getDouble(path + ".scale", 1.0);
        double damageMult = c.getDouble(path + ".damage-multiplier", 1.0);
        int    regenAmt   = c.getInt(path + ".regen-amount", 0);
        int    regenIntvl = Math.max(1, c.getInt(path + ".regen-interval", 10));
        String armorMat   = c.getString(path + ".armor.material", "NETHERITE").toUpperCase();

        Map<String, Integer> enchants =
                loadEnchants(c.getConfigurationSection(path + ".armor.enchants"));

        Particle particle = parseParticle(c.getString(path + ".particles", "FLAME"));
        BarColor barColor = parseBarColor(c.getString(path + ".bossbar-color", "PURPLE"));
        String title      = color(c.getString(path + ".title", ""));
        String subtitle   = color(c.getString(path + ".subtitle", ""));

        return new PhaseConfig(speed, scale, damageMult,
                regenAmt, regenIntvl, armorMat,
                enchants, particle, hpThreshold,
                barColor, title, subtitle);
    }

    /**
     * Lit une section « enchant: niveau » et retourne une Map (vide si absente).
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

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Config] Particule invalide: '" + name + "', utilisation de FLAME.");
            return Particle.FLAME;
        }
    }

    private BarColor parseBarColor(String name) {
        try {
            return BarColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Config] bossbar-color invalide: '" + name + "', utilisation de PURPLE.");
            return BarColor.PURPLE;
        }
    }

    private BarStyle parseBarStyle(String name) {
        try {
            return BarStyle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Config] bossbar-style invalide: '" + name + "', utilisation de SEGMENTED_10.");
            return BarStyle.SEGMENTED_10;
        }
    }
}

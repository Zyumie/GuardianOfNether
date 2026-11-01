package fr.zyumie.GuardianOfNether;

import fr.zyumie.GuardianOfNether.Boss.GuardianBoss;
import fr.zyumie.GuardianOfNether.Commandes.ItemCommand;
import fr.zyumie.GuardianOfNether.Commandes.SpawnCommand;
import fr.zyumie.GuardianOfNether.Manager.RewardManager;
import fr.zyumie.GuardianOfNether.SoftDepend.StackMobHook;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.SoftDepend.AntiStack;
import fr.zyumie.GuardianOfNether.Listener.ArmorListener;
import fr.zyumie.GuardianOfNether.Listener.BossListener;
import fr.zyumie.GuardianOfNether.Listener.ChestplateRulesListener;
import fr.zyumie.GuardianOfNether.Manager.VersionManager;
import fr.zyumie.GuardianOfNether.Listener.NetherListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    // UUIDs des boss actifs — utilisé par les listeners pour identifier l'entité
    public static final Set<UUID> trackedBosses = new HashSet<>();

    // ── Singletons accessibles globalement ───────────────────────
    private ConfigManager  configManager;
    private RewardManager rewardManager;
    private NetherListener netherListener;
    private BossListener   bossListener;
    private VersionManager versionManager;
    private StackMobHook   stackMobHook;

    @Override
    public void onEnable() {
        // 1. Configs — toujours en premier
        //    (Config.yml, Boss.yml et Nether.yml sont créés au besoin par le ConfigManager)
        configManager = new ConfigManager(this);

        // 2. Managers indépendants
        rewardManager  = new RewardManager(this, configManager);
        netherListener = new NetherListener(this, configManager);

        // 3. BossListener — a besoin de reward + nether
        bossListener = new BossListener(this, configManager, rewardManager, netherListener);

        // 4. Listeners
        getServer().getPluginManager().registerEvents(bossListener,   this);
        getServer().getPluginManager().registerEvents(netherListener,  this);
        getServer().getPluginManager().registerEvents(
                new ArmorListener(this, configManager, rewardManager), this);
        getServer().getPluginManager().registerEvents(
                new ChestplateRulesListener(configManager, rewardManager), this);

        // 5. StackMob (soft-depend — enregistré seulement si présent)
        stackMobHook = new StackMobHook();
        if (stackMobHook.isEnabled()) {
            getServer().getPluginManager().registerEvents(
                    new AntiStack(stackMobHook), this);
            getLogger().info("StackMob détecté — AntiStack activé.");
        }

        // 6. VersionManager
        versionManager = new VersionManager(this);
        getServer().getPluginManager().registerEvents(versionManager, this);
        if (configManager.checkUpdates) {
            versionManager.checkAsync();
        }

        // 7. Commandes
        SpawnCommand spawnCmd = new SpawnCommand(this, configManager, bossListener, netherListener);
        getCommand("guardian-of-nether").setExecutor(spawnCmd);
        getCommand("guardian-of-nether").setTabCompleter(spawnCmd);

        ItemCommand itemCmd = new ItemCommand(rewardManager);
        getCommand("guardian-items").setExecutor(itemCmd);
        getCommand("guardian-items").setTabCompleter(itemCmd);

        getLogger().info("GuardianOfNether v" + getDescription().getVersion() + " activé !");
    }

    @Override
    public void onDisable() {
        // Arrêt propre du boss actif : restaure les blocs de magma, retire
        // les sbires, la bossbar et (selon Boss.yml) le boss lui-même
        if (bossListener != null) {
            GuardianBoss active = bossListener.getActiveBoss();
            if (active != null) active.shutdown();
        }
        trackedBosses.clear();
        getLogger().info("GuardianOfNether désactivé !");
    }

    // ── Getters ──────────────────────────────────────────────────

    public ConfigManager  getConfigManager()  { return configManager;  }
    public RewardManager  getRewardManager()  { return rewardManager;  }
    public NetherListener getNetherListener() { return netherListener; }
    public BossListener   getBossListener()   { return bossListener;   }
}

package fr.zyumie.GuardianOfNether;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import fr.zyumie.Commandes.GuardianCommand;
import fr.zyumie.Commandes.GuardianItems;
import fr.zyumie.Listener.*;
import fr.zyumie.SoftDepend.StackMobHook;

public class Main extends JavaPlugin {

    public static Set<java.util.UUID> trackedBosses = new java.util.HashSet<>();

    private StackMobHook stackMobHook;
    private GuardianOfNether guardian;
    private BossManager bossListener;
    private NetherManager netherListener;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        reloadConfig();
        
        
		// CheckVersion
		VersionManager.check(this);
        
        
        // Instanciation des classes
        guardian = new GuardianOfNether(this);
        netherListener = new NetherManager(this);
        bossListener = new BossManager(this, guardian, netherListener);
      
        
     // StackMob (soft depend)
        stackMobHook = new StackMobHook();

        if (stackMobHook.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(
                new AntiStack(stackMobHook),
                this
            );
        }
                
        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(bossListener, this);
        getServer().getPluginManager().registerEvents(netherListener, this);
        getServer().getPluginManager().registerEvents(new ArmorGlow(this), this);
      
        
        // Commandes
        getCommand("guardian-of-nether").setExecutor(new GuardianCommand(guardian));
        getCommand("guardian-items").setExecutor(new GuardianItems(this));

        getLogger().info("GuardianOfNether activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("GuardianOfNether désactivé !");
    }

    public GuardianOfNether getGuardian() {
        return guardian;
    }

    public NetherManager getNetherListener() {
        return netherListener;
    }
}

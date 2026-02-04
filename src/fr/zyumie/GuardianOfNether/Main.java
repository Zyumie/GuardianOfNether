package fr.zyumie.GuardianOfNether;

import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;

import fr.zyumie.Commandes.GuardianCommand;
import fr.zyumie.Commandes.GuardianItems;
import fr.zyumie.Listener.*;

public class Main extends JavaPlugin {

    public static Set<java.util.UUID> trackedBosses = new java.util.HashSet<>();
    private GuardianOfNether guardian;
    private BossListener bossListener;
    private NetherListener netherListener;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        reloadConfig();
        
        
		// CheckVersion
		VersionManager.check(this);
        
        
        // Instanciation des classes
        guardian = new GuardianOfNether(this);
        netherListener = new NetherListener(this);
        bossListener = new BossListener(this, guardian, netherListener);
      
        
        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(bossListener, this);
        getServer().getPluginManager().registerEvents(netherListener, this);
        getServer().getPluginManager().registerEvents(new AntiStackListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorGlowListener(this), this);
      
        
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

    public NetherListener getNetherListener() {
        return netherListener;
    }
}

package fr.zyumie.GuardianOfNether;

import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public static Set<java.util.UUID> trackedBosses = new java.util.HashSet<>();
    private GuardianOfNether guardian;

    @Override
    public void onEnable() {

    	saveDefaultConfig();
    	reloadConfig(); 

        this.guardian = new GuardianOfNether(this);
        getCommand("guardian-of-nether").setExecutor(new GuardianCommand(guardian));
        getServer().getPluginManager().registerEvents(new BossListener(this, guardian), this);
        getServer().getPluginManager().registerEvents(new AntiStackListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorGlowListener(this), this);
        this.getCommand("guardian-items").setExecutor(new GuardianItems(this));
        getLogger().info("GuardianOfNether activé !");
    }


    @Override
    public void onDisable() {
        getLogger().info("GuardianOfNether désactivé !");
    }
}
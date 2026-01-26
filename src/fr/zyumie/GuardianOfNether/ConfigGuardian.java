package fr.zyumie.GuardianOfNether;

import org.bukkit.plugin.java.JavaPlugin;

public class ConfigGuardian {

    private final JavaPlugin plugin;

    public boolean clearInventory;
    public boolean clearEnderChest;
    public boolean killVillagers;
    public boolean givePlastron;
    public boolean netherClose;

    public ConfigGuardian(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        clearInventory = plugin.getConfig().getBoolean("Clear-Stuff.clear-inventory", false);
        clearEnderChest = plugin.getConfig().getBoolean("Clear-Stuff.clear-enderchest", false);
        killVillagers = plugin.getConfig().getBoolean("Clear-Stuff.kill-villagers", false);
        givePlastron = plugin.getConfig().getBoolean("Give-Plastron", true);
        netherClose = plugin.getConfig().getBoolean("Nether.Nether-Close", true);
    }
}
package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Manager.RewardManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Applique les règles « Chestplate-Rules » de Config.yml :
 *  - enchantable          → table d'enchantement + enclume
 *  - allow-grindstone     → meule (retire les enchantements)
 *  - allow-trim           → table de forgeron (trim)
 *  - allow-netherite-upgrade → table de forgeron (upgrade netherite)
 *
 * On vide simplement le résultat : le joueur voit une sortie vide.
 */
public class ChestplateRulesListener implements Listener {

    private final ConfigManager config;
    private final RewardManager rewardManager;

    public ChestplateRulesListener(ConfigManager config, RewardManager rewardManager) {
        this.config        = config;
        this.rewardManager = rewardManager;
    }

    // ── Enclume ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (config.chestEnchantable) return;
        var inv = event.getInventory();
        if (isGuardian(inv.getItem(0)) || isGuardian(inv.getItem(1))) {
            event.setResult(null);
        }
    }

    // ── Table d'enchantement ─────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (config.chestEnchantable) return;
        if (isGuardian(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchant(EnchantItemEvent event) {
        if (config.chestEnchantable) return;
        if (isGuardian(event.getItem())) event.setCancelled(true);
    }

    // ── Meule ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (config.chestAllowGrindstone) return;
        var inv = event.getInventory();
        if (isGuardian(inv.getItem(0)) || isGuardian(inv.getItem(1))) {
            event.setResult(null);
        }
    }

    // ── Table de forgeron : trim + upgrade netherite ─────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        var inv = event.getInventory();
        ItemStack template  = inv.getItem(0);
        ItemStack equipment = inv.getItem(1);

        if (!isGuardian(equipment)) return;

        boolean netheriteUpgrade = template != null
                && template.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;

        if (netheriteUpgrade && !config.chestAllowNetherite) {
            event.setResult(null);
        } else if (!netheriteUpgrade && !config.chestAllowTrim) {
            event.setResult(null);
        }
    }

    // ── Helper ───────────────────────────────────────────────────

    private boolean isGuardian(ItemStack item) {
        return rewardManager.isGuardianChestplate(item);
    }
}
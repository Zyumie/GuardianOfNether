package fr.zyumie.GuardianOfNether;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ArmorGlowListener implements Listener {

    private final Main plugin;

    public ArmorGlowListener(Main plugin) {
        this.plugin = plugin;
    }

    // Vérifie si l’item est le plastron custom du boss
    private boolean isGuardianChest(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
			return false;
		}
        return item.getItemMeta().getCustomModelData() == 1;
    }

    // Vérifie si le joueur porte le plastron
    private boolean hasGuardianChestEquipped(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        return isGuardianChest(chest);
    }

    // Applique ou retire l’effet glowing selon l’armure
    private void updateGlowingEffect(Player player) {
        if (hasGuardianChestEquipped(player)) {
            if (!player.hasPotionEffect(PotionEffectType.GLOWING)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, true));
            }
        } else {
            if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }

    // Vérifie régulièrement les changements de position
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        updateGlowingEffect(event.getPlayer());
    }

    // Vérifie après un clic d’inventaire
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateGlowingEffect(player), 1L);
    }

    // Vérifie après un drag d’inventaire
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateGlowingEffect(player), 1L);
    }

    // Vérifie quand le joueur clique droit pour s'équiper
    @EventHandler
    public void onArmorRightClick(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) || (event.getItem() == null) || !isGuardianChest(event.getItem())) {
			return;
		}

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateGlowingEffect(player), 1L);
    }

    // Vérifie à la reconnexion
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateGlowingEffect(event.getPlayer());
    }
}

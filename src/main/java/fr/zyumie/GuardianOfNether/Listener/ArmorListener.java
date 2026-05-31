package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ArmorListener implements Listener {

	private final Main          plugin;
	private final ConfigManager config;
	private final RewardManager rewardManager;

	public ArmorListener(Main plugin, ConfigManager config, RewardManager rewardManager) {
		this.plugin        = plugin;
		this.config        = config;
		this.rewardManager = rewardManager;

		// Scheduler léger toutes les 2s — rattrape les cas non couverts par les events
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			for (Player p : Bukkit.getOnlinePlayers()) {
				updateGlow(p);
			}
		}, 40L, 40L);
	}

	// ── Glow ────────────────────────────────────────────────────

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Bukkit.getScheduler().runTaskLater(plugin, () ->
				updateGlow(event.getPlayer()), 1L);
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Bukkit.getScheduler().runTaskLater(plugin, () ->
				updateGlow(event.getPlayer()), 1L);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;

		// Blocage conteneur
		if (config.chestLockOnEquip) {
			if (handleLock(event, player)) return;
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> updateGlow(player), 1L);
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;

		if (config.chestBlockContainer && isGuardianInDrag(event)) {
			event.setCancelled(true);
			player.sendMessage("§cVous ne pouvez pas stocker la Chestplate of Guardian !");
			return;
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> updateGlow(player), 1L);
	}

	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR
				&& event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getItem() == null) return;
		if (!rewardManager.isGuardianChestplate(event.getItem())) return;

		Bukkit.getScheduler().runTaskLater(plugin, () ->
				updateGlow(event.getPlayer()), 1L);
	}

	// ── Lock ────────────────────────────────────────────────────

	private boolean handleLock(InventoryClickEvent event, Player player) {
		ItemStack current = event.getCurrentItem();
		ItemStack cursor  = event.getCursor();

		boolean currentIsGuardian = rewardManager.isGuardianChestplate(current);
		boolean cursorIsGuardian  = rewardManager.isGuardianChestplate(cursor);

		if (!currentIsGuardian && !cursorIsGuardian) return false;

		// Blocage retrait depuis le slot armure
		if (currentIsGuardian && isArmorSlot(event)) {
			event.setCancelled(true);
			player.sendMessage("§cVous ne pouvez pas retirer la Chestplate of Guardian !");
			return true;
		}

		// Blocage dépôt dans conteneur externe
		if (config.chestBlockContainer && isExternalContainer(event)) {
			event.setCancelled(true);
			player.sendMessage("§cVous ne pouvez pas stocker la Chestplate of Guardian !");
			return true;
		}

		// Blocage shift-clic vers conteneur
		if (currentIsGuardian
				&& config.chestBlockContainer
				&& event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
				&& event.getInventory().getType() != InventoryType.PLAYER) {
			event.setCancelled(true);
			player.sendMessage("§cVous ne pouvez pas stocker la Chestplate of Guardian !");
			return true;
		}

		return false;
	}

	private boolean isArmorSlot(InventoryClickEvent event) {
		if (event.getSlotType() == InventoryType.SlotType.ARMOR) return true;
		if (event.getClick() == ClickType.SHIFT_LEFT
				|| event.getClick() == ClickType.SHIFT_RIGHT) {
			return rewardManager.isGuardianChestplate(event.getCurrentItem());
		}
		return false;
	}

	private boolean isExternalContainer(InventoryClickEvent event) {
		InventoryType type = event.getInventory().getType();
		return type == InventoryType.CHEST
				|| type == InventoryType.ENDER_CHEST
				|| type == InventoryType.BARREL
				|| type == InventoryType.SHULKER_BOX
				|| type == InventoryType.HOPPER
				|| type == InventoryType.DROPPER
				|| type == InventoryType.DISPENSER;
	}

	private boolean isGuardianInDrag(InventoryDragEvent event) {
		if (!rewardManager.isGuardianChestplate(event.getOldCursor())) return false;
		InventoryType type = event.getInventory().getType();
		return type != InventoryType.PLAYER && type != InventoryType.CRAFTING;
	}

	// ── Glow helper ─────────────────────────────────────────────

	private void updateGlow(Player player) {
		if (!config.chestGlowing) return;

		boolean hasChest = rewardManager.isGuardianChestplate(
				player.getInventory().getChestplate()
		);

		if (hasChest && !player.hasPotionEffect(PotionEffectType.GLOWING)) {
			player.addPotionEffect(
					new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, true)
			);
		} else if (!hasChest && player.hasPotionEffect(PotionEffectType.GLOWING)) {
			player.removePotionEffect(PotionEffectType.GLOWING);
		}
	}
}
package fr.zyumie.GuardianOfNether.Listener;

import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gère le plastron du Gardien :
 *  - verrouillage dans le slot d'armure (lock-on-equip)
 *  - protection contre tout stockage (coffres, enderchest, sacs, cadres...)
 *    via TOUS les chemins : clic, shift-clic, touches 1-9, touche F,
 *    glisser-déposer, entonnoirs
 *  - effet Glowing quand il est porté
 *
 * Le shift-clic / clic droit pour ÉQUIPER le plastron reste autorisé.
 */
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

	// ── Glow : events de rattrapage ─────────────────────────────

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		scheduleGlow(event.getPlayer());
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		scheduleGlow(event.getPlayer());
	}

	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR
				&& event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getItem() == null) return;
		if (!rewardManager.isGuardianChestplate(event.getItem())) return;

		scheduleGlow(event.getPlayer());
	}

	// ── Clic dans un inventaire ─────────────────────────────────

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;

		ItemStack current = event.getCurrentItem();
		ItemStack cursor  = event.getCursor();
		boolean currentG  = isGuardian(current);
		boolean cursorG   = isGuardian(cursor);

		// 1. Retrait depuis le slot d'armure (clic, shift-clic, touches 1-9, F, Q...)
		if (config.chestLockOnEquip && currentG && isOwnArmorSlot(event)) {
			event.setCancelled(true);
			player.sendMessage(config.msgRemoveBlocked);
			return;
		}

		if (config.containerProtection) {
			// 2. Sacs (bundles) : l'item peut y être glissé sans ouvrir de conteneur
			if (config.containerBlockBundles
					&& ((currentG && isBundle(cursor)) || (cursorG && isBundle(current)))) {
				event.setCancelled(true);
				player.sendMessage(config.msgContainerBlocked);
				return;
			}

			// 3. Conteneurs ouverts (enderchest, coffre, ...)
			if (wouldEnterBlockedInventory(event, player, currentG, cursorG)) {
				event.setCancelled(true);
				player.sendMessage(config.msgContainerBlocked);
				return;
			}
		}

		scheduleGlow(player);
	}

	/**
	 * Vrai si ce clic ferait entrer le plastron dans l'inventaire du HAUT
	 * (celui du conteneur) alors que ce type est bloqué.
	 *
	 * Les chemins d'exploit couverts :
	 *  - clic avec le plastron sur le curseur
	 *  - touches 1-9 (échange avec la hotbar) — ni current ni cursor ne sont
	 *    le plastron, c'est l'item de la hotbar !
	 *  - touche F (échange avec la main secondaire)
	 *  - shift-clic depuis l'inventaire du joueur
	 */
	private boolean wouldEnterBlockedInventory(InventoryClickEvent event, Player player,
	                                           boolean currentG, boolean cursorG) {
		Inventory top = event.getView().getTopInventory();
		if (!isBlockedType(top.getType())) return false;
		if (event.getClickedInventory() == null) return false;

		boolean clickedTop = event.getRawSlot() < top.getSize();

		if (clickedTop) {
			if (cursorG) return true;

			ClickType click = event.getClick();
			if (click == ClickType.NUMBER_KEY) {
				int button = event.getHotbarButton();
				if (button >= 0 && isGuardian(player.getInventory().getItem(button))) return true;
			}
			if (click == ClickType.SWAP_OFFHAND
					&& isGuardian(player.getInventory().getItemInOffHand())) return true;

			return false;
		}

		// Clic dans l'inventaire du joueur : le shift-clic envoie vers le conteneur
		return currentG
				&& (event.isShiftClick()
				|| event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY);
	}

	// ── Glisser-déposer ─────────────────────────────────────────

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;

		if (config.containerProtection && isGuardian(event.getOldCursor())) {
			Inventory top = event.getView().getTopInventory();
			if (isBlockedType(top.getType())) {
				int topSize = top.getSize();
				for (int rawSlot : event.getRawSlots()) {
					if (rawSlot < topSize) {
						event.setCancelled(true);
						player.sendMessage(config.msgContainerBlocked);
						return;
					}
				}
			}
		}

		scheduleGlow(player);
	}

	// ── Entonnoirs / cadres / armor stands ──────────────────────

	/** Un entonnoir sous le plastron jeté au sol ne doit pas l'aspirer. */
	@EventHandler(ignoreCancelled = true)
	public void onHopperPickup(InventoryPickupItemEvent event) {
		if (!config.containerProtection) return;
		if (!isGuardian(event.getItem().getItemStack())) return;
		if (isBlockedType(event.getInventory().getType())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		if (!config.containerProtection || !config.containerBlockFrames) return;

		Entity clicked = event.getRightClicked();
		if (!(clicked instanceof ItemFrame) && !(clicked instanceof ArmorStand)) return;

		PlayerInventory inv = event.getPlayer().getInventory();
		if (isGuardian(inv.getItem(event.getHand()))) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(config.msgContainerBlocked);
		}
	}

	// ── Helpers ─────────────────────────────────────────────────

	private boolean isGuardian(ItemStack item) {
		return rewardManager.isGuardianChestplate(item);
	}

	private boolean isBundle(ItemStack item) {
		return item != null && item.getType().name().endsWith("BUNDLE");
	}

	/** Vrai si ce type d'inventaire est protégé (block-all ou liste blocked-types). */
	private boolean isBlockedType(InventoryType type) {
		if (config.containerBlockAll) {
			return type != InventoryType.PLAYER
					&& type != InventoryType.CRAFTING
					&& type != InventoryType.CREATIVE;
		}
		return config.containerBlockedTypes.contains(type);
	}

	/** Clic sur un slot d'armure du joueur (le shift-clic depuis ailleurs n'est PAS concerné). */
	private boolean isOwnArmorSlot(InventoryClickEvent event) {
		return event.getSlotType() == InventoryType.SlotType.ARMOR
				&& event.getClickedInventory() instanceof PlayerInventory;
	}

	private void scheduleGlow(Player player) {
		Bukkit.getScheduler().runTaskLater(plugin, () -> updateGlow(player), 1L);
	}

	private void updateGlow(Player player) {
		if (!config.chestGlowing) return;

		boolean hasChest = isGuardian(player.getInventory().getChestplate());

		if (hasChest && !player.hasPotionEffect(PotionEffectType.GLOWING)) {
			player.addPotionEffect(
					new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, true)
			);
		} else if (!hasChest && player.hasPotionEffect(PotionEffectType.GLOWING)) {
			player.removePotionEffect(PotionEffectType.GLOWING);
		}
	}
}

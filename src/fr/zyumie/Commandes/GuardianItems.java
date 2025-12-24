package fr.zyumie.Commandes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.zyumie.GuardianOfNether.Main;

public class GuardianItems implements CommandExecutor {

	private final Main plugin;

	public GuardianItems(Main plugin) {
		this.plugin = plugin;
	}

	// --- Méthode statique pour récupérer le plastron custom ---
	public static ItemStack getGuardianChest() {
		ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
		ItemMeta meta = chest.getItemMeta();
		meta.setDisplayName("§6Plastron Of GuardianNether");
		meta.addEnchant(Enchantment.PROTECTION, 6, true); // même enchant
		meta.setUnbreakable(true);
		meta.setCustomModelData(1); // permet d’identifier pour effet Glowing
		chest.setItemMeta(meta);
		return chest;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("tomboss.give.guardianitems")) {
			sender.sendMessage("§cVous n'avez pas la permission !");
			return true;
		}

		Player target;
		if (args.length > 0) {
			target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				sender.sendMessage("§cJoueur introuvable !");
				return true;
			}
		} else if (sender instanceof Player p) {
			target = p;
		} else {
			sender.sendMessage("§cVous devez spécifier un joueur !");
			return true;
		}

		// Utiliser la méthode statique pour créer le plastron
		ItemStack chest = getGuardianChest();
		target.getInventory().addItem(chest);
		target.sendMessage("§aVous avez reçu le Plastron Of GuardianNether !");
		sender.sendMessage("§aLe plastron a été donné à " + target.getName());
		return true;
	}
}

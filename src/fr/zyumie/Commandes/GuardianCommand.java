package fr.zyumie.Commandes;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.zyumie.GuardianOfNether.GuardianOfNether;

public class GuardianCommand implements CommandExecutor {

	private final GuardianOfNether guardian;

	public GuardianCommand(GuardianOfNether guardian) {
		this.guardian = guardian;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("tomboss.spawn.gardianofnether")) {
			sender.sendMessage("§cVous n'avez pas la permission !");
			return true;
		}

		Location spawnLoc;
		if (args.length == 3 && sender instanceof Player player) {
			try {
				double x = Double.parseDouble(args[0]);
				double y = Double.parseDouble(args[1]);
				double z = Double.parseDouble(args[2]);
				spawnLoc = new Location(player.getWorld(), x, y, z);
			} catch (NumberFormatException e) {
				sender.sendMessage("§cCoordonnées invalides !");
				return true;
			}
		} else if (sender instanceof Player player) {
			spawnLoc = player.getLocation();
		} else {
			sender.sendMessage("§cVous devez spécifier les coordonnées depuis la console !");
			return true;
		}

		// Spawn le boss
		guardian.spawnBoss(spawnLoc, sender instanceof Player ? (Player) sender : null);
		return true;
	}
}

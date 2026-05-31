package fr.zyumie.GuardianOfNether.Commandes;

import fr.zyumie.GuardianOfNether.Manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /guardian-items [joueur]
 *
 * Donne le plastron du Gardien au joueur ciblé (ou à soi-même).
 */
public class ItemCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "guardianofnether.items";

    private final RewardManager rewardManager;

    public ItemCommand(RewardManager rewardManager) {
        this.rewardManager = rewardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage("§cVous n'avez pas la permission !");
            return true;
        }

        Player target;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJoueur introuvable ou hors ligne : §e" + args[0]);
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cDepuis la console, spécifiez un joueur : /guardian-items <joueur>");
            return true;
        }

        ItemStack chestplate = rewardManager.buildChestplate();
        target.getInventory().addItem(chestplate);
        target.sendMessage("§aVous avez reçu la §cChestplate of Guardian §a!");

        if (!sender.equals(target)) {
            sender.sendMessage("§aLa Chestplate of Guardian a été donnée à §e" + target.getName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
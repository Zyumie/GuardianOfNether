package fr.zyumie.GuardianOfNether.Commandes;

import fr.zyumie.GuardianOfNether.Main;
import fr.zyumie.GuardianOfNether.Boss.GuardianBoss;
import fr.zyumie.GuardianOfNether.Boss.MinionManager;
import fr.zyumie.GuardianOfNether.Config.ConfigManager;
import fr.zyumie.GuardianOfNether.Listener.BossListener;
import fr.zyumie.GuardianOfNether.Listener.NetherListener;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /guardian-of-nether <sous-commande>
 *
 * Sous-commandes :
 *   spawn [x y z]  — Spawne le boss à la position du joueur ou aux coordonnées
 *   reload         — Recharge le config.yml à chaud
 *   nether reset   — Reverrouille le Nether pour un nouveau cycle
 */
public class SpawnCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_SPAWN  = "guardianofnether.spawn";
    private static final String PERM_RELOAD = "guardianofnether.reload";
    private static final String PERM_NETHER = "guardianofnether.nether";

    private final Main          plugin;
    private final ConfigManager config;
    private final BossListener  bossListener;
    private final NetherListener netherListener;

    public SpawnCommand(Main plugin, ConfigManager config,
                        BossListener bossListener, NetherListener netherListener) {
        this.plugin         = plugin;
        this.config         = config;
        this.bossListener   = bossListener;
        this.netherListener = netherListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn"  -> handleSpawn(sender, args);
            case "reload" -> handleReload(sender);
            case "nether" -> handleNether(sender, args);
            default       -> { sendHelp(sender); yield true; }
        };
    }

    // ── Sous-commandes ───────────────────────────────────────────

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_SPAWN)) {
            sender.sendMessage("§cVous n'avez pas la permission !");
            return true;
        }

        // Un seul boss à la fois
        if (bossListener.getActiveBoss() != null && bossListener.getActiveBoss().isAlive()) {
            sender.sendMessage("§cUn Gardien du Nether est déjà actif !");
            return true;
        }

        Location spawnLoc;

        // Coordonnées fournies en argument
        if (args.length == 4) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cSpécifiez aussi le monde depuis la console !");
                return true;
            }
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                spawnLoc = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCoordonnées invalides ! Usage: /guardian-of-nether spawn [x y z]");
                return true;
            }
        } else if (sender instanceof Player player) {
            // Pas de coordonnées → position du joueur
            spawnLoc = player.getLocation();
        } else {
            sender.sendMessage("§cDepuis la console, spécifiez les coordonnées : /guardian-of-nether spawn <x> <y> <z>");
            return true;
        }

        // Création et spawn du boss
        MinionManager minionManager = new MinionManager(plugin, config);
        GuardianBoss boss = new GuardianBoss(plugin, config, minionManager);
        boss.spawn(spawnLoc);

        // On enregistre le boss actif dans le BossListener
        bossListener.setActiveBoss(boss);

        sender.sendMessage("§6Le Gardien du Nether a été invoqué !");
        plugin.getLogger().info("[GuardianOfNether] Boss spawné par " + sender.getName()
                + " en " + formatLoc(spawnLoc));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERM_RELOAD)) {
            sender.sendMessage("§cVous n'avez pas la permission !");
            return true;
        }
        config.reload();
        sender.sendMessage("§a[GuardianOfNether] Configuration rechargée !");
        plugin.getLogger().info("[GuardianOfNether] Config rechargée par " + sender.getName());
        return true;
    }

    private boolean handleNether(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_NETHER)) {
            sender.sendMessage("§cVous n'avez pas la permission !");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
            sender.sendMessage("§cUsage: /guardian-of-nether nether reset");
            return true;
        }
        netherListener.resetNether();
        sender.sendMessage("§6[GuardianOfNether] Le Nether a été reverrouillé.");
        return true;
    }

    // ── Tab completion ────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "reload", "nether");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nether")) {
            return List.of("reset");
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("spawn")) {
            return switch (args.length) {
                case 2 -> List.of("<x>");
                case 3 -> List.of("<y>");
                case 4 -> List.of("<z>");
                default -> List.of();
            };
        }
        return List.of();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Guardian of Nether ===");
        sender.sendMessage("§e/guardian-of-nether spawn §7[x y z] §f— Spawne le boss");
        sender.sendMessage("§e/guardian-of-nether reload §f— Recharge la config");
        sender.sendMessage("§e/guardian-of-nether nether reset §f— Reverrouille le Nether");
    }

    private String formatLoc(Location loc) {
        return String.format("%.1f, %.1f, %.1f (%s)",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld() != null ? loc.getWorld().getName() : "?");
    }
}
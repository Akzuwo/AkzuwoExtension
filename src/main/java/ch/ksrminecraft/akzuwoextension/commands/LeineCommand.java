package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class LeineCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;
    private static final UUID PRIVILEGED_UUID = UUID.fromString("ce49da3f-39fe-4de0-8c9f-dabf55dc61e3");

    public LeineCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Command nutzen.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("akzuwoextension.superadmin") && !player.getUniqueId().equals(PRIVILEGED_UUID)) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /leine <spieler> | /leine stop");
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            boolean stopped = plugin.getLeineManager().stopLeine(player);
            if (stopped) {
                sender.sendMessage(ChatColor.GREEN + "Deine Leine wurde gestoppt.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Du hast aktuell keine aktive Leine.");
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Spieler \"" + args[0] + "\" wurde nicht gefunden.");
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(ChatColor.RED + "Du kannst dich nicht selbst an die Leine nehmen.");
            return true;
        }

        plugin.getLeineManager().startLeine(player, target);
        sender.sendMessage(ChatColor.LIGHT_PURPLE + target.getName() + " wurde an dich angeleint. Nutze /leine stop zum Beenden.");
        target.sendMessage(ChatColor.LIGHT_PURPLE + "Du wurdest von " + player.getName() + " angeleint.");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return handle(sender, args);
    }
}

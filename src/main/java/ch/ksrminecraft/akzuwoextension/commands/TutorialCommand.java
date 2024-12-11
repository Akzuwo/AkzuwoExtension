package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;

    public TutorialCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfen, ob der Command von einem Spieler ausgeführt wird
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        // Überprüfen, ob tutorialcommand in der Config auf true gesetzt ist
        if (!plugin.getConfig().getBoolean("tutorialcommand", false)) {
            sender.sendMessage("Der Tutorial-Befehl ist momentan deaktiviert.");
            return true;
        }

        // Logik des Commands hier
        Player player = (Player) sender;
        player.sendMessage("Willkommen beim Tutorial!");
        sender.sendMessage(ChatColor.AQUA + "WorldEdit Mini-Guide:");
        sender.sendMessage(ChatColor.YELLOW + "//wand" + ChatColor.WHITE + " - Erhalte das WorldEdit-Werkzeug");
        sender.sendMessage(ChatColor.YELLOW + "//set <block>" + ChatColor.WHITE + " - Setze ausgewählten Bereich auf Blocktyp");
        sender.sendMessage(ChatColor.YELLOW + "//copy" + ChatColor.WHITE + " - Kopiere ausgewählten Bereich");
        sender.sendMessage(ChatColor.YELLOW + "//paste" + ChatColor.WHITE + " - Füge kopierten Bereich ein");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "PlotSquared Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/plot auto" + ChatColor.WHITE + " - Automatisch ein Grundstück erhalten");
        sender.sendMessage(ChatColor.YELLOW + "/plot claim" + ChatColor.WHITE + " - Grundstück beanspruchen");
        sender.sendMessage(ChatColor.YELLOW + "/plot delete" + ChatColor.WHITE + " - Grundstück löschen");
        sender.sendMessage(ChatColor.YELLOW + "/plot home" + ChatColor.WHITE + " - Zum eigenen Grundstück zurückkehren");
        return true;



    }
}

package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AkzuwoExtensionCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;

    public AkzuwoExtensionCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            Integer reportId = plugin.pollPendingDelete(sender.getName());
            if (reportId == null) {
                sender.sendMessage(ChatColor.RED + "Es gibt keinen Löschvorgang zu bestätigen.");
                return true;
            }

            Report report = plugin.getReportRepository().getReportById(reportId);
            if (report == null) {
                sender.sendMessage(ChatColor.RED + "Report mit der ID " + reportId + " wurde nicht gefunden.");
                return true;
            }

            plugin.getReportRepository().deleteReportById(reportId);
            sender.sendMessage(ChatColor.GREEN + "Report mit der ID " + reportId + " wurde erfolgreich gelöscht.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Verwendung: /akzuwoextension confirm");
        return true;
    }
}

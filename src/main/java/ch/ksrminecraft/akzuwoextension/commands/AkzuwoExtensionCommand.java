package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class AkzuwoExtensionCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;
    private final LeineCommand leineCommand;

    public AkzuwoExtensionCommand(AkzuwoExtension plugin, LeineCommand leineCommand) {
        this.plugin = plugin;
        this.leineCommand = leineCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("confirm")) {
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

            if (args[0].equalsIgnoreCase("remind")) {
                if (!sender.hasPermission("akzuwoextension.staff")) {
                    sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
                    return true;
                }

                if (plugin.triggerReportReminder()) {
                    sender.sendMessage(ChatColor.GREEN + "Report-Reminder wurde an Discord gesendet.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Report-Reminder ist aktuell nicht verfügbar.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("leine")) {
                return leineCommand.handle(sender, new String[0]);
            }
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("leine")) {
            return leineCommand.handle(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        sender.sendMessage(ChatColor.RED + "Verwendung: /akzuwoextension confirm | remind | leine <spieler|stop>");
        return true;
    }
}

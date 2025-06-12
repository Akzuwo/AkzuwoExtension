package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DeleteReportCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;
    private final ReportRepository reportRepository;

    public DeleteReportCommand(AkzuwoExtension plugin, ReportRepository reportRepository) {
        this.plugin = plugin;
        this.reportRepository = reportRepository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("akzuwoextension.staff")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /deletereport <Report-ID>");
            return false;
        }

        try {
            // Umwandeln der Report-ID von String zu Integer (nicht UUID, weil die ID ein Integer ist)
            int reportId = Integer.parseInt(args[0]);

            // Versuch, den Report aus der Datenbank zu holen
            Report report = reportRepository.getReportById(reportId);

            if (report == null) {
                sender.sendMessage(ChatColor.RED + "Report mit der ID " + reportId + " wurde nicht gefunden.");
                return true;
            }

            // Löschvorgang vormerken
            plugin.setPendingDelete(sender.getName(), reportId);
            sender.sendMessage(ChatColor.YELLOW + "Bestätige das Löschen mit /akzuwoextension confirm.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Die angegebene Report-ID ist ungültig.");
        }

        return true;
    }
}

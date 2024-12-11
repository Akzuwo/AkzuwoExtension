package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public class ViewReportsCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;

    public ViewReportsCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("akzuwoextension.staff")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        // Zugriff auf das ReportRepository
        ReportRepository reportRepository = plugin.getReportRepository();

        // Alle offenen Reports abrufen
        List<Report> reports = reportRepository.getAllReports();
        if (reports.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Keine Reports vorhanden.");
            return true;
        }

        // Reports anzeigen
        sender.sendMessage(ChatColor.AQUA + "Offene Reports:");
        for (Report report : reports) {
            String playerName = getPlayerNameFromUUID(report.getPlayerUUID());
            String reporterName = report.getReporterName(); // Annahme: Reporter ist im Report gespeichert
            String status = report.getStatus(); // Status des Reports
            String timestamp = report.getTimestamp().toString(); // Zeit des Reports

            sender.sendMessage(ChatColor.YELLOW + "ID: " + report.getId() +
                    ChatColor.GRAY + " | Spieler: " + playerName +
                    ChatColor.GRAY + " | Grund: " + report.getReason() +
                    ChatColor.GRAY + " | Gemeldet von: " + reporterName +
                    ChatColor.GRAY + " | Status: " + status +
                    ChatColor.GRAY + " | Zeit: " + timestamp);
        }

        return true;
    }

    /**
     * Hilfsmethode, um den Spielernamen anhand der UUID zu erhalten.
     *
     * @param uuid Die UUID des Spielers
     * @return Den Spielernamen oder "Unbekannt", falls Name nicht ermittelt werden kann
     */
    private String getPlayerNameFromUUID(String uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        return player.getName() != null ? player.getName() : "Unbekannt";
    }
}

package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteReportTabCompleter implements TabCompleter {

    private final ReportRepository reportRepository;

    /**
     * Konstruktor, der das ReportRepository akzeptiert.
     *
     * @param reportRepository Das Repository für die Verwaltung der Reports.
     */
    public DeleteReportTabCompleter(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Prüfe, ob es sich um das erste Argument handelt
        if (args.length == 1) {
            if (reportRepository == null) {
                // Logge einen schweren Fehler, falls das Repository nicht initialisiert wurde
                System.err.println("ReportRepository ist null. Tab-Vervollständigung kann nicht ausgeführt werden.");
                return Collections.emptyList(); // Gib eine leere Liste zurück
            }

            List<String> suggestions = new ArrayList<>();
            try {
                // Abrufen aller Reports aus der Datenbank
                List<Report> allReports = reportRepository.getAllReports();
                for (Report report : allReports) {
                    suggestions.add(String.valueOf(report.getId())); // Füge die Report-IDs hinzu
                }
            } catch (Exception e) {
                // Logge den Fehler
                System.err.println("Fehler beim Abrufen der Reports: " + e.getMessage());
                e.printStackTrace();
                return Collections.emptyList(); // Gib eine leere Liste zurück, um Fehler zu vermeiden
            }

            return suggestions; // Gib die gesammelten Vorschläge zurück
        }

        return Collections.emptyList(); // Gib eine leere Liste zurück, wenn das Argument nicht relevant ist
    }
}

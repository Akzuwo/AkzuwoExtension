package ch.ksrminecraft.akzuwoextension.utils;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ReportPlaceholder extends PlaceholderExpansion {

    private final AkzuwoExtension plugin;

    public ReportPlaceholder(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "akzuwoextension";
    }

    @Override
    public String getAuthor() {
        return "Akzuwo"; // Optional: Ändere zu deinem Namen
    }

    @Override
    public String getVersion() {
        return "1.6"; // Optional: Version anpassen
    }

    @Override
    public boolean persist() {
        return true; // Damit der Placeholder auch nach einem Reload funktioniert
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        // Überprüfen, welcher Placeholder angefordert wird
        if (identifier.equalsIgnoreCase("report_count")) {
            // Anzahl der Einträge in der Datenbank abrufen
            ReportRepository reportRepository = plugin.getReportRepository();
            if (reportRepository != null) {
                int reportCount = reportRepository.getReportCount(); // Neue Methode in ReportRepository
                return String.valueOf(reportCount);
            } else {
                return "Fehler"; // Wenn das Repository nicht verfügbar ist
            }
        }
        return null; // Placeholder ist nicht definiert
    }
}

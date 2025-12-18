package ch.ksrminecraft.akzuwoextension.utils;

import java.sql.Timestamp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Report {
    private final Integer id;                  // Eindeutige ID des Reports (Integer)
    private final String playerUUID;           // UUID des gemeldeten Spielers
    private final String reporterName;         // Name des Reporters
    private final String reason;               // Grund für den Report
    private final String status;               // Status des Reports (z. B. "open", "closed")
    private final String assignedStaff;        // Zuständiges Teammitglied
    private final String notes;                // Notizen zum Report
    private final Timestamp timestamp;         // Zeitstempel des Reports
    private final Timestamp lastUpdated;       // Letzte Aktualisierung

    // Vollständiger Konstruktor
    public Report(Integer id, String playerUUID, String reporterName, String reason, String status,
                  String assignedStaff, String notes, Timestamp timestamp, Timestamp lastUpdated) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.reporterName = reporterName;
        this.reason = reason;
        this.status = status;
        this.assignedStaff = assignedStaff;
        this.notes = notes;
        this.timestamp = timestamp;
        this.lastUpdated = lastUpdated;
    }

    // Getter-Methoden
    public Integer getId() {
        return id;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getAssignedStaff() {
        return assignedStaff;
    }

    public String getNotes() {
        return notes;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Gibt den Spielernamen des gemeldeten Spielers zurück.
     * Wenn der Spieler offline ist, wird die UUID zurückgegeben.
     */
    public String getPlayerName() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName(); // Wenn der Spieler online ist, gib den Namen zurück
        } else {
            return playerUUID; // Wenn der Spieler offline ist, gib die UUID zurück
        }
    }

    @Override
    public String toString() {
        return "Von: " + reporterName +
                " | Gegen: " + getPlayerName() + // Dynamisch den Spielernamen abrufen
                " | Grund: " + reason +
                " | Status: " + status +
                " | Zuständig: " + (assignedStaff == null ? "keiner" : assignedStaff) +
                " | Notiz: " + (notes == null || notes.isBlank() ? "-" : notes) +
                " | Zeit: " + timestamp;
    }
}

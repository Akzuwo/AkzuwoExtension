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
    private final Timestamp timestamp;         // Zeitstempel des Reports

    // Vollständiger Konstruktor
    public Report(Integer id, String playerUUID, String reporterName, String reason, String status, Timestamp timestamp) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.reporterName = reporterName;
        this.reason = reason;
        this.status = status;
        this.timestamp = timestamp;
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

    public Timestamp getTimestamp() {
        return timestamp;
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
                " | Zeit: " + timestamp;
    }
}

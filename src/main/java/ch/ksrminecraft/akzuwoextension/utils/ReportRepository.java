package ch.ksrminecraft.akzuwoextension.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ReportRepository  {

    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final JsonReportStorage jsonStorage;
    private boolean databaseAvailable;

    /**
     * Konstruktor der Klasse ReportRepository.
     *
     * @param databaseManager Die Datenbankverwaltungsklasse
     * @param logger          Der Logger für Fehlermeldungen und Debugging
     */
    public ReportRepository(DatabaseManager databaseManager, Logger logger, JsonReportStorage jsonStorage, boolean databaseAvailable) {
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.jsonStorage = jsonStorage;
        this.databaseAvailable = databaseAvailable;
    }

    /**
     * Fügt einen neuen Report in die Datenbank ein.
     *
     * @param playerUUID   UUID des gemeldeten Spielers
     * @param reporterName Name des Reporters
     * @param reason       Grund für den Report
     */
    public void addReport(String playerUUID, String reporterName, String reason) {
        if (databaseAvailable) {
            String sql = "INSERT INTO reports (player_uuid, reporter_name, reason, timestamp) VALUES (?, ?, ?, ?)";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerUUID);
                statement.setString(2, reporterName);
                statement.setString(3, reason);
                statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

                statement.executeUpdate();
                logger.info("Report erfolgreich hinzugefügt: " + reason);

            } catch (SQLException e) {
                logger.severe("Fehler beim Hinzufügen eines Reports: " + e.getMessage());
                databaseAvailable = false;
                jsonStorage.addReport(playerUUID, reporterName, reason);
            }
        } else {
            jsonStorage.addReport(playerUUID, reporterName, reason);
        }
    }

    /**
     * Ruft alle Reports aus der Datenbank ab.
     *
     * @return Eine Liste aller Reports
     */
    public List<Report> getAllReports() {
        if (databaseAvailable) {
            List<Report> reports = new ArrayList<>();
            String sql = "SELECT * FROM reports";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String playerUUID = resultSet.getString("player_uuid");
                    String reporterName = resultSet.getString("reporter_name");
                    String reason = resultSet.getString("reason");
                    String status = resultSet.getString("status");
                    Timestamp timestamp = resultSet.getTimestamp("timestamp");

                    reports.add(new Report(id, playerUUID, reporterName, reason, status, timestamp));
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen aller Reports: " + e.getMessage());
                databaseAvailable = false;
                return jsonStorage.getAllReports();
            }

            return reports;
        } else {
            return jsonStorage.getAllReports();
        }
    }

    /**
     * Ruft einen spezifischen Report basierend auf der ID ab.
     *
     * @param reportId Die ID des Reports
     * @return Der gefundene Report oder null, falls kein Eintrag existiert
     */
    public Report getReportById(int reportId) {
        if (databaseAvailable) {
            String sql = "SELECT * FROM reports WHERE id = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setInt(1, reportId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String playerUUID = resultSet.getString("player_uuid");
                        String reporterName = resultSet.getString("reporter_name");
                        String reason = resultSet.getString("reason");
                        String status = resultSet.getString("status");
                        Timestamp timestamp = resultSet.getTimestamp("timestamp");

                        return new Report(reportId, playerUUID, reporterName, reason, status, timestamp);
                    }
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen eines Reports mit ID " + reportId + ": " + e.getMessage());
                databaseAvailable = false;
                return jsonStorage.getReportById(reportId);
            }

            return null;
        } else {
            return jsonStorage.getReportById(reportId);
        }
    }

    /**
     * Löscht einen Report aus der Datenbank basierend auf der ID.
     *
     * @param reportId Die ID des zu löschenden Reports
     */
    public void deleteReportById(int reportId) {
        if (databaseAvailable) {
            String sql = "DELETE FROM reports WHERE id = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setInt(1, reportId);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    logger.info("Report mit ID " + reportId + " wurde gelöscht.");
                } else {
                    logger.warning("Kein Report mit der ID " + reportId + " gefunden.");
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Löschen eines Reports mit ID " + reportId + ": " + e.getMessage());
                databaseAvailable = false;
                jsonStorage.deleteReportById(reportId);
            }
        } else {
            jsonStorage.deleteReportById(reportId);
        }
    }

    /**
     * Aktualisiert den Status eines Reports.
     *
     * @param reportId Die ID des Reports
     * @param status   Neuer Status
     */
    public void updateReportStatus(int reportId, String status) {
        if (databaseAvailable) {
            String sql = "UPDATE reports SET status = ? WHERE id = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, status);
                statement.setInt(2, reportId);
                statement.executeUpdate();

            } catch (SQLException e) {
                logger.severe("Fehler beim Aktualisieren des Report-Status: " + e.getMessage());
                databaseAvailable = false;
                jsonStorage.updateReportStatus(reportId, status);
            }
        } else {
            jsonStorage.updateReportStatus(reportId, status);
        }
    }

    /**
     * Gibt die Anzahl aller gespeicherten Reports zurück.
     *
     * @return Anzahl der Reports
     */
    public int getReportCount() {
        if (databaseAvailable) {
            String sql = "SELECT COUNT(*) AS count FROM reports";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getInt("count");
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen der Anzahl der Reports: " + e.getMessage());
                databaseAvailable = false;
                return jsonStorage.getReportCount();
            }

            return 0; // 0, wenn kein Ergebnis
        } else {
            return jsonStorage.getReportCount();
        }
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
}

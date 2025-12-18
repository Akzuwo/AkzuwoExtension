package ch.ksrminecraft.akzuwoextension.utils;

import com.google.gson.*;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ReportRepository  {

    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final File jsonFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Konstruktor der Klasse ReportRepository.
     *
     * @param databaseManager Die Datenbankverwaltungsklasse
     * @param logger          Der Logger für Fehlermeldungen und Debugging
     * @param dataFolder      Plugin-Datenordner für Offline-Speicherung
     */
    public ReportRepository(DatabaseManager databaseManager, Logger logger, File dataFolder) {
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.jsonFile = new File(dataFolder, "reports.json");
        if (!jsonFile.exists()) {
            try {
                if (jsonFile.createNewFile()) {
                    try (Writer writer = new FileWriter(jsonFile)) {
                        writer.write("[]");
                    }
                }
            } catch (IOException e) {
                logger.severe("Fehler beim Erstellen der reports.json: " + e.getMessage());
            }
        }
    }

    /**
     * Synchronisiert eventuell offline gespeicherte Reports mit der Datenbank
     * und leert danach die JSON-Datei.
     */
    public void migrateOfflineReports() {
        if (databaseManager == null) {
            return;
        }
        JsonArray array = loadJson();
        if (array.size() == 0) {
            return;
        }
        String sql = "INSERT INTO reports (player_uuid, reporter_name, reason, status, assigned_staff, notes, timestamp, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                statement.setString(1, obj.get("player_uuid").getAsString());
                statement.setString(2, obj.get("reporter_name").getAsString());
                statement.setString(3, obj.get("reason").getAsString());
                String status = getAsString(obj, "status", "offen");
                statement.setString(4, status);
                setNullableString(statement, 5, getNullableString(obj, "assigned_staff"));
                statement.setString(6, getAsString(obj, "notes", ""));
                Timestamp created = new Timestamp(obj.get("timestamp").getAsLong());
                statement.setTimestamp(7, created);
                Timestamp lastUpdated = obj.has("last_updated")
                        ? new Timestamp(obj.get("last_updated").getAsLong())
                        : created;
                statement.setTimestamp(8, lastUpdated);
                statement.addBatch();
            }
            statement.executeBatch();
            logger.info("Offline gespeicherte Reports wurden in die Datenbank übertragen: " + array.size());
            saveJson(new JsonArray());
        } catch (SQLException e) {
            logger.severe("Fehler beim Übertragen offline gespeicherter Reports: " + e.getMessage());
        }
    }

    /**
     * Fügt einen neuen Report in die Datenbank ein.
     *
     * @param playerUUID   UUID des gemeldeten Spielers
     * @param reporterName Name des Reporters
     * @param reason       Grund für den Report
     */
    public void addReport(String playerUUID, String reporterName, String reason) {
        if (databaseManager != null) {
            String sql = "INSERT INTO reports (player_uuid, reporter_name, reason, status, assigned_staff, notes, timestamp, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerUUID);
                statement.setString(2, reporterName);
                statement.setString(3, reason);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                statement.setString(4, "offen");
                statement.setNull(5, Types.VARCHAR);
                statement.setString(6, "");
                statement.setTimestamp(7, now);
                statement.setTimestamp(8, now);

                statement.executeUpdate();
                logger.info("Report erfolgreich hinzugefügt: " + reason);

            } catch (SQLException e) {
                logger.severe("Fehler beim Hinzufügen eines Reports: " + e.getMessage());
            }
        } else {
            JsonArray array = loadJson();
            int newId = 1;
            for (JsonElement element : array) {
                int id = element.getAsJsonObject().get("id").getAsInt();
                if (id >= newId) newId = id + 1;
            }

            JsonObject obj = new JsonObject();
            obj.addProperty("id", newId);
            obj.addProperty("player_uuid", playerUUID);
            obj.addProperty("reporter_name", reporterName);
            obj.addProperty("reason", reason);
            obj.addProperty("status", "offen");
            long now = System.currentTimeMillis();
            obj.addProperty("timestamp", now);
            obj.add("assigned_staff", JsonNull.INSTANCE);
            obj.addProperty("notes", "");
            obj.addProperty("last_updated", now);

            array.add(obj);
            saveJson(array);
        }
    }

    /**
     * Ruft alle Reports aus der Datenbank ab.
     *
     * @return Eine Liste aller Reports
     */
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        if (databaseManager != null) {
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
                    String assignedStaff = resultSet.getString("assigned_staff");
                    String notes = resultSet.getString("notes");
                    Timestamp timestamp = resultSet.getTimestamp("timestamp");
                    Timestamp lastUpdated = resultSet.getTimestamp("last_updated");

                    reports.add(new Report(id, playerUUID, reporterName, reason, status,
                            assignedStaff, notes, timestamp, lastUpdated));
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen aller Reports: " + e.getMessage());
            }

            return reports;
        }

        JsonArray array = loadJson();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            reports.add(new Report(
                    obj.get("id").getAsInt(),
                    obj.get("player_uuid").getAsString(),
                    obj.get("reporter_name").getAsString(),
                    obj.get("reason").getAsString(),
                    getAsString(obj, "status", "offen"),
                    getNullableString(obj, "assigned_staff"),
                    getAsString(obj, "notes", ""),
                    new Timestamp(obj.get("timestamp").getAsLong()),
                    new Timestamp(obj.has("last_updated") ? obj.get("last_updated").getAsLong()
                            : obj.get("timestamp").getAsLong())
            ));
        }
        return reports;
    }

    /**
     * Ruft alle Reports für einen bestimmten Spieler ab.
     *
     * @param uuid UUID des gemeldeten Spielers
     * @return Eine Liste der passenden Reports
     */
    public List<Report> getReportsByPlayer(UUID uuid) {
        List<Report> reports = new ArrayList<>();
        if (uuid == null) {
            return reports;
        }

        if (databaseManager != null) {
            String sql = "SELECT * FROM reports WHERE player_uuid = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, uuid.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String playerUUID = resultSet.getString("player_uuid");
                        String reporterName = resultSet.getString("reporter_name");
                        String reason = resultSet.getString("reason");
                        String status = resultSet.getString("status");
                        String assignedStaff = resultSet.getString("assigned_staff");
                        String notes = resultSet.getString("notes");
                        Timestamp timestamp = resultSet.getTimestamp("timestamp");
                        Timestamp lastUpdated = resultSet.getTimestamp("last_updated");

                        reports.add(new Report(id, playerUUID, reporterName, reason, status,
                                assignedStaff, notes, timestamp, lastUpdated));
                    }
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen der Reports für Spieler " + uuid + ": " + e.getMessage());
            }

            return reports;
        }

        JsonArray array = loadJson();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (uuid.toString().equalsIgnoreCase(obj.get("player_uuid").getAsString())) {
                reports.add(new Report(
                        obj.get("id").getAsInt(),
                        obj.get("player_uuid").getAsString(),
                        obj.get("reporter_name").getAsString(),
                        obj.get("reason").getAsString(),
                        getAsString(obj, "status", "offen"),
                        getNullableString(obj, "assigned_staff"),
                        getAsString(obj, "notes", ""),
                        new Timestamp(obj.get("timestamp").getAsLong()),
                        new Timestamp(obj.has("last_updated") ? obj.get("last_updated").getAsLong()
                                : obj.get("timestamp").getAsLong())
                ));
            }
        }
        return reports;
    }

    /**
     * Ruft einen spezifischen Report basierend auf der ID ab.
     *
     * @param reportId Die ID des Reports
     * @return Der gefundene Report oder null, falls kein Eintrag existiert
     */
    public Report getReportById(int reportId) {
        if (databaseManager != null) {
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
                        String assignedStaff = resultSet.getString("assigned_staff");
                        String notes = resultSet.getString("notes");
                        Timestamp timestamp = resultSet.getTimestamp("timestamp");
                        Timestamp lastUpdated = resultSet.getTimestamp("last_updated");

                        return new Report(reportId, playerUUID, reporterName, reason, status,
                                assignedStaff, notes, timestamp, lastUpdated);
                    }
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen eines Reports mit ID " + reportId + ": " + e.getMessage());
            }

            return null;
        }

        JsonArray array = loadJson();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.get("id").getAsInt() == reportId) {
                return new Report(
                        reportId,
                        obj.get("player_uuid").getAsString(),
                        obj.get("reporter_name").getAsString(),
                        obj.get("reason").getAsString(),
                        getAsString(obj, "status", "offen"),
                        getNullableString(obj, "assigned_staff"),
                        getAsString(obj, "notes", ""),
                        new Timestamp(obj.get("timestamp").getAsLong()),
                        new Timestamp(obj.has("last_updated") ? obj.get("last_updated").getAsLong()
                                : obj.get("timestamp").getAsLong())
                );
            }
        }
        return null;
    }

    /**
     * Löscht einen Report aus der Datenbank basierend auf der ID.
     *
     * @param reportId Die ID des zu löschenden Reports
     */
    public void deleteReportById(int reportId) {
        if (databaseManager != null) {
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
            }
            return;
        }

        JsonArray array = loadJson();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if (obj.get("id").getAsInt() == reportId) {
                array.remove(i);
                saveJson(array);
                logger.info("Report mit ID " + reportId + " wurde gelöscht.");
                return;
            }
        }
        logger.warning("Kein Report mit der ID " + reportId + " gefunden.");
    }

    /**
     * Aktualisiert den Status eines Reports.
     *
     * @param reportId Die ID des Reports
     * @param status   Neuer Status
     */
    public void updateReportStatus(int reportId, String status) {
        if (databaseManager != null) {
            String sql = "UPDATE reports SET status = ?, last_updated = ? WHERE id = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, status);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                statement.setTimestamp(2, now);
                statement.setInt(3, reportId);
                statement.executeUpdate();

            } catch (SQLException e) {
                logger.severe("Fehler beim Aktualisieren des Report-Status: " + e.getMessage());
            }
            return;
        }

        JsonArray array = loadJson();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.get("id").getAsInt() == reportId) {
                obj.addProperty("status", status);
                obj.addProperty("last_updated", System.currentTimeMillis());
                saveJson(array);
                return;
            }
        }
    }

    public void updateReportAssignment(int reportId, String staffName) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (databaseManager != null) {
            String sql = "UPDATE reports SET assigned_staff = ?, last_updated = ? WHERE id = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                if (staffName == null || staffName.isBlank()) {
                    statement.setNull(1, Types.VARCHAR);
                } else {
                    statement.setString(1, staffName);
                }
                statement.setTimestamp(2, now);
                statement.setInt(3, reportId);
                statement.executeUpdate();
                return;

            } catch (SQLException e) {
                logger.severe("Fehler beim Aktualisieren der Zuständigkeit: " + e.getMessage());
            }
        }

        JsonArray array = loadJson();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.get("id").getAsInt() == reportId) {
                if (staffName == null || staffName.isBlank()) {
                    obj.add("assigned_staff", JsonNull.INSTANCE);
                } else {
                    obj.addProperty("assigned_staff", staffName);
                }
                obj.addProperty("last_updated", now.getTime());
                saveJson(array);
                return;
            }
        }
    }

    public void updateReportNotes(int reportId, String notes) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (databaseManager != null) {
            String sql = "UPDATE reports SET notes = ?, last_updated = ? WHERE id = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, notes == null ? "" : notes);
                statement.setTimestamp(2, now);
                statement.setInt(3, reportId);
                statement.executeUpdate();
                return;

            } catch (SQLException e) {
                logger.severe("Fehler beim Aktualisieren der Notizen: " + e.getMessage());
            }
        }

        JsonArray array = loadJson();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.get("id").getAsInt() == reportId) {
                obj.addProperty("notes", notes == null ? "" : notes);
                obj.addProperty("last_updated", now.getTime());
                saveJson(array);
                return;
            }
        }
    }

    /**
     * Gibt die Anzahl aller gespeicherten Reports zurück.
     *
     * @return Anzahl der Reports
     */
    public int getReportCount() {
        if (databaseManager != null) {
            String sql = "SELECT COUNT(*) AS count FROM reports";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getInt("count");
                }

            } catch (SQLException e) {
                logger.severe("Fehler beim Abrufen der Anzahl der Reports: " + e.getMessage());
            }

            return 0; // Rückgabe 0 bei Fehler
        }

        return loadJson().size();
    }

    private static String getAsString(JsonObject obj, String member, String def) {
        if (!obj.has(member) || obj.get(member).isJsonNull()) {
            return def;
        }
        return obj.get(member).getAsString();
    }

    private static String getNullableString(JsonObject obj, String member) {
        if (!obj.has(member) || obj.get(member).isJsonNull()) {
            return null;
        }
        return obj.get(member).getAsString();
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private JsonArray loadJson() {
        if (jsonFile == null) {
            return new JsonArray();
        }
        try (Reader reader = new FileReader(jsonFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray();
            }
        } catch (IOException e) {
            logger.severe("Fehler beim Lesen von reports.json: " + e.getMessage());
        }
        return new JsonArray();
    }

    private void saveJson(JsonArray array) {
        if (jsonFile == null) {
            return;
        }
        try (Writer writer = new FileWriter(jsonFile)) {
            gson.toJson(array, writer);
        } catch (IOException e) {
            logger.severe("Fehler beim Schreiben von reports.json: " + e.getMessage());
        }
    }
}

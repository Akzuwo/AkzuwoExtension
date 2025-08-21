package ch.ksrminecraft.akzuwoextension.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Verwaltet Reports in einer lokalen JSON-Datei als Fallback, falls keine
 * Datenbankverbindung besteht.
 */
public class JsonReportStorage {
    private final File file;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Report>>(){}.getType();

    public JsonReportStorage(File dataFolder) {
        this.file = new File(dataFolder, "reports.json");
    }

    private List<Report> load() {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(file)) {
            List<Report> reports = gson.fromJson(reader, listType);
            return reports != null ? reports : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void save(List<Report> reports) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(reports, writer);
        } catch (IOException ignored) {
        }
    }

    public void addReport(String playerUUID, String reporterName, String reason) {
        List<Report> reports = load();
        reports.add(new Report(null, playerUUID, reporterName, reason, "offen", new Timestamp(System.currentTimeMillis())));
        save(reports);
    }

    public List<Report> getAllReports() {
        return load();
    }

    public Report getReportById(int id) {
        for (Report r : load()) {
            if (r.getId() != null && r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    public void deleteReportById(int id) {
        List<Report> reports = load();
        Iterator<Report> iterator = reports.iterator();
        while (iterator.hasNext()) {
            Report r = iterator.next();
            if (r.getId() != null && r.getId() == id) {
                iterator.remove();
            }
        }
        save(reports);
    }

    public void updateReportStatus(int id, String status) {
        List<Report> reports = load();
        for (Report r : reports) {
            if (r.getId() != null && r.getId() == id) {
                reports.set(reports.indexOf(r), new Report(r.getId(), r.getPlayerUUID(), r.getReporterName(), r.getReason(), status, r.getTimestamp()));
                break;
            }
        }
        save(reports);
    }

    public int getReportCount() {
        return load().size();
    }

    public void clear() {
        if (file.exists()) {
            file.delete();
        }
    }
}

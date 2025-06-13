package ch.ksrminecraft.akzuwoextension.utils;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String VERSION_URL = "https://akzuwo.ch/plugins/akzuwoextension/version.txt";
    private final AkzuwoExtension plugin;

    public UpdateChecker(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(VERSION_URL).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    plugin.getLogger().warning("Konnte nicht auf Update-Server zugreifen.");
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String latest = reader.readLine();
                    if (latest == null) {
                        plugin.getLogger().warning("Ungültige Antwort vom Update-Server.");
                        return;
                    }

                    String current = plugin.getDescription().getVersion();
                    if (!current.equalsIgnoreCase(latest.trim())) {
                        plugin.getLogger().info("Eine neue Version (" + latest + ") ist verfügbar!");
                    } else {
                        plugin.getLogger().info("Plugin ist auf dem neuesten Stand.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Prüfen auf Updates: " + e.getMessage());
            }
        });
    }
}

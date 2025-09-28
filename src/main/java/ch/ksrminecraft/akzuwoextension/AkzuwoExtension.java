package ch.ksrminecraft.akzuwoextension;

import ch.ksrminecraft.akzuwoextension.commands.*;
import ch.ksrminecraft.akzuwoextension.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class AkzuwoExtension extends JavaPlugin {
    private PrefixedLogger logger;
    private DatabaseManager databaseManager;
    private ReportRepository reportRepository;
    private DiscordNotifier discordNotifier;
    private PointsAPI pointsAPI;
    private long rankPointsCheckInterval;
    private String serverName;
    private final java.util.Map<String, Integer> pendingDeleteReports = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        logger = new PrefixedLogger(super.getLogger());

        // Plugin-Ordner erstellen
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            logger.severe("Fehler beim Erstellen des Plugin-Ordners.");
            return;
        }
        saveDefaultConfig();

        serverName = getConfig().getString("default-server-name", "Unbekannt");
        if (serverName == null || serverName.isBlank()) {
            serverName = "Unbekannt";
        }

        // Discord Notifier initialisieren
        discordNotifier = new DiscordNotifier(this);
        if (!discordNotifier.initialize()) {
            discordNotifier = null;
        }

        // Datenbankkonfiguration aus config.yml laden
        boolean databaseEnabled = getConfig().getBoolean("database.enabled", false);
        if (!databaseEnabled) {
            logger.info("Datenbank in config.yml deaktiviert, Offline-Modus wird verwendet.");
            databaseManager = null;
            reportRepository = new ReportRepository(null, logger, getDataFolder());
        } else {
            String host = getConfig().getString("database.host");
            int port = getConfig().getInt("database.port");
            String database = getConfig().getString("database.name");
            String username = getConfig().getString("database.username");
            String password = getConfig().getString("database.password");

            // Datenbankverbindung und Repository initialisieren
            if (host == null || host.isBlank() || database == null || database.isBlank()
                    || username == null || username.isBlank() || password == null || password.isBlank()) {
                logger.warning("Ungültige Datenbankkonfiguration, Offline-Modus wird verwendet.");
                databaseManager = null;
                reportRepository = new ReportRepository(null, logger, getDataFolder());
            } else {
                databaseManager = new DatabaseManager(host, port, database, username, password);
                try {
                    databaseManager.connect();
                    reportRepository = new ReportRepository(databaseManager, logger, getDataFolder());
                    reportRepository.migrateOfflineReports();
                    logger.info("Datenbank erfolgreich verbunden.");
                } catch (SQLException e) {
                    logger.warning("Datenbankverbindung konnte nicht hergestellt werden, Offline-Modus wird verwendet.");
                    e.printStackTrace();
                    databaseManager = null;
                    reportRepository = new ReportRepository(null, logger, getDataFolder());
                }
            }
        }

        // RankPointsAPI initialisieren, falls in der Config aktiviert
        rankPointsCheckInterval = getConfig().getLong("rankpointsapi.check-interval", 10) * 1000L;
        if (getConfig().getBoolean("rankpointsapi.integration", true)) {
            String rpHost = getConfig().getString("rankpointsapi.host");
            int rpPort = getConfig().getInt("rankpointsapi.port");
            String rpDatabase = getConfig().getString("rankpointsapi.name");
            String rpUsername = getConfig().getString("rankpointsapi.username");
            String rpPassword = getConfig().getString("rankpointsapi.password");

            if (rpHost == null || rpHost.isBlank() || rpDatabase == null || rpDatabase.isBlank()
                    || rpUsername == null || rpUsername.isBlank() || rpPassword == null || rpPassword.isBlank()) {
                logger.warning("Ungültige RankPointsAPI-Datenbankkonfiguration, Integration deaktiviert.");
            } else {
                try {
                    String url = "jdbc:mysql://" + rpHost + ":" + rpPort + "/" + rpDatabase;
                    pointsAPI = new PointsAPI(url, rpUsername, rpPassword, logger, false);
                } catch (Exception e) {
                    logger.severe("RankPointsAPI konnte nicht initialisiert werden: " + e.getMessage());
                }
            }
        }

        // Placeholder laden
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ReportPlaceholder(this).register();
            if (pointsAPI != null) {
                new RankPointsPlaceholder(this).register();
            }
        }

        // Commands und TabCompleter registrieren
        registerCommands();

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new ViewReportsGuiListener(this), this);

        // Nach Updates suchen
        new UpdateChecker(this).checkForUpdates();
    }

    @Override
    public void onDisable() {
        // Datenbankverbindung schließen
        try {
            if (databaseManager != null) {
                databaseManager.disconnect();
                logger.info("Datenbankverbindung geschlossen.");
            }
        } catch (SQLException e) {
            logger.severe("Fehler beim Schließen der Datenbankverbindung: " + e.getMessage());
        }

        // Discord-Benachrichtigung senden und Bot herunterfahren
        if (discordNotifier != null) {
            String name = serverName != null
                    ? serverName
                    : getConfig().getString("default-server-name", "Unbekannt");
            discordNotifier.sendServerNotification(
                    "Plugin wurde auf Server " + name + " heruntergefahren");
            discordNotifier.shutdown();
        }

        // RankPointsAPI Verbindung schließen
        if (pointsAPI != null) {
            try {
                pointsAPI.close();
            } catch (SQLException e) {
                logger.severe("Fehler beim Schließen der RankPointsAPI-Verbindung: " + e.getMessage());
            }
        }

    }

    /**
     * Registriert alle Commands und zugehörigen TabCompleter.
     */
    private void registerCommands() {
        getCommand("report").setExecutor(new ReportCommand(this, discordNotifier));
        getCommand("report").setTabCompleter(new ReportTabCompleter());
        getCommand("viewreports").setExecutor(new ViewReportsCommand(this));
        getCommand("viewreportsgui").setExecutor(new ViewReportsGuiCommand(this));
        getCommand("deletereport").setExecutor(new DeleteReportCommand(this, reportRepository));
        getCommand("deletereport").setTabCompleter(new DeleteReportTabCompleter(reportRepository));
        getCommand("akzuwoextension").setExecutor(new AkzuwoExtensionCommand(this));
    }

    public ReportRepository getReportRepository() {
        return reportRepository;
    }

    public PrefixedLogger getPrefixedLogger() {
        return logger;
    }

    public String getServerName() {
        return serverName;
    }

    public void setPendingDelete(String sender, int id) {
        pendingDeleteReports.put(sender, id);
    }

    public Integer pollPendingDelete(String sender) {
        return pendingDeleteReports.remove(sender);
    }

    public PointsAPI getPointsAPI() {
        return pointsAPI;
    }

    public long getRankPointsCheckInterval() {
        return rankPointsCheckInterval;
    }
}

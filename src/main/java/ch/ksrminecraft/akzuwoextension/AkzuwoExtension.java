package ch.ksrminecraft.akzuwoextension;

import ch.ksrminecraft.akzuwoextension.commands.*;
import ch.ksrminecraft.akzuwoextension.services.ReportReminderService;
import ch.ksrminecraft.akzuwoextension.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class AkzuwoExtension extends JavaPlugin {
    private PrefixedLogger logger;
    private DatabaseManager databaseManager;
    private ReportRepository reportRepository;
    private DiscordNotifier discordNotifier;
    private PointsAPI pointsAPI;
    private long rankPointsCheckInterval;
    private String serverName;
    private final java.util.Map<String, Integer> pendingDeleteReports = new java.util.HashMap<>();
    private long reportCooldownSeconds;
    private int reportMaxReports;
    private ReportReminderService reportReminderService;
    private int reminderTaskId = -1;
    private LeineManager leineManager;

    @Override
    public void onEnable() {
        logger = new PrefixedLogger(super.getLogger());
        leineManager = new LeineManager(this);

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

        reportCooldownSeconds = Math.max(1L, getConfig().getLong("report.cooldown-seconds", 300L));
        reportMaxReports = Math.max(1, getConfig().getInt("report.max-reports-per-interval", 2));

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

        // Reminder Scheduler initialisieren
        setupReportReminderScheduler();

        // Nach Updates suchen
        new UpdateChecker(this).checkForUpdates();
    }

    @Override
    public void onDisable() {
        if (leineManager != null) {
            leineManager.cancelAll();
        }

        if (reminderTaskId != -1) {
            Bukkit.getScheduler().cancelTask(reminderTaskId);
            reminderTaskId = -1;
        }

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

        reportReminderService = null;

    }

    private void setupReportReminderScheduler() {
        if (discordNotifier == null) {
            logger.warning("Report-Reminder konnte nicht gestartet werden, Discord-Notifier ist nicht verfügbar.");
            return;
        }

        long longRunningHours = Math.max(1L, getConfig().getLong("reminders.long-running-threshold-hours", 48L));
        int escalateOpenCount = Math.max(0, getConfig().getInt("reminders.escalate-open-count", 10));
        reportReminderService = new ReportReminderService(this, reportRepository, discordNotifier, longRunningHours, escalateOpenCount);

        if (!getConfig().getBoolean("reminders.enabled", false)) {
            logger.info("Report-Reminder ist in der Konfiguration deaktiviert.");
            return;
        }

        long intervalMinutes = Math.max(1L, getConfig().getLong("reminders.interval-minutes", 60L));
        String sendTime = getConfig().getString("reminders.daily-send-time");

        long periodTicks = intervalMinutes * 60L * 20L;
        long initialDelayTicks = 20L; // Default 1 Sekunde

        if (sendTime != null && !sendTime.isBlank()) {
            try {
                LocalTime target = LocalTime.parse(sendTime.trim());
                ZoneId zoneId = ZoneId.systemDefault();
                ZonedDateTime now = ZonedDateTime.now(zoneId);
                ZonedDateTime nextRun = now.with(target);
                if (!now.toLocalTime().isBefore(target)) {
                    nextRun = nextRun.plusDays(1);
                }
                Duration untilNext = Duration.between(now, nextRun);
                long delaySeconds = Math.max(1L, untilNext.getSeconds());
                initialDelayTicks = delaySeconds * 20L;
                periodTicks = 20L * 60L * 60L * 24L; // Ein Tag
            } catch (DateTimeParseException ex) {
                logger.warning("Ungültiges Format für reminders.daily-send-time. Erwartet wird HH:mm. Es wird das Intervall verwendet.");
            }
        }

        if (periodTicks <= 0) {
            periodTicks = 20L * 60L; // Fallback auf eine Minute
        }

        reminderTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                reportReminderService.runReminder();
            } catch (Exception exception) {
                logger.severe("Fehler beim Ausführen des Report-Reminders: " + exception.getMessage());
            }
        }, initialDelayTicks, periodTicks).getTaskId();

        logger.info("Report-Reminder gestartet (Periode: " + (periodTicks / (20L * 60L)) + " Minuten).");
    }

    public boolean triggerReportReminder() {
        if (reportReminderService == null) {
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                reportReminderService.runReminder();
            } catch (Exception exception) {
                logger.severe("Fehler beim manuellen Ausführen des Report-Reminders: " + exception.getMessage());
            }
        });
        return true;
    }

    /**
     * Registriert alle Commands und zugehörigen TabCompleter.
     */
    private void registerCommands() {
        LeineCommand leineCommand = new LeineCommand(this);

        getCommand("report").setExecutor(new ReportCommand(this, discordNotifier, reportCooldownSeconds, reportMaxReports));
        getCommand("report").setTabCompleter(new ReportTabCompleter());
        getCommand("viewreports").setExecutor(new ViewReportsCommand(this));
        getCommand("viewreportsgui").setExecutor(new ViewReportsGuiCommand(this));
        getCommand("deletereport").setExecutor(new DeleteReportCommand(this, reportRepository));
        getCommand("deletereport").setTabCompleter(new DeleteReportTabCompleter(reportRepository));
        ViewPlayerReportsCommand viewPlayerReportsCommand = new ViewPlayerReportsCommand(this);
        getCommand("viewplayerreports").setExecutor(viewPlayerReportsCommand);
        getCommand("viewplayerreports").setTabCompleter(viewPlayerReportsCommand);
        getCommand("akzuwoextension").setExecutor(new AkzuwoExtensionCommand(this, leineCommand));
        getCommand("leine").setExecutor(leineCommand);
    }

    public ReportRepository getReportRepository() {
        return reportRepository;
    }

    public PrefixedLogger getPrefixedLogger() {
        return logger;
    }

    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
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

    public LeineManager getLeineManager() {
        return leineManager;
    }
}

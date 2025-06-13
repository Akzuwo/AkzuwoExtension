package ch.ksrminecraft.akzuwoextension;

import ch.ksrminecraft.akzuwoextension.commands.*;
import ch.ksrminecraft.akzuwoextension.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.sql.SQLException;
import java.nio.charset.StandardCharsets;

public class AkzuwoExtension extends JavaPlugin implements PluginMessageListener {

    private static final String CHANNEL = "akzuwo:servername";
    private DatabaseManager databaseManager;
    private ReportRepository reportRepository;
    private DiscordNotifier discordNotifier;
    private String serverName;
    private final java.util.Map<String, Integer> pendingDeleteReports = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        // Plugin-Ordner erstellen
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Fehler beim Erstellen des Plugin-Ordners.");
            return;
        }
        saveDefaultConfig();

        // Placeholder laden
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ReportPlaceholder(this).register();
        }

        // Discord Notifier initialisieren
        discordNotifier = new DiscordNotifier(this);
        if (!discordNotifier.initialize()) {
            discordNotifier = null;
        }

        // Datenbankkonfiguration aus config.yml laden
        String host = getConfig().getString("database.host");
        int port = getConfig().getInt("database.port");
        String database = getConfig().getString("database.name");
        String username = getConfig().getString("database.username");
        String password = getConfig().getString("database.password");

        // Prüfen, ob alle Daten vorhanden sind
        if (host == null || host.isBlank() || database == null || database.isBlank()
                || username == null || username.isBlank() || password == null || password.isBlank()) {
            getLogger().severe("Ungültige Datenbankkonfiguration. Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Datenbankverbindung und Repository initialisieren
        databaseManager = new DatabaseManager(host, port, database, username, password);
        try {
            databaseManager.connect();
            reportRepository = new ReportRepository(databaseManager, getLogger());
            getLogger().info("Datenbank erfolgreich verbunden.");
        } catch (SQLException e) {
            getLogger().severe("Datenbankverbindung konnte nicht hergestellt werden! Plugin wird deaktiviert.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registriere Plugin Messaging Channel
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);

        // Servernamen vom Proxy anfordern
        requestServerName();

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
                getLogger().info("Datenbankverbindung geschlossen.");
            }
        } catch (SQLException e) {
            getLogger().severe("Fehler beim Schließen der Datenbankverbindung: " + e.getMessage());
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

        // Deregistriere Plugin Messaging Channel
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        // Servername vom Proxy auslesen
        serverName = new String(message, StandardCharsets.UTF_8);
        getLogger().info("Servername vom Velocity-Proxy: " + serverName);

        // Discord-Benachrichtigung senden
        if (discordNotifier != null) {
            String version = getDescription().getVersion();
            discordNotifier.sendServerNotification("Plugin Version " + version + " erfolgreich gestartet auf Server: " + serverName);
        } else {
            getLogger().warning("DiscordNotifier ist nicht initialisiert.");
        }
    }

    private void requestServerName() {
        // Einen Online-Spieler suchen, um die Anfrage zu senden
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) {
            getLogger().warning("Kein Spieler online, Servername konnte nicht abgefragt werden.");
            return;
        }

        // Plugin-Message senden, um den Servernamen anzufordern
        player.sendPluginMessage(this, CHANNEL, new byte[0]);
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

    public String getServerName() {
        return serverName;
    }

    public void setPendingDelete(String sender, int id) {
        pendingDeleteReports.put(sender, id);
    }

    public Integer pollPendingDelete(String sender) {
        return pendingDeleteReports.remove(sender);
    }
}

//git fetch origin
//git reset --hard origin/main

package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.DiscordNotifier;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ReportCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;
    private final DiscordNotifier discordNotifier;
    private final Map<UUID, List<Long>> reportCooldown = new HashMap<>();
    private static final long COOLDOWN_PERIOD = 300_000; // 5 Minuten in Millisekunden
    private static final int MAX_REPORTS = 2;

    public ReportCommand(AkzuwoExtension plugin, DiscordNotifier discordNotifier) {
        this.plugin = plugin;
        this.discordNotifier = discordNotifier;

        @Nullable String serverName = plugin.getConfig().getString("default-server-name");
        if (discordNotifier != null) {
            String version = plugin.getDescription().getVersion();
            discordNotifier.sendServerNotification("Plugin Version " + version + " erfolgreich gestartet auf Server: " + serverName);
        } else {
            plugin.getPrefixedLogger().warning("DiscordNotifier ist nicht initialisiert.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfen, ob der Befehl korrekt verwendet wird
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /report <Spieler> <Grund>");
            return false;
        }

        String reportedPlayerName = args[0];
        OfflinePlayer reportedPlayer = Bukkit.getOfflinePlayer(reportedPlayerName);

        // Überprüfen, ob der Spieler jemals auf dem Server war
        if (!reportedPlayer.hasPlayedBefore() && !reportedPlayer.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Spieler '" + reportedPlayerName + "' wurde nie auf diesem Server gesehen.");
            return true;
        }

        // Überprüfen, ob der Zielspieler von Reports ausgeschlossen ist
        if (reportedPlayer.isOnline() && reportedPlayer.getPlayer().hasPermission("akzuwoextension.exempt")) {
            sender.sendMessage(ChatColor.RED + "Du kannst diesen Spieler nicht melden.");
            return true;
        }

        // Grund für den Report aus den Argumenten zusammensetzen
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // UUID und Name des Reporters abrufen
        String reporterName = sender.getName();

        if (sender instanceof Player) {
            Player reporter = (Player) sender;

            // Staff-Mitglieder sind von Einschränkungen befreit
            if (reporter.hasPermission("akzuwoextension.staff")) {
                return handleReport(sender, reportedPlayer, reporterName, reason);
            }

            // Überprüfen, ob der Spieler das Report-Limit erreicht hat
            UUID reporterUUID = reporter.getUniqueId();
            if (!canReport(reporterUUID)) {
                sender.sendMessage(ChatColor.RED + "Du kannst nur " + MAX_REPORTS + " Spieler innerhalb von 5 Minuten melden.");
                return true;
            }

            // Report-Timestamp registrieren
            registerReportTimestamp(reporterUUID);
        }

        return handleReport(sender, reportedPlayer, reporterName, reason);
    }


     // Überprüft, ob ein Spieler noch melden kann (basierend auf dem Cooldown).

    private boolean canReport(UUID reporterUUID) {
        List<Long> timestamps = reportCooldown.getOrDefault(reporterUUID, new ArrayList<>());
        long currentTime = System.currentTimeMillis();

        // Entferne abgelaufene Timestamps
        timestamps.removeIf(timestamp -> currentTime - timestamp > COOLDOWN_PERIOD);

        reportCooldown.put(reporterUUID, timestamps);
        return timestamps.size() < MAX_REPORTS;
    }


     //Registriert den aktuellen Zeitpunkt eines Reports für einen Spieler.

    private void registerReportTimestamp(UUID reporterUUID) {
        reportCooldown.computeIfAbsent(reporterUUID, k -> new ArrayList<>()).add(System.currentTimeMillis());
    }


     // Bearbeitet und speichert den Report.

    private boolean handleReport(CommandSender sender, OfflinePlayer reportedPlayer, String reporterName, String reason) {
        ReportRepository reportRepository = plugin.getReportRepository();
        if (reportRepository == null) {
            sender.sendMessage(ChatColor.RED + "Es gab ein Problem mit der Report-Datenbank. Bitte versuche es später erneut.");
            plugin.getPrefixedLogger().severe("ReportRepository ist null. Report konnte nicht gespeichert werden.");
            return true;
        }

        // Report speichern
        try {
            reportRepository.addReport(reportedPlayer.getUniqueId().toString(), reporterName, reason);
            sender.sendMessage(ChatColor.GREEN + "Report gegen " + ChatColor.YELLOW + reportedPlayer.getName() + ChatColor.GREEN + " wurde erfolgreich eingereicht.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Fehler beim Speichern des Reports. Bitte versuche es später erneut.");
            plugin.getPrefixedLogger().severe("Fehler beim Speichern des Reports: " + e.getMessage());
            return true;
        }

        // Nachricht an Staff und Discord senden
        notifyStaffAndDiscord(reportedPlayer, reporterName, reason);
        return true;
    }

    /**
     * Benachrichtigt alle Staff-Mitglieder und sendet eine Nachricht an Discord.
     */
    private void notifyStaffAndDiscord(OfflinePlayer reportedPlayer, String reporterName, String reason) {
        String message = ChatColor.RED + "[Report] " + ChatColor.YELLOW + reportedPlayer.getName() +
                ChatColor.GRAY + " wurde gemeldet von " + ChatColor.GREEN + reporterName +
                ChatColor.GRAY + " Grund: " + ChatColor.WHITE + reason;

        // Discord-Benachrichtigung
        String discordMessage = "**Ein Spieler wurde gemeldet!**\n" +
                "Melder: `" + reporterName + "`\n" +
                "Spieler: `" + reportedPlayer.getName() + "`\n" +
                "Grund: `" + reason + "`";
        if (discordNotifier != null) {
            discordNotifier.sendReportNotification(discordMessage);
        }

        // Nachricht an alle Staff-Mitglieder senden
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("akzuwoextension.staff")) {
                player.sendMessage(message);
            }
        }
    }
}

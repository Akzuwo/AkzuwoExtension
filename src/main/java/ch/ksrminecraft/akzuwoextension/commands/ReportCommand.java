package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.DiscordNotifier;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ReportCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;
    private final DiscordNotifier discordNotifier;
    private final Map<UUID, List<Long>> reportCooldown = new HashMap<>();
    private final long cooldownPeriodMillis;
    private final long cooldownPeriodSeconds;
    private final int maxReports;

    public ReportCommand(AkzuwoExtension plugin, DiscordNotifier discordNotifier, long cooldownSeconds, int maxReports) {
        this.plugin = plugin;
        this.discordNotifier = discordNotifier;
        this.cooldownPeriodSeconds = cooldownSeconds;
        this.cooldownPeriodMillis = cooldownSeconds * 1000L;
        this.maxReports = maxReports;

        String serverName = plugin.getServerName();
        if (discordNotifier != null) {
            String version = plugin.getDescription().getVersion();
            discordNotifier.sendServerNotification("Plugin Version " + version + " erfolgreich gestartet auf Server: " + serverName);
        } else {
            plugin.getPrefixedLogger().warning("DiscordNotifier ist nicht initialisiert.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (sender.hasPermission("akzuwoextension.staff")) {
                switch (subCommand) {
                    case "claim":
                        return handleClaim(sender, args, false);
                    case "unclaim":
                        return handleClaim(sender, args, true);
                    case "note":
                        return handleNote(sender, args);
                    default:
                        break;
                }
            }
        }

        // Überprüfen, ob der Befehl korrekt verwendet wird
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /report <Spieler> <Grund>");
            sender.sendMessage(ChatColor.RED + "oder /report claim|unclaim|note <ID> [Text]");
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
                sender.sendMessage(ChatColor.RED + "Du kannst nur " + maxReports + " Spieler innerhalb von "
                        + cooldownPeriodSeconds + " Sekunden melden.");
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
        timestamps.removeIf(timestamp -> currentTime - timestamp > cooldownPeriodMillis);

        reportCooldown.put(reporterUUID, timestamps);
        return timestamps.size() < maxReports;
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

    private boolean handleClaim(CommandSender sender, String[] args, boolean unclaim) {
        if (!sender.hasPermission("akzuwoextension.staff")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + (unclaim ? "Verwendung: /report unclaim <ID>"
                    : "Verwendung: /report claim <ID>"));
            return true;
        }

        int reportId;
        try {
            reportId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Die ID muss eine Zahl sein.");
            return true;
        }

        ReportRepository repository = plugin.getReportRepository();
        if (repository == null) {
            sender.sendMessage(ChatColor.RED + "Das Report-System ist derzeit nicht verfügbar.");
            return true;
        }

        Report report = repository.getReportById(reportId);
        if (report == null) {
            sender.sendMessage(ChatColor.RED + "Report mit ID " + reportId + " wurde nicht gefunden.");
            return true;
        }

        String staffName = sender.getName();
        String previousAssignee = report.getAssignedStaff();

        if (!unclaim && previousAssignee != null && !previousAssignee.isBlank()
                && !previousAssignee.equalsIgnoreCase(staffName)) {
            sender.sendMessage(ChatColor.YELLOW + "Dieser Report ist bereits " + previousAssignee + " zugewiesen.");
        }

        repository.updateReportAssignment(reportId, unclaim ? null : staffName);
        if (!unclaim && "offen".equalsIgnoreCase(report.getStatus())) {
            repository.updateReportStatus(reportId, "in Bearbeitung");
        }

        Report updated = repository.getReportById(reportId);
        String action = unclaim ? "freigegeben" : "übernommen";
        sender.sendMessage(ChatColor.GREEN + "Report " + reportId + " wurde " + action + ".");

        String staffMessage = ChatColor.GOLD + "[Reports] " + ChatColor.YELLOW + sender.getName() + ChatColor.GRAY
                + " hat Report " + ChatColor.WHITE + "#" + reportId + ChatColor.GRAY + " " + action + ".";
        broadcastToStaff(staffMessage, sender instanceof Player ? (Player) sender : null);

        String changeDescription = unclaim
                ? sender.getName() + " hat den Report freigegeben"
                : sender.getName() + " hat den Report übernommen";
        notifyDiscordReportUpdate("Report " + (unclaim ? "unassigned" : "claimed"), updated, changeDescription);
        return true;
    }

    private boolean handleNote(CommandSender sender, String[] args) {
        if (!sender.hasPermission("akzuwoextension.staff")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /report note <ID> <Text|clear>");
            return true;
        }

        int reportId;
        try {
            reportId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Die ID muss eine Zahl sein.");
            return true;
        }

        String noteText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (noteText.equalsIgnoreCase("clear")) {
            noteText = "";
        }

        ReportRepository repository = plugin.getReportRepository();
        if (repository == null) {
            sender.sendMessage(ChatColor.RED + "Das Report-System ist derzeit nicht verfügbar.");
            return true;
        }

        if (repository.getReportById(reportId) == null) {
            sender.sendMessage(ChatColor.RED + "Report mit ID " + reportId + " wurde nicht gefunden.");
            return true;
        }

        repository.updateReportNotes(reportId, noteText);
        Report updated = repository.getReportById(reportId);
        sender.sendMessage(ChatColor.GREEN + "Notiz für Report " + reportId + " aktualisiert.");

        String staffMessage = ChatColor.GOLD + "[Reports] " + ChatColor.YELLOW + sender.getName() + ChatColor.GRAY
                + " hat eine Notiz für Report " + ChatColor.WHITE + "#" + reportId + ChatColor.GRAY + " gesetzt.";
        broadcastToStaff(staffMessage, sender instanceof Player ? (Player) sender : null);

        notifyDiscordReportUpdate("Report note updated",
                updated,
                sender.getName() + (noteText.isBlank() ? " hat die Notiz entfernt" : " hat eine Notiz hinterlassen"));
        return true;
    }

    private void broadcastToStaff(String message, Player exclude) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(exclude)) {
                continue;
            }
            if (player.hasPermission("akzuwoextension.staff")) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Benachrichtigt alle Staff-Mitglieder und sendet eine Nachricht an Discord.
     */
    private void notifyStaffAndDiscord(OfflinePlayer reportedPlayer, String reporterName, String reason) {
        String message = ChatColor.RED + "[Report] " + ChatColor.YELLOW + reportedPlayer.getName() +
                ChatColor.GRAY + " wurde gemeldet von " + ChatColor.GREEN + reporterName +
                ChatColor.GRAY + " Grund: " + ChatColor.WHITE + reason +
                ChatColor.GRAY + " | Zuständig: " + ChatColor.RED + "(frei)" +
                ChatColor.GRAY + " | Notiz: " + ChatColor.DARK_GRAY + "-";

        // Discord-Benachrichtigung
        String discordMessage = "**Ein Spieler wurde gemeldet!**\n" +
                "Melder: `" + reporterName + "`\n" +
                "Spieler: `" + reportedPlayer.getName() + "`\n" +
                "Grund: `" + reason + "`\n" +
                "Zuständig: `nicht zugewiesen`\n" +
                "Notiz: `-`";
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

    private void notifyDiscordReportUpdate(String title, Report updated, String changeDescription) {
        if (discordNotifier == null || updated == null) {
            return;
        }

        String assigned = (updated.getAssignedStaff() == null || updated.getAssignedStaff().isBlank())
                ? "nicht zugewiesen"
                : updated.getAssignedStaff();
        String note = (updated.getNotes() == null || updated.getNotes().isBlank()) ? "-" : updated.getNotes();

        String message = "**" + title + "**\n" +
                "Report-ID: `" + updated.getId() + "`\n" +
                "Status: `" + (updated.getStatus() == null ? "unbekannt" : updated.getStatus()) + "`\n" +
                "Änderung: `" + changeDescription + "`\n" +
                "Zuständig: `" + assigned + "`\n" +
                "Notiz: `" + note + "`";

        discordNotifier.sendReportNotification(message);
    }
}

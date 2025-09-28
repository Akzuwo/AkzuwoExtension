package ch.ksrminecraft.akzuwoextension.services;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.DiscordNotifier;
import ch.ksrminecraft.akzuwoextension.utils.PrefixedLogger;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service-Klasse, die zyklisch Erinnerungen über offene Reports an Discord sendet.
 */
public class ReportReminderService {

    private static final int MAX_DETAILS = 5;
    private static final Comparator<Report> REPORT_TIMESTAMP_COMPARATOR =
            Comparator.comparing(Report::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()));

    private final AkzuwoExtension plugin;
    private final ReportRepository reportRepository;
    private final DiscordNotifier discordNotifier;
    private final PrefixedLogger logger;
    private final long longRunningThresholdHours;
    private final int escalateOpenCount;

    public ReportReminderService(AkzuwoExtension plugin,
                                 ReportRepository reportRepository,
                                 DiscordNotifier discordNotifier,
                                 long longRunningThresholdHours,
                                 int escalateOpenCount) {
        this.plugin = plugin;
        this.reportRepository = reportRepository;
        this.discordNotifier = discordNotifier;
        this.logger = plugin.getPrefixedLogger();
        this.longRunningThresholdHours = Math.max(1L, longRunningThresholdHours);
        this.escalateOpenCount = Math.max(0, escalateOpenCount);
    }

    /**
     * Führt den Reminderlauf aus und informiert den hinterlegten Discord-Channel.
     */
    public void runReminder() {
        if (discordNotifier == null) {
            logger.fine("Report-Reminder übersprungen, da kein Discord-Notifier initialisiert wurde.");
            return;
        }

        List<Report> reports = new ArrayList<>(reportRepository.getAllReports());
        Instant now = Instant.now();

        List<Report> openReports = reports.stream()
                .filter(report -> report.getStatus() != null && report.getStatus().equalsIgnoreCase("open"))
                .sorted(REPORT_TIMESTAMP_COMPARATOR)
                .collect(Collectors.toList());

        List<Report> longRunningReports = openReports.stream()
                .filter(report -> isLongRunning(report, now))
                .sorted(REPORT_TIMESTAMP_COMPARATOR)
                .collect(Collectors.toList());

        String message = buildMessage(openReports, longRunningReports, now);
        discordNotifier.sendReportNotification(message);
        logger.fine("Report-Reminder versendet (" + openReports.size() + " offene Reports).");
    }

    private boolean isLongRunning(Report report, Instant now) {
        Timestamp timestamp = report.getTimestamp();
        if (timestamp == null) {
            return false;
        }

        Instant created = timestamp.toInstant();
        if (created.isAfter(now)) {
            return false;
        }

        Duration duration = Duration.between(created, now);
        return duration.toHours() >= longRunningThresholdHours;
    }

    private String buildMessage(List<Report> openReports, List<Report> longRunningReports, Instant now) {
        long totalMinutes = 0;
        Duration longestAge = Duration.ZERO;

        for (Report report : openReports) {
            Timestamp timestamp = report.getTimestamp();
            if (timestamp == null) {
                continue;
            }
            Instant created = timestamp.toInstant();
            if (created.isAfter(now)) {
                continue;
            }
            Duration age = Duration.between(created, now);
            totalMinutes += Math.max(0, age.toMinutes());
            if (age.compareTo(longestAge) > 0) {
                longestAge = age;
            }
        }

        double averageHours = openReports.isEmpty()
                ? 0
                : (double) totalMinutes / openReports.size() / 60.0;

        StringBuilder builder = new StringBuilder();
        builder.append("**Report-Reminder - ")
                .append(plugin.getServerName())
                .append("**\n");
        builder.append("Offene Reports: ")
                .append(openReports.size())
                .append(" | Durchschnittliches Alter: ")
                .append(String.format(Locale.GERMAN, "%.1f", averageHours))
                .append("h\n");

        if (!longestAge.isZero()) {
            builder.append("Ältester Report: ")
                    .append(formatDuration(longestAge))
                    .append(" offen\n");
        }

        if (!longRunningReports.isEmpty()) {
            builder.append("\n**Langlaufende Reports (>")
                    .append(longRunningThresholdHours)
                    .append("h)**\n");

            int details = Math.min(MAX_DETAILS, longRunningReports.size());
            for (int i = 0; i < details; i++) {
                Report report = longRunningReports.get(i);
                Timestamp timestamp = report.getTimestamp();
                Duration age = Duration.ZERO;
                if (timestamp != null) {
                    Instant created = timestamp.toInstant();
                    if (!created.isAfter(now)) {
                        age = Duration.between(created, now);
                    }
                }

                builder.append("- #")
                        .append(report.getId())
                        .append(" | Spieler: ")
                        .append(report.getPlayerUUID() != null ? report.getPlayerUUID() : "unbekannt")
                        .append(" | Grund: ")
                        .append(report.getReason() != null ? report.getReason() : "-")
                        .append(" | Dauer: ")
                        .append(formatDuration(age))
                        .append("\n");
            }

            if (longRunningReports.size() > MAX_DETAILS) {
                builder.append("… und ")
                        .append(longRunningReports.size() - MAX_DETAILS)
                        .append(" weitere.\n");
            }
        }

        if (escalateOpenCount > 0 && openReports.size() >= escalateOpenCount) {
            builder.append("\n:warning: Eskalation: ")
                    .append(openReports.size())
                    .append(" offene Reports überschreiten den Schwellwert von ")
                    .append(escalateOpenCount)
                    .append(".\n");
        }

        return builder.toString();
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            builder.append(hours).append("h ");
        }
        builder.append(minutes).append("m");
        return builder.toString().trim();
    }
}

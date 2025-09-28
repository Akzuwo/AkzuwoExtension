package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ViewPlayerReportsCommand implements CommandExecutor, TabCompleter {

    private final AkzuwoExtension plugin;

    public ViewPlayerReportsCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("akzuwoextension.staff")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /" + label + " <Spieler>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Spieler \"" + args[0] + "\" wurde nicht gefunden.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "Spieler \"" + args[0] + "\" wurde nicht gefunden.");
            return true;
        }

        ReportRepository reportRepository = plugin.getReportRepository();
        List<Report> reports = reportRepository.getReportsByPlayer(targetUUID);
        reports.removeIf(r ->
                !("offen".equalsIgnoreCase(r.getStatus()) ||
                        "in Bearbeitung".equalsIgnoreCase(r.getStatus())));

        if (reports.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Keine Reports für " + getPlayerDisplayName(target) + " gefunden.");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "Reports für " + getPlayerDisplayName(target) + ":");
        for (Report report : reports) {
            String playerName = getPlayerNameFromUUID(report.getPlayerUUID());
            String reporterName = report.getReporterName();
            String status = report.getStatus();
            String timestamp = report.getTimestamp().toString();

            sender.sendMessage(ChatColor.YELLOW + "ID: " + report.getId() +
                    ChatColor.GRAY + " | Spieler: " + playerName +
                    ChatColor.GRAY + " | Grund: " + report.getReason() +
                    ChatColor.GRAY + " | Gemeldet von: " + reporterName +
                    ChatColor.GRAY + " | Status: " + status +
                    ChatColor.GRAY + " | Zeit: " + timestamp);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("akzuwoextension.staff")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName() != null ? player.getName() : "")
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], playerNames, completions);
            Collections.sort(completions);
            return completions;
        }

        return Collections.emptyList();
    }

    private String getPlayerNameFromUUID(String uuid) {
        OfflinePlayer player;
        try {
            player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return "Unbekannt";
        }
        return player.getName() != null ? player.getName() : "Unbekannt";
    }

    private String getPlayerDisplayName(OfflinePlayer player) {
        String name = player.getName();
        return name != null ? name : player.getUniqueId().toString();
    }
}

package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ReportTabCompleter implements TabCompleter {

    private static final List<String> STAFF_SUBCOMMANDS = Arrays.asList("claim", "unclaim", "note");

    private final AkzuwoExtension plugin;

    public ReportTabCompleter(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String lastArg = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("akzuwoextension.staff")) {
                suggestions.addAll(STAFF_SUBCOMMANDS);
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 2 && sender.hasPermission("akzuwoextension.staff")) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (STAFF_SUBCOMMANDS.contains(sub)) {
                ReportRepository repository = plugin.getReportRepository();
                if (repository != null) {
                    for (Report report : repository.getAllReports()) {
                        suggestions.add(String.valueOf(report.getId()));
                    }
                }
            }
        } else if (args.length == 3 && sender.hasPermission("akzuwoextension.staff")
                && args[0].equalsIgnoreCase("note")) {
            suggestions.add("clear");
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lastArg))
                .collect(Collectors.toList());
    }
}

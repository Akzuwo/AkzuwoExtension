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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ViewReportsGuiCommand implements CommandExecutor {

    private final AkzuwoExtension plugin;

    public ViewReportsGuiCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können dieses Kommando verwenden.");
            return true;
        }
        if (!sender.hasPermission("akzuwoextension.staff")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        ReportRepository repo = plugin.getReportRepository();
        List<Report> reports = repo.getAllReports();
        if (reports.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Keine Reports vorhanden.");
            return true;
        }

        reports.sort(Comparator.comparingInt(r -> getStatusOrder(r.getStatus())));
        int size = ((reports.size() - 1) / 9 + 1) * 9;
        size = Math.min(size, 54);

        Inventory inventory = Bukkit.createInventory(new ReportsHolder(), size, ChatColor.DARK_RED + "Reports");
        NamespacedKey key = new NamespacedKey(plugin, "reportId");

        for (int i = 0; i < Math.min(reports.size(), size); i++) {
            Report report = reports.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "ID " + report.getId() + " [" + report.getStatus() + "]");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Melder: " + report.getReporterName());
                lore.add(ChatColor.GRAY + "Gemeldete: " + getPlayerNameFromUUID(report.getPlayerUUID()));
                lore.add(ChatColor.GRAY + "Grund: " + report.getReason());
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, report.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        ((Player) sender).openInventory(inventory);
        return true;
    }

    private int getStatusOrder(String status) {
        if (status == null) return 3;
        if (status.equalsIgnoreCase("offen")) return 0;
        if (status.equalsIgnoreCase("in Bearbeitung")) return 1;
        if (status.equalsIgnoreCase("geschlossen")) return 2;
        return 3;
    }

    private String getPlayerNameFromUUID(String uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        return player.getName() != null ? player.getName() : "Unbekannt";
    }

    public static class ReportsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

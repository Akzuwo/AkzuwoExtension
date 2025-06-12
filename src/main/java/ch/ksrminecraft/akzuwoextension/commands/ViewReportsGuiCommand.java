package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ViewReportsGuiCommand implements CommandExecutor, Listener {

    private final AkzuwoExtension plugin;
    private final ReportRepository reportRepository;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final NamespacedKey idKey;

    public ViewReportsGuiCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.reportRepository = plugin.getReportRepository();
        this.idKey = new NamespacedKey(plugin, "report_id");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können dieses Kommando nutzen.");
            return true;
        }
        if (!player.hasPermission("akzuwoextension.staff")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }

        List<Report> reports = reportRepository.getAllReports();
        reports.sort(Comparator.comparingInt(r -> getStatusOrder(r.getStatus())));

        int size = Math.min(((reports.size() + 8) / 9) * 9, 54);
        Inventory inv = Bukkit.createInventory(player, Math.max(size, 9), "Reports");

        for (int i = 0; i < reports.size() && i < size; i++) {
            Report report = reports.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Report #" + report.getId());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Melder: " + report.getReporterName());
            lore.add(ChatColor.GRAY + "Gemeldeter: " + report.getPlayerName());
            lore.add(ChatColor.GRAY + "Grund: " + report.getReason());
            lore.add(ChatColor.GRAY + "Status: " + report.getStatus());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, report.getId());
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        return true;
    }

    private int getStatusOrder(String status) {
        if (status == null) return 3;
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "offen" -> 0;
            case "in bearbeitung" -> 1;
            case "geschlossen" -> 2;
            default -> 3;
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null || !event.getInventory().equals(inv)) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.PAPER || !event.isRightClick()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        Integer id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.INTEGER);
        if (id == null) return;

        Report report = reportRepository.getReportById(id);
        if (report == null) return;

        String newStatus = null;
        String status = report.getStatus();
        if ("offen".equalsIgnoreCase(status)) {
            newStatus = "in Bearbeitung";
        } else if ("in Bearbeitung".equalsIgnoreCase(status)) {
            newStatus = "geschlossen";
        }

        if (newStatus != null) {
            reportRepository.updateReportStatus(id, newStatus);
            List<String> lore = meta.getLore();
            if (lore != null && lore.size() >= 4) {
                lore.set(3, ChatColor.GRAY + "Status: " + newStatus);
                meta.setLore(lore);
                item.setItemMeta(meta);
                player.sendMessage(ChatColor.GREEN + "Report #" + id + " ist nun '" + newStatus + "'.");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }
}

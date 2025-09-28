package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ViewReportsGuiCommand implements CommandExecutor {

    public static final String REPORT_ID_KEY = "reportId";
    public static final String CONTROL_KEY = "reportsControl";
    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    private final AkzuwoExtension plugin;
    private final NamespacedKey reportIdKey;
    private final NamespacedKey controlKey;

    public ViewReportsGuiCommand(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.reportIdKey = new NamespacedKey(plugin, REPORT_ID_KEY);
        this.controlKey = new NamespacedKey(plugin, CONTROL_KEY);
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

        Player player = (Player) sender;
        ReportsHolder holder = new ReportsHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, ChatColor.DARK_RED + "Reports");
        populateInventory(inventory, holder);
        player.openInventory(inventory);
        if (holder.getTotalReports() == 0) {
            player.sendMessage(ChatColor.YELLOW + "Keine Reports vorhanden.");
        }
        sendStatusMessage(player, holder);
        return true;
    }

    void populateInventory(Inventory inventory, ReportsHolder holder) {
        inventory.clear();

        List<Report> reports = getFilteredReports(holder.isOnlyOpen());
        holder.setTotalReports(reports.size());

        int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / ITEMS_PER_PAGE));
        holder.setTotalPages(totalPages);

        if (holder.getPage() >= totalPages) {
            holder.setPage(totalPages - 1);
        }

        int startIndex = holder.getPage() * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, reports.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
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
                meta.getPersistentDataContainer().set(reportIdKey, PersistentDataType.INTEGER, report.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        for (; slot < ITEMS_PER_PAGE; slot++) {
            inventory.setItem(slot, null);
        }

        addNavigationItems(inventory, holder);
    }

    void refreshInventory(Player player, ReportsHolder holder, Inventory inventory) {
        populateInventory(inventory, holder);
        sendStatusMessage(player, holder);
    }

    private void addNavigationItems(Inventory inventory, ReportsHolder holder) {
        int baseSlot = INVENTORY_SIZE - 9;
        inventory.setItem(baseSlot, createNavigationItem("previous", holder.getPage() > 0, holder));
        inventory.setItem(baseSlot + 8, createNavigationItem("next", holder.getPage() < holder.getTotalPages() - 1, holder));
        inventory.setItem(baseSlot + 4, createPageIndicator(holder));
        inventory.setItem(baseSlot + 5, createFilterItem(holder));
    }

    private ItemStack createNavigationItem(String type, boolean enabled, ReportsHolder holder) {
        Material material = enabled ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (enabled) {
                String displayName = type.equals("previous")
                        ? ChatColor.GREEN + "Vorherige Seite"
                        : ChatColor.GREEN + "Nächste Seite";
                meta.setDisplayName(displayName);
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Aktuelle Seite: "
                        + (holder.getPage() + 1) + "/" + holder.getTotalPages()));
                meta.getPersistentDataContainer().set(controlKey, PersistentDataType.STRING, type);
            } else {
                String displayName = type.equals("previous")
                        ? ChatColor.DARK_GRAY + "Keine vorherige Seite"
                        : ChatColor.DARK_GRAY + "Keine nächste Seite";
                meta.setDisplayName(displayName);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPageIndicator(ReportsHolder holder) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Seite " + (holder.getPage() + 1) + " / " + holder.getTotalPages());
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Gesamt: " + holder.getTotalReports() + " Reports"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFilterItem(ReportsHolder holder) {
        boolean onlyOpen = holder.isOnlyOpen();
        Material material = onlyOpen ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter: " + (onlyOpen ? "Nur offene" : "Alle"));
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Klicke zum Umschalten"));
            meta.getPersistentDataContainer().set(controlKey, PersistentDataType.STRING, "filter");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void sendStatusMessage(Player player, ReportsHolder holder) {
        String filterMessage = holder.isOnlyOpen() ? ChatColor.GREEN + "Nur offene" : ChatColor.YELLOW + "Alle";
        player.sendMessage(ChatColor.GOLD + "Seite " + (holder.getPage() + 1) + "/" + holder.getTotalPages()
                + ChatColor.GRAY + " | Filter: " + filterMessage);
    }

    private List<Report> getFilteredReports(boolean onlyOpen) {
        ReportRepository repo = plugin.getReportRepository();
        List<Report> allReports = repo.getAllReports();
        List<Report> filteredReports = new ArrayList<>();
        for (Report report : allReports) {
            if (!onlyOpen || "offen".equalsIgnoreCase(report.getStatus())) {
                filteredReports.add(report);
            }
        }
        filteredReports.sort(Comparator
                .comparingInt((Report r) -> getStatusOrder(r.getStatus()))
                .thenComparingInt(Report::getId));
        return filteredReports;
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

        private final ViewReportsGuiCommand command;
        private int page;
        private int totalPages = 1;
        private int totalReports;
        private boolean onlyOpen;

        public ReportsHolder(ViewReportsGuiCommand command) {
            this.command = command;
        }

        public ViewReportsGuiCommand getCommand() {
            return command;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = Math.max(0, page);
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = Math.max(1, totalPages);
        }

        public int getTotalReports() {
            return totalReports;
        }

        public void setTotalReports(int totalReports) {
            this.totalReports = Math.max(0, totalReports);
        }

        public boolean isOnlyOpen() {
            return onlyOpen;
        }

        public void setOnlyOpen(boolean onlyOpen) {
            this.onlyOpen = onlyOpen;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

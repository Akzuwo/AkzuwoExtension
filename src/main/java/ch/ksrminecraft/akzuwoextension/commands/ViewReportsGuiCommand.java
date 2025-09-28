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

        ReportsHolder holder = new ReportsHolder(plugin);
        Inventory inventory = holder.getInventory();
        Player player = (Player) sender;
        player.openInventory(inventory);

        if (holder.getFilteredReportCount() == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Keine Reports vorhanden.");
        }

        sender.sendMessage(ChatColor.GREEN + "Seite " + (holder.getPage() + 1) + "/" + holder.getTotalPages() +
                " | Filter: " + holder.getFilterDisplayName());
        return true;
    }

    private static int getStatusOrder(String status) {
        if (status == null) return 3;
        if (status.equalsIgnoreCase("offen")) return 0;
        if (status.equalsIgnoreCase("in Bearbeitung")) return 1;
        if (status.equalsIgnoreCase("geschlossen")) return 2;
        return 3;
    }

    private static String getPlayerNameFromUUID(String uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        return player.getName() != null ? player.getName() : "Unbekannt";
    }

    public static class ReportsHolder implements InventoryHolder {

        private static final int INVENTORY_SIZE = 54;
        private static final int REPORTS_PER_PAGE = 45;
        public static final String NAVIGATION_PREVIOUS = "previous";
        public static final String NAVIGATION_NEXT = "next";

        private final AkzuwoExtension plugin;
        private final Inventory inventory;
        private final NamespacedKey reportIdKey;
        private final NamespacedKey navigationKey;
        private final NamespacedKey filterKey;

        private int page = 0;
        private int totalPages = 1;
        private int filteredReportCount = 0;
        private StatusFilter filter = StatusFilter.ALL;

        public ReportsHolder(AkzuwoExtension plugin) {
            this.plugin = plugin;
            this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, ChatColor.DARK_RED + "Reports");
            this.reportIdKey = new NamespacedKey(plugin, "reportId");
            this.navigationKey = new NamespacedKey(plugin, "reportsNavigation");
            this.filterKey = new NamespacedKey(plugin, "reportsFilter");
            refreshInventory();
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void nextPage() {
            if (page + 1 < totalPages) {
                page++;
            }
        }

        public void previousPage() {
            if (page > 0) {
                page--;
            }
        }

        public void toggleFilter() {
            filter = filter.next();
            page = 0;
        }

        public int getPage() {
            return page;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public String getFilterDisplayName() {
            return filter.getDisplayName();
        }

        public int getFilteredReportCount() {
            return filteredReportCount;
        }

        public NamespacedKey getReportIdKey() {
            return reportIdKey;
        }

        public NamespacedKey getNavigationKey() {
            return navigationKey;
        }

        public NamespacedKey getFilterKey() {
            return filterKey;
        }

        public void refreshInventory() {
            inventory.clear();

            ReportRepository repo = plugin.getReportRepository();
            List<Report> reports = repo.getAllReports();
            reports.sort(Comparator.comparingInt(r -> ViewReportsGuiCommand.getStatusOrder(r.getStatus())));

            List<Report> filtered = new ArrayList<>();
            for (Report report : reports) {
                if (filter.matches(report)) {
                    filtered.add(report);
                }
            }

            filteredReportCount = filtered.size();
            totalPages = Math.max(1, (int) Math.ceil(filteredReportCount / (double) REPORTS_PER_PAGE));
            if (page >= totalPages) {
                page = totalPages - 1;
            }

            int startIndex = page * REPORTS_PER_PAGE;
            int endIndex = Math.min(startIndex + REPORTS_PER_PAGE, filteredReportCount);

            for (int i = startIndex, slot = 0; i < endIndex; i++, slot++) {
                Report report = filtered.get(i);
                inventory.setItem(slot, createReportItem(report));
            }

            addNavigationControls();
        }

        private ItemStack createReportItem(Report report) {
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
            return item;
        }

        private void addNavigationControls() {
            ItemStack filler = createFiller();
            for (int slot = REPORTS_PER_PAGE; slot < INVENTORY_SIZE; slot++) {
                inventory.setItem(slot, filler.clone());
            }

            if (page > 0) {
                inventory.setItem(45, createNavigationItem(Material.ARROW, ChatColor.GOLD + "Vorherige Seite", NAVIGATION_PREVIOUS));
            } else {
                inventory.setItem(45, createDisabledNavigationItem(ChatColor.DARK_GRAY + "Keine vorherige Seite"));
            }

            if (page + 1 < totalPages) {
                inventory.setItem(53, createNavigationItem(Material.ARROW, ChatColor.GOLD + "Nächste Seite", NAVIGATION_NEXT));
            } else {
                inventory.setItem(53, createDisabledNavigationItem(ChatColor.DARK_GRAY + "Keine weitere Seite"));
            }

            inventory.setItem(49, createFilterItem());
            inventory.setItem(51, createPageIndicator());
        }

        private ItemStack createNavigationItem(Material material, String name, String navigationValue) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Seite " + (page + 1) + "/" + totalPages);
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(navigationKey, PersistentDataType.STRING, navigationValue);
                item.setItemMeta(meta);
            }
            return item;
        }

        private ItemStack createDisabledNavigationItem(String name) {
            ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                item.setItemMeta(meta);
            }
            return item;
        }

        private ItemStack createFilterItem() {
            ItemStack item = new ItemStack(Material.COMPASS);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "Filter: " + filter.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Linksklick zum Umschalten");
                lore.add(ChatColor.YELLOW + "Aktiv: " + ChatColor.WHITE + filter.getDisplayName());
                lore.add(ChatColor.GRAY + "Nächster Filter: " + filter.next().getDisplayName());
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(filterKey, PersistentDataType.STRING, "toggle");
                item.setItemMeta(meta);
            }
            return item;
        }

        private ItemStack createPageIndicator() {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "Seite " + (page + 1) + " von " + totalPages);
                int visibleOnPage = Math.max(0, Math.min(filteredReportCount - page * REPORTS_PER_PAGE, REPORTS_PER_PAGE));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Angezeigte Reports: " + visibleOnPage);
                lore.add(ChatColor.GRAY + "Gesamt: " + filteredReportCount);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        private ItemStack createFiller() {
            ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                item.setItemMeta(meta);
            }
            return item;
        }

        private enum StatusFilter {
            ALL("Alle"),
            OPEN_ONLY("Nur offen"),
            IN_PROGRESS_ONLY("Nur in Bearbeitung"),
            CLOSED_ONLY("Nur geschlossen");

            private final String displayName;

            StatusFilter(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }

            public StatusFilter next() {
                StatusFilter[] values = StatusFilter.values();
                return values[(ordinal() + 1) % values.length];
            }

            public boolean matches(Report report) {
                if (this == ALL) {
                    return true;
                }

                String status = report.getStatus();
                if (status == null) {
                    return false;
                }

                switch (this) {
                    case OPEN_ONLY:
                        return status.equalsIgnoreCase("offen");
                    case IN_PROGRESS_ONLY:
                        return status.equalsIgnoreCase("in Bearbeitung");
                    case CLOSED_ONLY:
                        return status.equalsIgnoreCase("geschlossen");
                    default:
                        return false;
                }
            }
        }
    }
}

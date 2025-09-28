package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.DiscordNotifier;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ViewReportsGuiListener implements Listener {

    private final AkzuwoExtension plugin;
    private final DiscordNotifier discordNotifier;

    public ViewReportsGuiListener(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.discordNotifier = plugin.getDiscordNotifier();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ViewReportsGuiCommand.ReportsHolder)) {
            return;
        }
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        ViewReportsGuiCommand.ReportsHolder holder = (ViewReportsGuiCommand.ReportsHolder) event.getInventory().getHolder();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey navigationKey = holder.getNavigationKey();
        NamespacedKey filterKey = holder.getFilterKey();
        NamespacedKey reportKey = holder.getReportIdKey();
        Player player = (Player) event.getWhoClicked();

        if (container.has(navigationKey, PersistentDataType.STRING)) {
            String direction = container.get(navigationKey, PersistentDataType.STRING);
            if (ViewReportsGuiCommand.ReportsHolder.NAVIGATION_PREVIOUS.equals(direction)) {
                holder.previousPage();
            } else if (ViewReportsGuiCommand.ReportsHolder.NAVIGATION_NEXT.equals(direction)) {
                holder.nextPage();
            }
            holder.refreshInventory();
            sendPageFeedback(player, holder);
            return;
        }

        if (container.has(filterKey, PersistentDataType.STRING)) {
            holder.toggleFilter();
            holder.refreshInventory();
            sendPageFeedback(player, holder);
            return;
        }

        Integer id = container.get(reportKey, PersistentDataType.INTEGER);
        if (id == null) {
            return;
        }

        boolean rightClick = event.getClick().isRightClick();
        boolean leftClick = event.getClick().isLeftClick();
        if (!rightClick && !leftClick) {
            return;
        }

        ReportRepository repo = plugin.getReportRepository();
        Report report = repo.getReportById(id);
        if (report == null) return;

        String status = report.getStatus();
        String newStatus = null;
        if (rightClick) {
            if ("offen".equalsIgnoreCase(status)) {
                newStatus = "in Bearbeitung";
            } else if ("in Bearbeitung".equalsIgnoreCase(status)) {
                newStatus = "geschlossen";
            }
        } else if (leftClick) {
            if ("geschlossen".equalsIgnoreCase(status)) {
                newStatus = "in Bearbeitung";
            } else if ("in Bearbeitung".equalsIgnoreCase(status)) {
                newStatus = "offen";
            }
        }

        if (newStatus != null) {
            repo.updateReportStatus(id, newStatus);
            meta.setDisplayName(ChatColor.YELLOW + "ID " + id + " [" + newStatus + "]");
            clickedItem.setItemMeta(meta);

            player.sendMessage(ChatColor.GREEN + "Report " + id + " ist nun " + newStatus + ".");

            holder.refreshInventory();
            sendPageFeedback(player, holder);
            notifyDiscordStatusChange(player, id, status, newStatus);
        }
    }

    private void sendPageFeedback(Player player, ViewReportsGuiCommand.ReportsHolder holder) {
        player.sendMessage(ChatColor.GOLD + "Seite " + (holder.getPage() + 1) + "/" + holder.getTotalPages() +
                ChatColor.GRAY + " | Filter: " + ChatColor.AQUA + holder.getFilterDisplayName());
    }

    private void notifyDiscordStatusChange(Player staffMember, int reportId, String oldStatus, String newStatus) {
        if (discordNotifier == null) {
            plugin.getPrefixedLogger().warning("DiscordNotifier ist nicht konfiguriert. Statusänderung von Report " + reportId + " wurde nicht an Discord gesendet.");
            return;
        }

        String previousStatus = (oldStatus == null || oldStatus.isBlank()) ? "unbekannt" : oldStatus;
        String updatedStatus = (newStatus == null || newStatus.isBlank()) ? "unbekannt" : newStatus;

        String message = "**Report-Status aktualisiert**\n" +
                "Report-ID: `" + reportId + "`\n" +
                "Status: `" + previousStatus + "` → `" + updatedStatus + "`\n" +
                "Bearbeitet von: `" + staffMember.getName() + "`";

        discordNotifier.sendReportNotification(message);
    }
}

package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.DiscordNotifier;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViewReportsGuiListener implements Listener {

    private static final String ACTION_CLAIM = "claim";
    private static final String ACTION_UNCLAIM = "unclaim";
    private static final String ACTION_NOTE = "note";

    private final AkzuwoExtension plugin;
    private final DiscordNotifier discordNotifier;
    private final Map<UUID, ViewReportsGuiCommand.ReportsHolder> activeHolders = new HashMap<>();
    private final Map<UUID, PendingNote> pendingNoteEdits = new HashMap<>();

    public ViewReportsGuiListener(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.discordNotifier = plugin.getDiscordNotifier();
    }

    private static class PendingNote {
        private final ViewReportsGuiCommand.ReportsHolder holder;
        private final int reportId;

        private PendingNote(ViewReportsGuiCommand.ReportsHolder holder, int reportId) {
            this.holder = holder;
            this.reportId = reportId;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ViewReportsGuiCommand.ReportsHolder)
                && !(event.getInventory().getHolder() instanceof ViewReportsGuiCommand.ReportDetailHolder)) {
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

        Player player = (Player) event.getWhoClicked();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (event.getInventory().getHolder() instanceof ViewReportsGuiCommand.ReportDetailHolder) {
            ViewReportsGuiCommand.ReportDetailHolder detailHolder =
                    (ViewReportsGuiCommand.ReportDetailHolder) event.getInventory().getHolder();
            handleDetailClick(player, detailHolder, container);
            return;
        }

        ViewReportsGuiCommand.ReportsHolder holder = (ViewReportsGuiCommand.ReportsHolder) event.getInventory().getHolder();
        activeHolders.put(player.getUniqueId(), holder);
        NamespacedKey navigationKey = holder.getNavigationKey();
        NamespacedKey filterKey = holder.getFilterKey();
        NamespacedKey reportKey = holder.getReportIdKey();
        NamespacedKey actionKey = holder.getActionKey();

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

        if (container.has(actionKey, PersistentDataType.STRING)) {
            String action = container.get(actionKey, PersistentDataType.STRING);
            handleActionClick(player, holder, action);
            return;
        }

        Integer id = container.get(reportKey, PersistentDataType.INTEGER);
        if (id == null) {
            return;
        }

        holder.setSelectedReportId(id);
        ReportRepository repo = plugin.getReportRepository();
        Report report = repo.getReportById(id);
        if (report == null) {
            holder.refreshInventory();
            player.sendMessage(ChatColor.RED + "Dieser Report existiert nicht mehr.");
            return;
        }

        holder.refreshInventory();
        ViewReportsGuiCommand.ReportDetailHolder detailHolder =
                new ViewReportsGuiCommand.ReportDetailHolder(plugin, holder, id);
        player.openInventory(detailHolder.getInventory());
    }

    private void handleDetailClick(Player player, ViewReportsGuiCommand.ReportDetailHolder detailHolder,
                                   PersistentDataContainer container) {
        NamespacedKey actionKey = detailHolder.getActionKey();
        if (!container.has(actionKey, PersistentDataType.STRING)) {
            return;
        }

        String action = container.get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }

        if (ViewReportsGuiCommand.ReportDetailHolder.ACTION_BACK.equals(action)) {
            ViewReportsGuiCommand.ReportsHolder parent = detailHolder.getParentHolder();
            parent.refreshInventory();
            activeHolders.put(player.getUniqueId(), parent);
            player.openInventory(parent.getInventory());
            sendPageFeedback(player, parent);
            return;
        }

        ReportRepository repo = plugin.getReportRepository();
        if (repo == null) {
            player.sendMessage(ChatColor.RED + "Das Report-System ist derzeit nicht verfügbar.");
            return;
        }

        int reportId = detailHolder.getReportId();
        Report report = repo.getReportById(reportId);
        if (report == null) {
            detailHolder.refreshInventory();
            player.sendMessage(ChatColor.RED + "Dieser Report existiert nicht mehr.");
            return;
        }

        String newStatus = null;
        if (ViewReportsGuiCommand.ReportDetailHolder.ACTION_STATUS_OPEN.equals(action)) {
            newStatus = "offen";
        } else if (ViewReportsGuiCommand.ReportDetailHolder.ACTION_STATUS_IN_PROGRESS.equals(action)) {
            newStatus = "in Bearbeitung";
        } else if (ViewReportsGuiCommand.ReportDetailHolder.ACTION_STATUS_CLOSED.equals(action)) {
            newStatus = "geschlossen";
        }

        if (newStatus != null) {
            String oldStatus = report.getStatus();
            repo.updateReportStatus(reportId, newStatus);
            detailHolder.refreshInventory();
            player.sendMessage(ChatColor.GREEN + "Report " + reportId + " ist nun " + newStatus + ".");
            notifyDiscordStatusChange(player, reportId, oldStatus, newStatus,
                    player.getName() + " hat den Status geändert");
        }
    }

    private void handleActionClick(Player player, ViewReportsGuiCommand.ReportsHolder holder, String action) {
        ReportRepository repo = plugin.getReportRepository();
        if (repo == null) {
            player.sendMessage(ChatColor.RED + "Das Report-System ist derzeit nicht verfügbar.");
            return;
        }

        Integer selectedId = holder.getSelectedReportId();
        if (selectedId == null) {
            player.sendMessage(ChatColor.YELLOW + "Bitte wähle zuerst einen Report aus.");
            return;
        }

        Report report = repo.getReportById(selectedId);
        if (report == null) {
            holder.refreshInventory();
            player.sendMessage(ChatColor.RED + "Dieser Report existiert nicht mehr.");
            return;
        }

        switch (action) {
            case ACTION_CLAIM: {
                String oldStatus = report.getStatus();
                repo.updateReportAssignment(selectedId, player.getName());
                if ("offen".equalsIgnoreCase(oldStatus)) {
                    repo.updateReportStatus(selectedId, "in Bearbeitung");
                }
                holder.refreshInventory();
                Report updated = repo.getReportById(selectedId);
                if (updated != null) {
                    sendSelectionInfo(player, updated);
                }
                sendPageFeedback(player, holder);
                player.sendMessage(ChatColor.GREEN + "Report " + selectedId + " wurde dir zugewiesen.");
                broadcastToStaff(ChatColor.GOLD + "[Reports] " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY
                        + " hat Report #" + selectedId + " übernommen.", player);
                String newStatus = updated != null ? updated.getStatus() : report.getStatus();
                notifyDiscordStatusChange(player, selectedId, oldStatus, newStatus,
                        player.getName() + " hat den Report übernommen");
                break;
            }
            case ACTION_UNCLAIM: {
                repo.updateReportAssignment(selectedId, null);
                holder.refreshInventory();
                Report updated = repo.getReportById(selectedId);
                if (updated != null) {
                    sendSelectionInfo(player, updated);
                }
                sendPageFeedback(player, holder);
                player.sendMessage(ChatColor.YELLOW + "Report " + selectedId + " wurde freigegeben.");
                broadcastToStaff(ChatColor.GOLD + "[Reports] " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY
                        + " hat Report #" + selectedId + " freigegeben.", player);
                notifyDiscordStatusChange(player, selectedId, report.getStatus(), report.getStatus(),
                        player.getName() + " hat den Report freigegeben");
                break;
            }
            case ACTION_NOTE: {
                pendingNoteEdits.put(player.getUniqueId(), new PendingNote(holder, selectedId));
                player.closeInventory();
                player.sendMessage(ChatColor.AQUA + "Gib nun im Chat die neue Notiz ein.\n" +
                        ChatColor.GRAY + "Schreibe 'clear' zum Löschen oder 'cancel' zum Abbrechen.");
                break;
            }
            default:
                break;
        }
    }

    private void sendPageFeedback(Player player, ViewReportsGuiCommand.ReportsHolder holder) {
        player.sendMessage(ChatColor.GOLD + "Seite " + (holder.getPage() + 1) + "/" + holder.getTotalPages() +
                ChatColor.GRAY + " | Filter: " + ChatColor.AQUA + holder.getFilterDisplayName());
    }

    private void sendSelectionInfo(Player player, Report report) {
        player.sendMessage(ChatColor.GOLD + "Report #" + report.getId() + ChatColor.GRAY + " | Status: "
                + ChatColor.WHITE + report.getStatus());
        String assigned = report.getAssignedStaff() == null || report.getAssignedStaff().isBlank()
                ? ChatColor.RED + "(niemand)"
                : ChatColor.GREEN + report.getAssignedStaff();
        player.sendMessage(ChatColor.GRAY + "Zuständig: " + assigned);
        String note = report.getNotes();
        if (note == null || note.isBlank()) {
            player.sendMessage(ChatColor.GRAY + "Notiz: " + ChatColor.DARK_GRAY + "-");
        } else {
            player.sendMessage(ChatColor.GRAY + "Notiz:");
            for (String line : wrapText(note, 40)) {
                player.sendMessage(ChatColor.WHITE + line);
            }
        }
    }

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= maxLength) {
                lines.add(remaining);
                break;
            }
            int split = remaining.lastIndexOf(' ', maxLength);
            if (split <= 0) {
                split = maxLength;
            }
            lines.add(remaining.substring(0, split));
            remaining = remaining.substring(Math.min(split + 1, remaining.length()));
        }
        return lines;
    }

    private void notifyDiscordStatusChange(Player staffMember, int reportId, String oldStatus, String newStatus,
                                           String context) {
        if (discordNotifier == null) {
            plugin.getPrefixedLogger().warning("DiscordNotifier ist nicht konfiguriert. Statusänderung von Report " + reportId +
                    " wurde nicht an Discord gesendet.");
            return;
        }

        String previousStatus = (oldStatus == null || oldStatus.isBlank()) ? "unbekannt" : oldStatus;
        String updatedStatus = (newStatus == null || newStatus.isBlank()) ? "unbekannt" : newStatus;

        ReportRepository repository = plugin.getReportRepository();
        Report updatedReport = repository != null ? repository.getReportById(reportId) : null;
        String assigned = updatedReport != null && updatedReport.getAssignedStaff() != null
                && !updatedReport.getAssignedStaff().isBlank()
                ? updatedReport.getAssignedStaff()
                : "nicht zugewiesen";
        String note = updatedReport != null ? updatedReport.getNotes() : null;
        if (note == null || note.isBlank()) {
            note = "-";
        }

        String message = "**Report-Status aktualisiert**\n" +
                "Report-ID: `" + reportId + "`\n" +
                "Status: `" + previousStatus + "` → `" + updatedStatus + "`\n" +
                "Bearbeitet von: `" + staffMember.getName() + "`\n" +
                "Aktion: `" + context + "`\n" +
                "Zuständig: `" + assigned + "`\n" +
                "Notiz: `" + note + "`";

        discordNotifier.sendReportNotification(message);
    }

    @EventHandler
    public void onNoteChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PendingNote pending = pendingNoteEdits.get(uuid);
        if (pending == null) {
            return;
        }

        event.setCancelled(true);
        pendingNoteEdits.remove(uuid);
        String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            ReportRepository repo = plugin.getReportRepository();
            if (repo == null) {
                player.sendMessage(ChatColor.RED + "Das Report-System ist derzeit nicht verfügbar.");
                return;
            }

            String noteText = message;
            if (noteText.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "Notizbearbeitung abgebrochen.");
                reopenHolder(player, pending);
                return;
            }

            if (noteText.equalsIgnoreCase("clear")) {
                noteText = "";
            }

            repo.updateReportNotes(pending.reportId, noteText);
            Report updated = repo.getReportById(pending.reportId);
            if (updated != null) {
                player.sendMessage(ChatColor.GREEN + "Notiz für Report " + pending.reportId + " gespeichert.");
                sendSelectionInfo(player, updated);
                notifyDiscordStatusChange(player, pending.reportId, updated.getStatus(), updated.getStatus(),
                        player.getName() + (noteText.isEmpty() ? " hat die Notiz entfernt" : " hat die Notiz aktualisiert"));
                broadcastToStaff(ChatColor.GOLD + "[Reports] " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY
                        + (noteText.isEmpty() ? " hat die Notiz von Report #" + pending.reportId + " entfernt."
                        : " hat die Notiz von Report #" + pending.reportId + " aktualisiert."), player);
            }

            reopenHolder(player, pending);
        });
    }

    private void reopenHolder(Player player, PendingNote pending) {
        ViewReportsGuiCommand.ReportsHolder holder = pending.holder;
        holder.setSelectedReportId(pending.reportId);
        holder.refreshInventory();
        activeHolders.put(player.getUniqueId(), holder);
        player.openInventory(holder.getInventory());
        sendPageFeedback(player, holder);
    }

    private void broadcastToStaff(String message, Player exclude) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(exclude)) {
                continue;
            }
            if (online.hasPermission("akzuwoextension.staff")) {
                online.sendMessage(message);
            }
        }
    }
}

package ch.ksrminecraft.akzuwoextension.commands;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.akzuwoextension.utils.Report;
import ch.ksrminecraft.akzuwoextension.utils.ReportRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ViewReportsGuiListener implements Listener {

    private final AkzuwoExtension plugin;
    private final NamespacedKey reportIdKey;
    private final NamespacedKey controlKey;

    public ViewReportsGuiListener(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.reportIdKey = new NamespacedKey(plugin, ViewReportsGuiCommand.REPORT_ID_KEY);
        this.controlKey = new NamespacedKey(plugin, ViewReportsGuiCommand.CONTROL_KEY);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ViewReportsGuiCommand.ReportsHolder)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getHolder() != event.getInventory().getHolder()) {
            return;
        }
        event.setCancelled(true);

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = currentItem.getItemMeta();
        if (meta == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ViewReportsGuiCommand.ReportsHolder holder = (ViewReportsGuiCommand.ReportsHolder) event.getInventory().getHolder();

        String control = meta.getPersistentDataContainer().get(controlKey, PersistentDataType.STRING);
        if (control != null) {
            handleControlClick(player, holder, event.getInventory(), control);
            return;
        }

        if (currentItem.getType() != Material.PAPER) {
            return;
        }

        boolean rightClick = event.getClick().isRightClick();
        boolean leftClick = event.getClick().isLeftClick();
        if (!rightClick && !leftClick) {
            return;
        }

        Integer id = meta.getPersistentDataContainer().get(reportIdKey, PersistentDataType.INTEGER);
        if (id == null) {
            return;
        }

        ReportRepository repo = plugin.getReportRepository();
        Report report = repo.getReportById(id);
        if (report == null) {
            return;
        }

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
            player.sendMessage(ChatColor.GREEN + "Report " + id + " ist nun " + newStatus + ".");
            holder.getCommand().refreshInventory(player, holder, event.getInventory());
        }
    }

    private void handleControlClick(Player player, ViewReportsGuiCommand.ReportsHolder holder,
                                     Inventory inventory, String control) {
        switch (control) {
            case ViewReportsGuiCommand.CONTROL_PREVIOUS:
                if (holder.getPage() > 0) {
                    holder.setPage(holder.getPage() - 1);
                    holder.getCommand().refreshInventory(player, holder, inventory);
                } else {
                    player.sendMessage(ChatColor.RED + "Du bist bereits auf der ersten Seite.");
                }
                break;
            case ViewReportsGuiCommand.CONTROL_NEXT:
                if (holder.getPage() < holder.getTotalPages() - 1) {
                    holder.setPage(holder.getPage() + 1);
                    holder.getCommand().refreshInventory(player, holder, inventory);
                } else {
                    player.sendMessage(ChatColor.RED + "Du bist bereits auf der letzten Seite.");
                }
                break;
            case ViewReportsGuiCommand.CONTROL_FILTER:
                holder.setOnlyOpen(!holder.isOnlyOpen());
                holder.setPage(0);
                holder.getCommand().refreshInventory(player, holder, inventory);
                break;
            default:
                break;
        }
    }
}

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ViewReportsGuiListener implements Listener {

    private final AkzuwoExtension plugin;

    public ViewReportsGuiListener(AkzuwoExtension plugin) {
        this.plugin = plugin;
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

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PAPER) {
            return;
        }
        boolean rightClick = event.getClick().isRightClick();
        boolean leftClick = event.getClick().isLeftClick();
        if (!rightClick && !leftClick) {
            return;
        }

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = new NamespacedKey(plugin, "reportId");
        Integer id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (id == null) return;

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
            event.getCurrentItem().setItemMeta(meta);
            ((Player) event.getWhoClicked()).sendMessage(ChatColor.GREEN + "Report " + id + " ist nun " + newStatus + ".");
        }
    }
}

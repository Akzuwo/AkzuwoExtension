package ch.ksrminecraft.akzuwoextension.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet aktive "Leinen", die ein Ziel in festen Abständen zum Besitzer teleportieren.
 */
public class LeineManager {

    private final Plugin plugin;
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    public LeineManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet eine neue Leine und ersetzt eine bestehende für den selben Besitzer.
     */
    public void startLeine(Player owner, Player target) {
        stopLeine(owner);

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!owner.isOnline() || !target.isOnline()) {
                stopLeine(owner.getUniqueId());
                return;
            }

            target.teleport(owner.getLocation());
        }, 0L, 16L).getTaskId(); // 0.8 Sekunden Intervall

        activeTasks.put(owner.getUniqueId(), taskId);
    }

    /**
     * Stoppt die Leine eines Besitzers, falls vorhanden.
     */
    public boolean stopLeine(Player owner) {
        return stopLeine(owner.getUniqueId());
    }

    private boolean stopLeine(UUID ownerId) {
        Integer taskId = activeTasks.remove(ownerId);
        if (taskId == null) {
            return false;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        return true;
    }

    public void cancelAll() {
        for (Integer taskId : activeTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        activeTasks.clear();
    }
}

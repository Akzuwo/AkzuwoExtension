package ch.ksrminecraft.akzuwoextension.utils;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Placeholder für die RankPointsAPI.
 */
public class RankPointsPlaceholder extends PlaceholderExpansion {

    private final AkzuwoExtension plugin;
    private final Map<UUID, CacheEntry> cache = new HashMap<>();
    private final long cacheDuration;

    public RankPointsPlaceholder(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.cacheDuration = plugin.getRankPointsCheckInterval();
    }

    @Override
    public String getIdentifier() {
        return "akzuwo";
    }

    @Override
    public String getAuthor() {
        return "Akzuwo";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equalsIgnoreCase("rankpoints")) {
            PointsAPI api = plugin.getPointsAPI();
            if (api != null) {
                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                CacheEntry entry = cache.get(uuid);
                if (entry == null || now - entry.timestamp > cacheDuration) {
                    int points = api.getPoints(uuid);
                    entry = new CacheEntry(points, now);
                    cache.put(uuid, entry);
                }
                return String.valueOf(entry.points);
            }
            return "0";
        }

        return null;
    }
    private static class CacheEntry {
        final int points;
        final long timestamp;

        CacheEntry(int points, long timestamp) {
            this.points = points;
            this.timestamp = timestamp;
        }
    }
}


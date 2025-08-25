package ch.ksrminecraft.akzuwoextension.utils;

import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;
import ch.ksrminecraft.RankPointsAPI.PointsAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Placeholder für die RankPointsAPI.
 */
public class RankPointsPlaceholder extends PlaceholderExpansion {

    private final AkzuwoExtension plugin;

    public RankPointsPlaceholder(AkzuwoExtension plugin) {
        this.plugin = plugin;
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
                return String.valueOf(api.getPoints(player.getUniqueId()));
            }
            return "0";
        }

        return null;
    }
}


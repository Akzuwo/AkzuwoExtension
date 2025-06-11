package ch.ksrminecraft.akzuwoextension.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ch.ksrminecraft.akzuwoextension.AkzuwoExtension;

public class DiscordNotifier {

    private final AkzuwoExtension plugin;
    private JDA jda;
    private final String botToken;
    private final String channelIdReport;
    private final String channelIdNotification; // Channel-ID, in den die Nachrichten gesendet werden

    public DiscordNotifier(AkzuwoExtension plugin) {
        this.plugin = plugin;
        this.botToken = plugin.getConfig().getString("discord-bot-token");
        this.channelIdReport = plugin.getConfig().getString("discord-channel-id-reports");
        this.channelIdNotification = plugin.getConfig().getString("discord-channel-id-notifications");
    }

    /**
     * Initialisiert den Discord-Bot.
     *
     * @return true, wenn der Bot erfolgreich gestartet wurde, sonst false
     */
    public boolean initialize() {
        if (botToken == null || botToken.isBlank()) {
            plugin.getLogger().warning("Kein Discord-Bot-Token gesetzt. Discord-Funktion deaktiviert.");
            return false;
        }

        try {
            jda = JDABuilder.createDefault(botToken).build();
            jda.awaitReady(); // Warten, bis der Bot vollständig gestartet ist
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Discord-Bot konnte nicht gestartet werden: " + e.getMessage());
            return false;
        }
    }

    public void sendReportNotification(String message) {
        sendMessage(channelIdReport, message);
    }

    public void sendServerNotification(String message) {
        sendMessage(channelIdNotification, message);
    }

    private void sendMessage(String channelId, String message) {
        if (jda == null || channelId == null || channelId.isBlank()) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            plugin.getLogger().warning("Textkanal nicht gefunden.");
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}


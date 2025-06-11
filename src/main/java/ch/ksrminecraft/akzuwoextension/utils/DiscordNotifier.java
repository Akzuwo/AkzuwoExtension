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

    public void initialize() {
        try {
            jda = JDABuilder.createDefault(botToken).build();
            jda.awaitReady(); // Warten, bis der Bot vollständig gestartet ist
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendReportNotification(String message) {
        sendMessage(channelIdReport, message);
    }

    public void sendServerNotification(String message) {
        sendMessage(channelIdNotification, message);
    }

    private void sendMessage(String channelId, String message) {
        if (jda != null) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
            } else {
                System.err.println("Textkanal nicht gefunden.");
            }
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}


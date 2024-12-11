package ch.ksrminecraft.akzuwoextension.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class DiscordNotifier {

    private JDA jda;
    private final String botToken = plugin.getConfig().getString("discord-bot-token");
    private final String channelIdReport = plugin.getConfig().getString("discord-channel-id-reports");
    private final String channelIdNotification = plugin.getConfig().getString("discord-channel-id-notifications")// Channel-ID, in den die Nachrichten gesendet werden

    public void initialize() {
        try {
            jda = JDABuilder.createDefault(botToken).build();
            jda.awaitReady(); // Warten, bis der Bot vollständig gestartet ist
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendReportNotification(String message) {
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


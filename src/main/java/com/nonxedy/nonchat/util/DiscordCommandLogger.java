package com.nonxedy.nonchat.util;

import java.awt.Color;
import java.time.Instant;

import com.nonxedy.nonchat.config.PluginMessages;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;

public class DiscordCommandLogger {
    
    private final TextChannel logChannel;
    private final PluginMessages messages;

    public DiscordCommandLogger(String channelId, PluginMessages messages) {
        this.logChannel = DiscordUtil.getTextChannelById(channelId);
        this.messages = messages;
    }

    public void logCommand(String executor, String command, boolean success) {
        if (logChannel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(messages.getString("discord.log.title"))
            .addField(messages.getString("discord.log.executor"), executor, true)
            .addField(messages.getString("discord.log.command"), command, true)
            .addField(messages.getString("discord.log.status"), 
                messages.getString(success ? 
                    "discord.log.status.success" : 
                    "discord.log.status.failed"), true)
            .setColor(success ? Color.GREEN : Color.RED)
            .setTimestamp(Instant.now());

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
}

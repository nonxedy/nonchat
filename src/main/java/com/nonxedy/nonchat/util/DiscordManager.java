package com.nonxedy.nonchat.util;

import com.nonxedy.nonchat.nonchat;

import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;

public class DiscordManager {

    private final nonchat plugin;
    private TextChannel staffChannel;
    private TextChannel chatChannel;

    public DiscordManager(nonchat plugin) {
        this.plugin = plugin;
        setupChannels();
    }
    
    private void setupChannels() {
        String staffChannelId = plugin.getConfig().getString("discord.staff-channel-id");
        String chatChannelId = plugin.getConfig().getString("discord.chat-channel-id");

        if (staffChannelId != null) {
            staffChannel = DiscordUtil.getTextChannelById(staffChannelId);
        }

        if (chatChannelId != null) {
            chatChannel = DiscordUtil.getTextChannelById(chatChannelId);
        }
    }

    private void sendStaffMessage(String sender, String message) {
        if (staffChannel != null) {
            String formatted = ColorUtil.parseColor(plugin.getConfig().getString("discord.staff-format")
                    .replace("{sender}", sender)
                    .replace("{message}", message));
            staffChannel.sendMessage(formatted).queue();
        }
    }

    public void sendChatMessage(String sender, String message) {
        if (chatChannel != null) {
            String formatted = ColorUtil.parseColor(plugin.getConfig().getString("discord.chat-format")
                .replace("{sender}", sender)
                .replace("{message}", message));
            chatChannel.sendMessage(formatted).queue();
        }
    }
}

package com.nonxedy.nonchat.config;

import org.bukkit.configuration.file.FileConfiguration;

import com.nonxedy.nonchat.nonchat;

public class DiscordManager {

    private final ConfigFile configFile;
    private final nonchat plugin;

    private String discordHook;
    private boolean useDiscordSRV;

    private boolean joinEnabled;
    private String joinTitle;
    private String joinDescription;
    private int joinColor;
    private boolean joinAvatarEnabled;

    private boolean quitEnabled;
    private String quitTitle;
    private String quitDescription;
    private int quitColor;
    private boolean quitAvatarEnabled;

    private boolean deathEnabled;
    private String deathTitle;
    private String deathDescription;
    private int deathColor;
    private boolean deathAvatarEnabled;

    private String bannedWordsTitle;
    private String bannedWordsDescription;
    private int bannedWordsColor;
    private boolean bannedWordsAvatar;
    private boolean bannedWordsEnabled;

    private boolean muteEnabled;
    private String muteWebhook;
    private String muteTitle;
    private String muteDescription;
    private int muteColor;
    private boolean muteAvatar;

    private String bannedCommandsTitle;
    private String bannedCommandsDescription;
    private int bannedCommandsColor;
    private boolean bannedCommandsAvatar;
    private boolean bannedCommandsEnabled;

    public DiscordManager(nonchat plugin) {
        this.plugin = plugin;
        this.configFile = new ConfigFile("discord.yml", "hooks", plugin);
        this.configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = configFile.getConfig();
        discordHook = config.getString("discord.webhook");
        useDiscordSRV = config.getBoolean("discord.use-discordsrv", true);

        joinEnabled = config.getBoolean("discord.join.enabled");
        if (joinEnabled) {
            joinTitle = config.getString("discord.join.title");
            joinDescription = config.getString("discord.join.description");
            joinColor = config.getInt("discord.join.color");
            joinAvatarEnabled = config.getBoolean("discord.join.avatar-enabled");
        }

        quitEnabled = config.getBoolean("discord.quit.enabled");
        if (quitEnabled) {
            quitTitle = config.getString("discord.quit.title");
            quitDescription = config.getString("discord.quit.description");
            quitColor = config.getInt("discord.quit.color");
            quitAvatarEnabled = config.getBoolean("discord.quit.avatar-enabled");
        }

        deathEnabled = config.getBoolean("discord.death.enabled");
        if (deathEnabled) {
            deathTitle = config.getString("discord.death.title");
            deathDescription = config.getString("discord.death.description");
            deathColor = config.getInt("discord.death.color");
            deathAvatarEnabled = config.getBoolean("discord.death.avatar-enabled");
        }

        // Banned words settings - unconditional now
        bannedWordsEnabled = config.getBoolean("discord.banned-words.enabled", false);
        bannedWordsTitle = config.getString("discord.banned-words.title", "Banned Word Detected!");
        bannedWordsDescription = config.getString("discord.banned-words.description", "Player %player% used a banned word: %word%\nMessage: \"%message%\"");
        bannedWordsColor = config.getInt("discord.banned-words.color", 15158332);
        bannedWordsAvatar = config.getBoolean("discord.banned-words.avatar-enabled", true);

        muteEnabled = config.getBoolean("discord.mute.enabled", false);
        if (muteEnabled) {
            muteWebhook = config.getString("discord.mute.webhook");
            muteTitle = config.getString("discord.mute.title", "Player muted!");
            muteDescription = config.getString("discord.mute.description", "Player %player% has muted by %admin%");
            muteColor = config.getInt("discord.mute.color", 15158332);
            muteAvatar = config.getBoolean("discord.mute.avatar-enabled", true);
        }

        // Banned commands settings - unconditional now
        bannedCommandsEnabled = config.getBoolean("discord.banned-commands.enabled", false);
        bannedCommandsTitle = config.getString("discord.banned-commands.title", "Banned Command Detected!");
        bannedCommandsDescription = config.getString("discord.banned-commands.description", "Player %player% used a banned command: %word%\nMessage: \"%message%\"");
        bannedCommandsColor = config.getInt("discord.banned-commands.color", 15158332);
        bannedCommandsAvatar = config.getBoolean("discord.banned-commands.avatar-enabled", true);
    }

    public void reloadConfig() {
        configFile.reloadConfig();
        loadConfig();
    }

    public String getBannedCommandsTitle() {
        return bannedCommandsTitle;
    }

    public String getBannedCommandsDescription() {
        return bannedCommandsDescription;
    }

    public int getBannedCommandsColor() {
        return bannedCommandsColor;
    }

    public boolean isBannedCommandsAvatar() {
        return bannedCommandsAvatar;
    }

    public boolean isBannedCommandsEnabled() {
        return bannedCommandsEnabled;
    }

    public boolean isBannedWordsAvatar() {
        return bannedWordsAvatar;
    }

    public String getBannedWordsTitle() {
        return bannedWordsTitle;
    }

    public String getBannedWordsDescription() {
        return bannedWordsDescription;
    }

    public int getBannedWordsColor() {
        return bannedWordsColor;
    }

    public boolean isBannedWordsEnabled() {
        return bannedWordsEnabled;
    }

    public boolean isMuteEnabled() {
        return muteEnabled;
    }

    public String getMuteWebhook() {
        return muteWebhook;
    }

    public String getMuteTitle() {
        return muteTitle;
    }

    public String getMuteDescription() {
        return muteDescription;
    }

    public int getMuteColor() {
        return muteColor;
    }

    public boolean isMuteAvatar() {
        return muteAvatar;
    }

    public String getDiscordHook() {
        return discordHook;
    }

    public boolean isUseDiscordSRV() {
        return useDiscordSRV;
    }

    public boolean isJoinEnabled() {
        return joinEnabled;
    }

    public String getJoinTitle() {
        return joinTitle;
    }

    public String getJoinDescription() {
        return joinDescription;
    }

    public int getJoinColor() {
        return joinColor;
    }

    public boolean isJoinAvatarEnabled() {
        return joinAvatarEnabled;
    }

    public boolean isQuitEnabled() {
        return quitEnabled;
    }

    public String getQuitTitle() {
        return quitTitle;
    }

    public String getQuitDescription() {
        return quitDescription;
    }

    public int getQuitColor() {
        return quitColor;
    }

    public boolean isQuitAvatarEnabled() {
        return quitAvatarEnabled;
    }

    public boolean isDeathEnabled() {
        return deathEnabled;
    }

    public String getDeathTitle() {
        return deathTitle;
    }

    public String getDeathDescription() {
        return deathDescription;
    }

    public int getDeathColor() {
        return deathColor;
    }

    public boolean isDeathAvatarEnabled() {
        return deathAvatarEnabled;
    }
}

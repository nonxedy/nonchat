package com.nonxedy.nonchat.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nonxedy.nonchat.utils.ColorUtil;
import com.nonxedy.nonchat.utils.MessageFormatter;

import net.kyori.adventure.text.Component;

public class PluginMessages {

    private File file;
    private FileConfiguration messages;
    private final MessageFormatter formatter;

    public PluginMessages() {
        file = new File("plugins/nonchat", "messages.yml");
        this.formatter = new MessageFormatter(this);
        if (!file.exists()) {
            createDefaultConfig();
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }
    
    private void createDefaultConfig() {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
    
            messages = new YamlConfiguration();
            messages.set("no-permission", "You do not have permission to use this command!");
            messages.set("player-only", "Only players can use this command!");
            messages.set("server-info", "Server Information:");
            messages.set("java-version", "Java Version: ");
            messages.set("port", "Port: ");
            messages.set("version", "Version: ");
            messages.set("os-name", "OS Name: ");
            messages.set("os-version", "OS Version: ");
            messages.set("cpu-cores", "CPU Cores: ");
            messages.set("cpu-family", "CPU Family: ");
            messages.set("number-of-plugins", "Number of Plugins: ");
            messages.set("number-of-worlds", "Number of Worlds: ");
            messages.set("reloading", "Reloading...");
            messages.set("reloaded", "Plugin reloaded!");
            messages.set("reload-failed", "Failed to reload plugin!");
            messages.set("help", "nonchat | commands:");
            messages.set("nreload", "/nreload - reload plugin");
            messages.set("help-command", "/help - commands list");
            messages.set("server-command", "/server - server information");
            messages.set("message-command", "/m <player> <message> (msg, w, whisper, message) - sent a message to a player");
            messages.set("broadcast-command", "/bc <message> (broadcast) - sent a message to all server");
            messages.set("ignore-command", "/ignore <player> - ignore a player");
            messages.set("sc-command", "/sc <message> - sent a message to staff");
            messages.set("spy-command", "/spy - enable/disable spy mode");
            messages.set("clear-chat", "Chat clearing...");
            messages.set("chat-cleared", "Chat cleared!");
            messages.set("broadcast", "Broadcast: {message}");
            messages.set("player-not-found", "Player not found.");
            messages.set("invalid-usage-message", "Use: /m <player> <message>");
            messages.set("invalid-usage-ignore", "Use: /ignore <player>");
            messages.set("invalid-usage-sc", "Use: /sc <message>");
            messages.set("invalid-usage-spy", "Use: /spy");
            messages.set("cannot-ignore-self", "You cant ignore yourself..");
            messages.set("ignored-player", "You started ignored the player {player}.");
            messages.set("unignored-player", "You no longer ignore the player {player}.");
            messages.set("ignored-by-target", "This player ignores you and you cant send him a message.");
            messages.set("spy-mode-enabled", "Spy mode enabled.");
            messages.set("spy-mode-disabled", "Spy mode disabled.");
            messages.set("blocked-words", "You are not allowed to use this word!");
            messages.set("mentioned", "You were mentioned in chat by {player}!");
            
            messages.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getNoPermission() {
        return getColoredString("no-permission");
    }

    public String getPlayerOnly() {
        return getColoredString("player-only");
    }

    public String getServerInfo() {
        return getColoredString("server-info");
    }

    public String getJavaVersion() {
        return getColoredString("java-version");
    }

    public String getPort() {
        return getColoredString("port");
    }

    public String getVersion() {
        return getColoredString("version");
    }

    public String getOsName() {
        return getColoredString("os-name");
    }

    public String getOsVersion() {
        return getColoredString("os-version");
    }

    public String getCpuCores() {
        return getColoredString("cpu-cores");
    }

    public String getCpuFamily() {
        return getColoredString("cpu-family");
    }

    public String getNumberOfPlugins() {
        return getColoredString("number-of-plugins");
    }

    public String getNumberOfWorlds() {
        return getColoredString("number-of-worlds");
    }
    
    public String getReloading() {
        return getColoredString("reloading");
    }
    
    public String getReloaded() {
        return getColoredString("reloaded");
    }
    
    public String getReloadFailed() {
        return getColoredString("reload-failed");
    }
    
    public String getHelp() {
        return getColoredString("help");
    }

    public String getNreload() {
        return getColoredString("nreload");
    }
    
    public String getHelpCommand() {
        return getColoredString("help-command");
    }
    
    public String getServerCommand() {
        return getColoredString("server-command");
    }
    
    public String getMessageCommand() {
        return getColoredString("message-command");
    }
    
    public String getBroadcastCommand() {
        return getColoredString("broadcast-command");
    }
    
    public String getScCommand() {
        return getColoredString("sc-command");
    }
    
    public String getClearChat() {
        return getColoredString("clear-chat");
    }
    
    public String getChatCleared() {
        return getColoredString("chat-cleared");
    }
    
    public String getBroadcast() {
        return getColoredString("broadcast");
    }
    
    public String getPlayerNotFound() {
        return getColoredString("player-not-found");
    }
    
    public String getInvalidUsageMessage() {
        return getColoredString("invalid-usage-message");
    }

    public String getIgnoreCommand() {
        return getColoredString("ignore-command");
    }

    public String getInvalidUsageIgnore() {
        return getColoredString("invalid-usage-ignore");
    }

    public String getIgnoredPlayer(String player) {
        return getColoredString("ignored-player").replace("{player}", player);
    }

    public String getUnignoredPlayer(String player) {
        return getColoredString("unignored-player").replace("{player}", player);
    }

    public String getIgnoredByTarget() {
        return getColoredString("ignored-by-target");
    }

    public String getCannotIgnoreSelf() {
        return getColoredString("cannot-ignore-self");
    }

    public String getInvalidUsageSc() {
        return getColoredString("invalid-usage-sc");
    }

    public String getSpyCommand() {
        return getColoredString("spy-command");
    }

    public String getInvalidUsageSpy() {
        return getColoredString("invalid-usage-spy");
    }

    public String getSpyModeEnabled() {
        return getColoredString("spy-mode-enabled");
    }

    public String getSpyModeDisabled() {
        return getColoredString("spy-mode-disabled");
    }

    public String getBlockedWords() {
        return getColoredString("blocked-words");
    }

    public String getMentioned() {
        return getColoredString("mentioned");
    }

    private String getColoredString(String key) {
        return ColorUtil.parseColor(messages.getString(key, ""));
    }
    public void saveConfig() {
        try {
            messages.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(String path) {
        return messages.getString(path);
    }

    public Component getFormatted(String path, Object... args) {
        return formatter.format(path, args);
    }
}

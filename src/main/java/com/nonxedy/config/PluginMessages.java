package com.nonxedy.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PluginMessages {

    private File file;
    private FileConfiguration messages;

    public PluginMessages() {
        file = new File("plugins/nonchat", "messages.yml");
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
            messages.set("no-permission", "Недостаточно прав");
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
            messages.set("reloading", "Перезагрузка...");
            messages.set("reloaded", "Плагин перезагружен!");
            messages.set("reload-failed", "Не удалось перезагрузить плагин!");
            messages.set("help", "nonchat | Команды плагина:");
            messages.set("nreload", "/nreload - перезагрузка плагина");
            messages.set("help-command", "/help - список команд");
            messages.set("server-command", "/server - информация о сервере");
            messages.set("message-command", "/m <игрок> <сообщение> (msg, w, whisper, message) - отправка личных сообщений");
            messages.set("broadcast-command", "/bc <сообщение> (broadcast) - отправка сообщений всем игрокам");
            messages.set("ignore-command", "/ignore <игрок> - игнорировать игрока");
            messages.set("clear-chat", "Очистка чата...");
            messages.set("chat-cleared", "Чат очищен");
            messages.set("broadcast", "Оповещение: ");
            messages.set("player-not-found", "Игрок не найден.");
            messages.set("invalid-usage-message", "Используйте: /m <игрок> <сообщение>");
            messages.set("invalid-usage-ignore", "Используйте: /ignore <игрок>");
            messages.set("ignored-player", "Вы начали игнорировать игрока {player}.");
            messages.set("unignored-player", "Вы больше не игнорируете игрока {player}.");
            messages.set("ignored-by-target", "Этот игрок игнорирует вас и вы не можете отправить ему сообщение.");
            
            messages.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getNoPermission() {
        return messages.getString("no-permission");
    }

    public String getServerInfo() {
        return messages.getString("server-info");
    }

    public String getJavaVersion() {
        return messages.getString("java-version");
    }

    public String getPort() {
        return messages.getString("port");
    }

    public String getVersion() {
        return messages.getString("version");
    }

    public String getOsName() {
        return messages.getString("os-name");
    }

    public String getOsVersion() {
        return messages.getString("os-version");
    }

    public String getCpuCores() {
        return messages.getString("cpu-cores");
    }

    public String getCpuFamily() {
        return messages.getString("cpu-family");
    }

    public String getNumberOfPlugins() {
        return messages.getString("number-of-plugins");
    }

    public String getNumberOfWorlds() {
        return messages.getString("number-of-worlds");
    }
    
    public String getReloading() {
        return messages.getString("reloading");
    }
    
    public String getReloaded() {
        return messages.getString("reloaded");
    }
    
    public String getReloadFailed() {
        return messages.getString("reload-failed");
    }
    
    public String getHelp() {
        return messages.getString("help");
    }

    public String getNreload() {
        return messages.getString("nreload");
    }
    
    public String getHelpCommand() {
        return messages.getString("help-command");
    }
    
    public String getServerCommand() {
        return messages.getString("server-command");
    }
    
    public String getMessageCommand() {
        return messages.getString("message-command");
    }
    
    public String getBroadcastCommand() {
        return messages.getString("broadcast-command");
    }
    
    public String getClearChat() {
        return messages.getString("clear-chat");
    }
    
    public String getChatCleared() {
        return messages.getString("chat-cleared");
    }
    
    public String getBroadcast() {
        return messages.getString("broadcast");
    }
    
    public String getPlayerNotFound() {
        return messages.getString("player-not-found");
    }
    
    public String getInvalidUsageMessage() {
        return messages.getString("invalid-usage-message");
    }

    public String getIgnoreCommand() {
        return messages.getString("ignore-command");
    }

    public String getInvalidUsageIgnore() {
        return messages.getString("invalid-usage-ignore");
    }

    public String getIgnoredPlayer(String player) {
        return messages.getString("ignored-player").replace("{player}", player);
    }

    public String getUnignoredPlayer(String player) {
        return messages.getString("unignored-player").replace("{player}", player);
    }

    public String getIgnoredByTarget() {
        return messages.getString("ignored-by-target");
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
}

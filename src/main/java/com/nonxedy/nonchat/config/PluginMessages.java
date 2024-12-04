package com.nonxedy.nonchat.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nonxedy.nonchat.utils.ColorUtil;

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
            messages.set("sc-command", "/sc <сообщение> - отправка сообщений всему персоналу");
            messages.set("spy-command", "/spy - активация режима шпиона");
            messages.set("clear-chat", "Очистка чата...");
            messages.set("chat-cleared", "Чат очищен");
            messages.set("broadcast", "Оповещение: ");
            messages.set("player-not-found", "Игрок не найден.");
            messages.set("invalid-usage-message", "Используйте: /m <игрок> <сообщение>");
            messages.set("invalid-usage-ignore", "Используйте: /ignore <игрок>");
            messages.set("invalid-usage-sc", "Используйте: /sc <сообщение>");
            messages.set("invalid-usage-spy", "Используйте: /spy");
            messages.set("ignored-player", "Вы начали игнорировать игрока {player}.");
            messages.set("unignored-player", "Вы больше не игнорируете игрока {player}.");
            messages.set("ignored-by-target", "Этот игрок игнорирует вас и вы не можете отправить ему сообщение.");
            messages.set("spy-mode-enabled", "Режим шпиона включен.");
            messages.set("spy-mode-disabled", "Режим шпиона выключен.");
            messages.set("blocked-words", "Вы отправляете запрещенное слово!");
            messages.set("mentioned", "Вы были упомянуты в чате {player}!");
            
            messages.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getNoPermission() {
        return getColoredString("no-permission");
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
}

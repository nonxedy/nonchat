package com.nonxedy.nonchat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.nonxedy.nonchat.command.BroadcastCommand;
import com.nonxedy.nonchat.command.ClearCommand;
import com.nonxedy.nonchat.command.IgnoreCommand;
import com.nonxedy.nonchat.command.MeCommand;
import com.nonxedy.nonchat.command.MessageCommand;
import com.nonxedy.nonchat.command.NhelpCommand;
import com.nonxedy.nonchat.command.NreloadCommand;
import com.nonxedy.nonchat.command.RollCommand;
import com.nonxedy.nonchat.command.ServerCommand;
import com.nonxedy.nonchat.command.SpyCommand;
import com.nonxedy.nonchat.command.StaffChatCommand;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.listeners.ChatBubbleListener;
import com.nonxedy.nonchat.listeners.ChatFormatListener;
import com.nonxedy.nonchat.listeners.DeathCoordinates;
import com.nonxedy.nonchat.listeners.DeathListener;
import com.nonxedy.nonchat.placeholders.NonchatExpansion;
import com.nonxedy.nonchat.utils.AutoBroadcastSender;
import com.nonxedy.nonchat.utils.BroadcastMessage;
import com.nonxedy.nonchat.utils.Debugger;
import com.nonxedy.nonchat.utils.DiscordCommandHandler;
import com.nonxedy.nonchat.utils.DiscordManager;
import com.nonxedy.nonchat.utils.HoverTextUtil;
import com.nonxedy.nonchat.utils.IgnoreManager;
import com.nonxedy.nonchat.utils.IntegrationUtil;

import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

// Main plugin class that extends JavaPlugin
public class nonchat extends JavaPlugin {

    // Declare listener instances
    private ChatFormatListener chatFormatListener;
    private DeathListener deathListener;
    private DeathCoordinates deathCoordinates;
    private AutoBroadcastSender autoBroadcastSender;
    
    // Configuration handlers
    private PluginConfig pluginConfig;
    private PluginMessages pluginMessages;
    
    // Utility class instances
    private BroadcastMessage broadcastMessage;
    private SpyCommand spyCommand;
    private Debugger debugger;
    private ChatBubbleListener chatBubbleListener;
    private IntegrationUtil integrationUtil;
    private DiscordManager discordManager;

    // Map to store ignored players (key: player UUID, value: set of ignored player UUIDs)
    public Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();

    // Plugin directory path
    File plugin_directory = new File("plugins/nonchat");

    // Called when plugin is enabled
    @Override
    public void onEnable() {
        // Save default configuration files
        saveDefaultConfig();
        saveResource("langs/messages_en.yml", false);
        saveResource("langs/messages_ru.yml", false);

        // Initialize plugin components
        registerPlaceholders();
        registerConfigs();
        registerCommands();
        registerListeners();
        registerUtils();

        // Send enable message to console with colored formatting
        Bukkit.getConsoleSender().sendMessage(Component.text()
            .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
            .append(Component.text("plugin enabled", TextColor.fromHexString("#52FFA6"))));
    }

    // Called when plugin is disabled
    @Override
    public void onDisable() {
        // Send disable message to console with colored formatting
        Bukkit.getConsoleSender().sendMessage(Component.text()
            .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
            .append(Component.text("nonchat disabled", TextColor.fromHexString("#FF5252"))));
    }

    // Register all plugin commands with their executors
    public void registerCommands() {
        // Initialize each command with required dependencies
        getCommand("message").setExecutor(new MessageCommand(this, pluginConfig, pluginMessages, spyCommand));
        getCommand("broadcast").setExecutor(new BroadcastCommand(pluginMessages, this));
        getCommand("server").setExecutor(new ServerCommand(pluginMessages, this));
        getCommand("nhelp").setExecutor(new NhelpCommand(pluginMessages, this));
        getCommand("nreload").setExecutor(new NreloadCommand(this, pluginMessages));
        getCommand("clear").setExecutor(new ClearCommand(pluginMessages, this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this, pluginMessages));
        getCommand("sc").setExecutor(new StaffChatCommand(this, pluginMessages, pluginConfig));
        getCommand("spy").setExecutor(new SpyCommand(this, pluginMessages, pluginConfig));
        getCommand("me").setExecutor(new MeCommand(this, pluginConfig, pluginMessages));
        getCommand("roll").setExecutor(new RollCommand(this, pluginConfig, pluginMessages));
    }

    // Register all event listeners
    public void registerListeners() {
        // Initialize listeners with required dependencies
        chatFormatListener = new ChatFormatListener(pluginConfig, pluginMessages);
        deathListener = new DeathListener(pluginConfig);
        deathCoordinates = new DeathCoordinates(pluginConfig);
        chatBubbleListener = new ChatBubbleListener(this, pluginConfig);

        // Register listeners with Bukkit plugin manager
        Bukkit.getPluginManager().registerEvents(chatFormatListener, this);
        Bukkit.getPluginManager().registerEvents(deathListener, this);
        Bukkit.getPluginManager().registerEvents(deathCoordinates, this);
        Bukkit.getPluginManager().registerEvents(chatBubbleListener, this);
    }

    // Initialize and register utility classes
    public void registerUtils() {
        // Initialize SpyCommand before other utils
        this.spyCommand = new SpyCommand(this, pluginMessages, pluginConfig);

        // Start auto broadcast system
        autoBroadcastSender = new AutoBroadcastSender(this, pluginConfig);
        autoBroadcastSender.start();

        // Initialize integration utils
        IntegrationUtil.setupIntegrations();
        
        // Initialize DiscordSRV support
        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
            this.discordManager = new DiscordManager(this);
            DiscordSRV.api.subscribe(new DiscordCommandHandler(this, pluginMessages));
        }

        // Initialize debugger if debug mode is enabled
        if (pluginConfig.isDebug()) {
            debugger = new Debugger(this);
            debugger.log("Plugin started successfully!");
        }
    }

    // Initialize configuration handlers
    public void registerConfigs() {
        this.pluginConfig = new PluginConfig();
        this.pluginMessages = new PluginMessages(this);
    }

    public void registerPlaceholders() {
        // Initialize placeholder API
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NonchatExpansion(this).register();
        }
    }

    public SpyCommand getSpyCommand() {
        return spyCommand;
    }
    
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public IgnoreManager getIgnoreManager() {
        return new IgnoreManager(ignoredPlayers);
    }

    public PluginMessages getPluginMessages() {
        return pluginMessages;
    }

    public HoverTextUtil getHoverTextUtil() {
        return pluginConfig.getHoverTextUtil();
    }
    
    // Reload plugin configuration files
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (pluginConfig != null) {
            pluginConfig.reloadConfig();
        }
        if (pluginMessages != null) {
            pluginMessages.reloadConfig();
        }
    }

    // Stop auto broadcast system
    public void stopAutoBroadcastSender() {
        autoBroadcastSender.stop();
    }

    // Debug logging methods
    public void log(String message) {
        if (debugger != null) {
            debugger.log(message);
        }
    }

    public void logCommand(String command, String[] args) {
        if (debugger != null) {
            debugger.log("Command: " + command + " Args: " + java.util.Arrays.toString(args));
        }
    }

    public void logResponse(String response) {
        if (debugger != null) {
            debugger.log("Response: " + response + TextColor.fromHexString("#52FFA6"));
        }
    }

    public void logError(String error) {
        if (debugger != null) {
            debugger.log("Error: " + error + TextColor.fromHexString("#FF5252"));
        }
    }

    // Reinitialize debugger based on config settings
    public void reloadDebugger() {
        if (pluginConfig.isDebug()) {
            if (debugger == null) {
                debugger = new Debugger(this);
            }
        } else {
            debugger = null;
        }
    }
}

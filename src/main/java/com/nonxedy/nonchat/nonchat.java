package com.nonxedy.nonchat;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.nonxedy.nonchat.api.ChannelAPI;
import com.nonxedy.nonchat.api.registry.FilterRegistry;
import com.nonxedy.nonchat.command.impl.IgnoreCommand;
import com.nonxedy.nonchat.command.impl.SpyCommand;
import com.nonxedy.nonchat.core.BroadcastManager;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.core.MessageManager;
import com.nonxedy.nonchat.hook.DiscordSRVHook;
import com.nonxedy.nonchat.integration.DiscordSRVIntegration;
import com.nonxedy.nonchat.listener.ChatListener;
import com.nonxedy.nonchat.listener.ChatListenerFactory;
import com.nonxedy.nonchat.listener.DeathCoordinates;
import com.nonxedy.nonchat.listener.DeathListener;
import com.nonxedy.nonchat.listener.DiscordSRVListener;
import com.nonxedy.nonchat.listener.JoinQuitListener;
import com.nonxedy.nonchat.placeholders.NonchatExpansion;
import com.nonxedy.nonchat.service.ChatService;
import com.nonxedy.nonchat.service.CommandService;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.Debugger;
import com.nonxedy.nonchat.util.Metrics;
import com.nonxedy.nonchat.util.UpdateChecker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class nonchat extends JavaPlugin {
    private ChatService chatService;
    private CommandService commandService;
    private ConfigService configService;
    private ChatManager chatManager;
    private MessageManager messageManager;
    private BroadcastManager broadcastManager;
    private SpyCommand spyCommand;
    private Debugger debugger;
    private ChatListener chatListener;
    private IgnoreCommand ignoreCommand;
    private DiscordSRVHook discordSRVHook;
    private DiscordSRVListener discordSRVListener;
    private DiscordSRVIntegration discordSRVIntegration;
    private Metrics metrics;
    private FilterRegistry filterRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("langs/messages_en.yml", false);
        saveResource("langs/messages_ru.yml", false);

        initializeServices();
        registerPlaceholders();
        registerListeners();
        initializeAnnotationSystems();
        setupIntegrations();

        Bukkit.getConsoleSender().sendMessage("§d[nonchat] §aplugin enabled");
    }

    private void initializeServices() {
        // First initialize configuration
        this.configService = new ConfigService(this);
        
        // Now that config is loaded, initialize the rest of the services
        this.spyCommand = new SpyCommand(this, configService.getMessages(), configService.getConfig());
        this.ignoreCommand = new IgnoreCommand(this, configService.getMessages());
        
        // Initialize core managers
        this.chatManager = new ChatManager(this, configService.getConfig(), configService.getMessages());
        this.messageManager = new MessageManager(configService.getConfig(), configService.getMessages(), spyCommand);
        this.broadcastManager = new BroadcastManager(this, configService.getConfig());
        
        // Set ignore command in managers
        this.chatManager.setIgnoreCommand(ignoreCommand);
        this.messageManager.setIgnoreCommand(ignoreCommand);
        
        // Initialize service layer that depends on managers
        this.chatService = new ChatService(chatManager, messageManager, broadcastManager, configService.getConfig());
        
        // Initialize command service last as it depends on all other services
        this.commandService = new CommandService(this, chatService, configService);
        
        // Initialize debug system if enabled
        if (configService.getConfig().isDebug()) {
            this.debugger = new Debugger(this);
            debugger.log("Services initialized successfully");
        }
    }

    private void registerListeners() {
        // Create and register chat listener
        this.chatListener = ChatListenerFactory.createChatListener(chatManager, chatService);
        getServer().getPluginManager().registerEvents(chatListener, this);
        
        // Register death-related listeners
        Bukkit.getPluginManager().registerEvents(new DeathListener(configService.getConfig()), this);
        Bukkit.getPluginManager().registerEvents(new DeathCoordinates(configService.getConfig()), this);
        
        // Register join/quit listener
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(configService.getConfig()), this);
        
        // Log successful listener registration
        if (debugger != null) {
            debugger.log("Event listeners registered successfully");
        }
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NonchatExpansion(this).register();
        }
    }

    private void setupIntegrations() {
        // Initialize metrics
        this.metrics = new Metrics(this, 25786);

        // Initialize DiscordSRV
        this.discordSRVHook = new DiscordSRVHook(this);
        ChannelAPI.initialize(this);
        
        // Initialize DiscordSRV listener
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
            this.discordSRVListener = new DiscordSRVListener(this);
            this.discordSRVIntegration = new DiscordSRVIntegration(this);
            getLogger().info("DiscordSRV integration enabled");
        }
        
        // Initialize update checker if enabled
        if (configService.getConfig().isUpdateCheckerEnabled()) {
            getLogger().info("Initializing update checker...");
            new UpdateChecker(this);
            if (debugger != null) {
                debugger.log("Update checker initialized");
            }
        }
    }

    @Override
    public void onDisable() {
        if (broadcastManager != null) {
            broadcastManager.stop();
        }
        
        if (commandService != null) {
            commandService.unregisterAll();
        }

        if (filterRegistry != null) {
            filterRegistry.clear();
        }

        if (discordSRVListener != null) {
            discordSRVListener.shutdown();
        }

        if (discordSRVIntegration != null) {
            discordSRVIntegration.unregister();
        }
        

        Bukkit.getConsoleSender().sendMessage(Component.text()
            .append(Component.text("[nonchat] ", TextColor.fromHexString("#E088FF")))
            .append(Component.text("plugin disabled", TextColor.fromHexString("#FF5252"))));
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        if (configService != null) {
            configService.reload();
        }

        if (broadcastManager != null) {
            broadcastManager.reload();
        }

        if (commandService != null) {
            commandService.reloadCommands();
        }
    }

    public void reloadServices() {
        // Stop services that need clean shutdown
        if (broadcastManager != null) {
            broadcastManager.stop();
        }
        
        // Reload configuration
        if (configService != null) {
            configService.reload();
        }
        
        // Reinitialize services that need it
        if (broadcastManager != null) {
            broadcastManager.reload();
        }
        
        // Reload commands and channels
        if (commandService != null) {
            commandService.reloadCommands();
        }
        
        if (chatManager != null) {
            chatManager.reloadChannels();
        }
        
        // Reinitialize debugger if needed
        if (configService != null && configService.getConfig().isDebug()) {
            if (debugger == null) {
                debugger = new Debugger(this);
            }
        } else {
            debugger = null;
        }
    }

    /**
     * Initializes the new annotation-based systems.
     */
    private void initializeAnnotationSystems() {
        logResponse("Initializing annotation-based systems...");
        
        // Initialize filter registry
        filterRegistry = new FilterRegistry(this);
    
        // Scan for filters and processors
        filterRegistry.scanAndRegister("com.nonxedy.nonchat.filter");
        filterRegistry.scanAndRegister("com.nonxedy.nonchat.processor");
    
        // Register annotated commands
        registerAnnotatedCommands();
    
        logResponse("Annotation-based systems initialized successfully");
    }

    public SpyCommand getSpyCommand() {
        return spyCommand;
    }
    
    public IgnoreCommand getIgnoreCommand() {
        return ignoreCommand;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    /**
     * Checks if debug mode is enabled.
     * 
     * @return true if debug is enabled
     */
    public boolean isDebugEnabled() {
        return debugger != null && configService != null && 
               configService.getConfig().getBoolean("debug.enabled", false);
    }

    public void logCommand(String command, String[] args) {
        if (debugger != null) {
            debugger.log("Command: " + command + " Args: " + Arrays.toString(args));
        }
    }

    public void logResponse(String response) {
        if (debugger != null) {
            debugger.log("Response: " + response);
        }
    }

    public void logError(String error) {
        if (debugger != null) {
            debugger.log("Error: " + error);
        }
    }

    public void logPlaceholder(String placeholder, String result) {
        if (debugger != null) {
            debugger.log("Placeholder: " + placeholder + " -> " + result);
        }
    }

    // -----------------------------------------------------------------------------------------

    public ChatManager getChatManager() {
        return chatManager;
    }

    public FilterRegistry getFilterRegistry() {
        return filterRegistry;
    }

    public CommandService getCommandService() {
        return commandService;
    }
}

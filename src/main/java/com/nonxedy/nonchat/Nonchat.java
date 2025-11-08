package com.nonxedy.nonchat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.nonxedy.nonchat.api.ChannelAPI;
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
import com.nonxedy.nonchat.placeholders.impl.ConfigurablePlaceholder;
import com.nonxedy.nonchat.service.ChatService;
import com.nonxedy.nonchat.service.CommandService;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.InteractivePlaceholderManager;
import com.nonxedy.nonchat.util.chat.filters.LinkDetector;
import com.nonxedy.nonchat.util.chat.packets.DisplayEntityUtil;
import com.nonxedy.nonchat.util.core.debugging.Debugger;
import com.nonxedy.nonchat.util.core.updates.UpdateChecker;
import com.nonxedy.nonchat.util.integration.external.IntegrationUtil;
import com.nonxedy.nonchat.util.integration.metrics.Metrics;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

@Slf4j
public class Nonchat extends JavaPlugin {

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
    private InteractivePlaceholderManager placeholderManager;
    private final Map<Player, List<TextDisplay>> bubbles = new HashMap<>();

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            
            // Only save language files if they don't exist (preserve user changes)
            if (!new File(getDataFolder(), "langs/messages_en.yml").exists()) {
                saveResource("langs/messages_en.yml", false);
            }
            if (!new File(getDataFolder(), "langs/messages_ru.yml").exists()) {
                saveResource("langs/messages_ru.yml", false);
            }
            if (!new File(getDataFolder(), "langs/messages_es.yml").exists()) {
                saveResource("langs/messages_es.yml", false);
            }

            initializeServices();
            registerPlaceholders();
            registerListeners();
            setupIntegrations();

            Bukkit.getConsoleSender().sendMessage("§d[nonchat] §aplugin enabled");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable plugin: {0}", e.getMessage());
             throw new RuntimeException("Failed to enable plugin", e);
        }
    }

    private void initializeServices() {
        try {
            // First initialize configuration
            this.configService = new ConfigService(this);

            // Now that config is loaded, initialize the rest of the services
            this.spyCommand = new SpyCommand(this, configService.getMessages(), configService.getConfig());
            this.ignoreCommand = new IgnoreCommand(this, configService.getMessages());

            // Initialize core managers
            this.chatManager = new ChatManager(this, configService.getConfig(), configService.getMessages());
            this.messageManager = new MessageManager(this, configService.getConfig(), configService.getMessages(), spyCommand);
            this.broadcastManager = new BroadcastManager(this, configService.getConfig());

            // Set ignore command in managers
            this.chatManager.setIgnoreCommand(ignoreCommand);
            this.messageManager.setIgnoreCommand(ignoreCommand);

            // Initialize LinkDetector with translation support before creating commands
            LinkDetector.initialize(configService.getMessages());

            // Initialize service layer that depends on managers
            this.chatService = new ChatService(chatManager, messageManager, broadcastManager, configService.getConfig());

            // Initialize command service last as it depends on all other services
            this.commandService = new CommandService(this, chatService, configService);

            // Initialize interactive placeholder manager
            initializeInteractivePlaceholders();

            // Initialize debug system if enabled
            if (configService.getConfig().isDebug()) {
                this.debugger = new Debugger(this, configService.getConfig().getDebugLogRetentionDays());
                debugger.info("Core", "Services initialized successfully");
            }

            getLogger().info("Core services initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core services: {0}", e.getMessage());
            throw new RuntimeException("Failed to initialize core services", e);
        }
    }

    private void initializeInteractivePlaceholders() {
        try {
            this.placeholderManager = new InteractivePlaceholderManager();

            // Register built-in placeholders
            registerBuiltInPlaceholders();

            // Load custom placeholders from config
            loadCustomPlaceholdersFromConfig();

            getLogger().info("Interactive placeholders initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize interactive placeholders: {0}", e.getMessage());
            throw new RuntimeException("Failed to initialize interactive placeholders", e);
        }
    }

    private void registerBuiltInPlaceholders() {
        // Built-in placeholders are now loaded from config in loadCustomPlaceholdersFromConfig()
        getLogger().info("Built-in interactive placeholders will be loaded from config");
    }

    private void loadCustomPlaceholdersFromConfig() {
        try {
            // Load custom placeholders from config
            ConfigurationSection customPlaceholdersSection =
                getConfig().getConfigurationSection("interactive-placeholders.custom-placeholders");

            if (customPlaceholdersSection != null) {
                int loadedCount = 0;
                for (String placeholderKey : customPlaceholdersSection.getKeys(false)) {
                    ConfigurationSection placeholderConfig =
                        customPlaceholdersSection.getConfigurationSection(placeholderKey);

                    if (placeholderConfig != null && placeholderConfig.getBoolean("enabled", false)) {
                        String displayName = placeholderConfig.getString("display-name", placeholderKey);
                        String description = placeholderConfig.getString("description", "");
                        String permission = placeholderConfig.getString("permission", "");
                        String activationKey = placeholderConfig.getString("placeholder", placeholderKey);
                        String format = placeholderConfig.getString("format", "[" + activationKey + "]");
                        List<String> hoverText = placeholderConfig.getStringList("hover-text");

                        // Click action
                        String clickActionType = "none";
                        String clickActionValue = "";
                        ConfigurationSection clickActionSection =
                            placeholderConfig.getConfigurationSection("click-action");
                        if (clickActionSection != null) {
                            clickActionType = clickActionSection.getString("type", "none");
                            clickActionValue = clickActionSection.getString("value", "");
                        }

                        // Create and register the placeholder
                        ConfigurablePlaceholder placeholder = new ConfigurablePlaceholder(
                            placeholderKey,
                            activationKey,
                            displayName,
                            description,
                            true, // Already checked enabled above
                            permission,
                            format,
                            hoverText,
                            clickActionType,
                            clickActionValue
                        );

                        placeholderManager.registerPlaceholder(placeholder);
                        loadedCount++;
                    }
                }
                getLogger().info("Loaded " + loadedCount + " custom interactive placeholders from config");
            } else {
                getLogger().info("No custom placeholders section found in config");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to load custom placeholders from config: {0}", e.getMessage());
        }
    }

    private void reloadInteractivePlaceholders() {
        try {
            if (placeholderManager != null) {
                // Clear existing placeholders
                placeholderManager.clearPlaceholders();

                // Reload from config
                loadCustomPlaceholdersFromConfig();

                getLogger().info("Interactive placeholders reloaded successfully");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to reload interactive placeholders: {0}", e.getMessage());
        }
    }

    private void registerListeners() {
        try {
            // Register chat listener via factory
            ChatListener chatListener = ChatListenerFactory.createChatListener(this, chatManager, chatService);
            getServer().getPluginManager().registerEvents(chatListener, this);

            // Register death-related listeners
            Bukkit.getPluginManager().registerEvents(new DeathListener(configService.getConfig()), this);
            Bukkit.getPluginManager().registerEvents(new DeathCoordinates(configService.getConfig(), configService.getMessages()), this);

            // Register join/quit listener
            Bukkit.getPluginManager().registerEvents(new JoinQuitListener(configService.getConfig(), chatManager.getChannelManager()), this);

            // Log successful listener registration
            if (debugger != null) {
                debugger.info("Events", "Event listeners registered successfully");
            }
            getLogger().info("Event listeners registered successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register event listeners: {0}", e.getMessage());
            throw new RuntimeException("Failed to register event listeners", e);
        }
    }

    private void registerPlaceholders() {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new NonchatExpansion(this).register();
                getLogger().info("PlaceholderAPI expansion registered successfully");
            } else {
                getLogger().info("PlaceholderAPI not available - placeholders will be disabled");
            }
        } catch (NoClassDefFoundError e) {
            getLogger().info("PlaceholderAPI not installed - placeholders will be disabled");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to register PlaceholderAPI expansion: {0}", e.getMessage());
        }
    }

    private void setupIntegrations() {
        try {
            // Initialize external integrations (PlaceholderAPI, LuckPerms, Vault)
            IntegrationUtil.setupIntegrations();
        } catch (NoClassDefFoundError e) {
            getLogger().info("Some external integrations not available - features will be disabled");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to setup external integrations: {0}", e.getMessage());
        }

        try {
            // Initialize metrics
            this.metrics = new Metrics(this, 25786);
            getLogger().info("Metrics initialized successfully");
        } catch (NoClassDefFoundError e) {
            getLogger().info("Metrics not available - metrics will be disabled");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize metrics: {0}", e.getMessage());
        }

        try {
            // Initialize DiscordSRV
            this.discordSRVHook = new DiscordSRVHook(this);
            ChannelAPI.initialize(this);

            // Initialize DiscordSRV listener
            if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
                this.discordSRVListener = new DiscordSRVListener(this);
                this.discordSRVIntegration = new DiscordSRVIntegration(this);
                getLogger().info("DiscordSRV integration enabled");
            } else {
                getLogger().info("DiscordSRV not available - Discord integration will be disabled");
            }
        } catch (NoClassDefFoundError e) {
            getLogger().info("DiscordSRV not installed - Discord integration will be disabled");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to setup DiscordSRV integration: {0}", e.getMessage());
        }

        try {
            // Initialize update checker if enabled
            if (configService.getConfig().isUpdateCheckerEnabled()) {
                getLogger().info("Initializing update checker...");
                new UpdateChecker(this);
                if (debugger != null) {
                    debugger.info("Updates", "Update checker initialized");
                }
                getLogger().info("Update checker initialized successfully");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize update checker: {0}", e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
            // Clean up all chat bubbles and display entities
            if (chatManager != null) {
                chatManager.cleanup();
            }
            
            // Clear display entity pool
            DisplayEntityUtil.clearPool();
            
            if (broadcastManager != null) {
                broadcastManager.stop();
            }

            if (commandService != null) {
                commandService.unregisterAll();
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
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during plugin shutdown: {0}", e.getMessage());
        }
    }

    @Override
    public void reloadConfig() {
        try {
            super.reloadConfig();

            if (configService != null) {
                configService.reload();
            }

            if (broadcastManager != null) {
                broadcastManager.reload();
            }

            // Reinitialize LinkDetector with updated messages before reloading commands
            if (configService != null) {
                LinkDetector.initialize(configService.getMessages());
            }

            if (commandService != null) {
                commandService.reloadCommands();
            }

            // Reload interactive placeholders
            reloadInteractivePlaceholders();

            getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload configuration: {0}", e.getMessage());
            throw new RuntimeException("Failed to reload configuration", e);
        }
    }

    public void reloadServices() {
        try {
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

            // Reinitialize LinkDetector with updated messages before reloading commands
            if (configService != null) {
                LinkDetector.initialize(configService.getMessages());
            }

            // Reload commands and channels after LinkDetector is reinitialized
            if (commandService != null) {
                commandService.reloadCommands();
            }

            if (chatManager != null) {
                chatManager.reloadChannels();
            }

            // Reload interactive placeholders
            reloadInteractivePlaceholders();

            // Reinitialize debugger if needed
            if (configService != null && configService.getConfig().isDebug()) {
                if (debugger == null) {
                    debugger = new Debugger(this, configService.getConfig().getDebugLogRetentionDays());
                }
            } else {
                debugger = null;
            }
            
            getLogger().info("Services reloaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload services: {0}", e.getMessage());
            throw new RuntimeException("Failed to reload services", e);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        try {
            List<TextDisplay> playerBubbles = bubbles.remove(event.getPlayer());
            if (playerBubbles != null) {
                DisplayEntityUtil.removeBubbles(playerBubbles);
            }
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Error handling player teleport: {0}", e.getMessage());
        }
    }

    public void logCommand(String command, String[] args) {
        try {
            if (debugger != null) {
                debugger.debug("Commands", command + " Args: " + Arrays.toString(args));
            }
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Failed to log command: {0}", e.getMessage());
        }
    }

    public void logResponse(String response) {
        try {
            if (debugger != null) {
                debugger.debug("API", "Response: " + response);
            }
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Failed to log response: {0}", e.getMessage());
        }
    }

    public void logError(String error) {
        try {
            if (debugger != null) {
                debugger.error("System", "Error occurred", new Exception(error));
            }
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Failed to log error: {0}", e.getMessage());
        }
    }

    public void logPlaceholder(String placeholder, String result) {
        try {
            if (debugger != null) {
                debugger.debug("Placeholders", placeholder + " -> " + result);
            }
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Failed to log placeholder: {0}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------------
    public ChatManager getChatManager() {
        return chatManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
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

    public InteractivePlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }
}

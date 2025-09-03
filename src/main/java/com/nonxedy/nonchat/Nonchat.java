package com.nonxedy.nonchat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
import com.nonxedy.nonchat.service.ChatService;
import com.nonxedy.nonchat.service.CommandService;
import com.nonxedy.nonchat.service.ConfigService;
import com.nonxedy.nonchat.util.chat.filters.LinkDetector;
import com.nonxedy.nonchat.util.chat.packets.BubblePacketUtil;
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
    private final Map<Player, List<ArmorStand>> bubbles = new HashMap<>();

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

            initializeServices();
            registerPlaceholders();
            registerListeners();
            setupIntegrations();

            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (entity instanceof ArmorStand) {
                                entity.remove();
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to clean up armor stands: " + e.getMessage());
                }
            }, 20);

            Bukkit.getConsoleSender().sendMessage("§d[nonchat] §aplugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to enable plugin: " + e.getMessage());
            e.printStackTrace();
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

            // Initialize service layer that depends on managers
            this.chatService = new ChatService(chatManager, messageManager, broadcastManager, configService.getConfig());

            // Initialize command service last as it depends on all other services
            this.commandService = new CommandService(this, chatService, configService);

            // Initialize LinkDetector with translation support
            LinkDetector.initialize(configService.getMessages());

            // Initialize debug system if enabled
            if (configService.getConfig().isDebug()) {
                this.debugger = new Debugger(this, configService.getConfig().getDebugLogRetentionDays());
                debugger.info("Core", "Services initialized successfully");
            }
            
            getLogger().info("Core services initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize core services: " + e.getMessage());
            throw new RuntimeException("Failed to initialize core services", e);
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
            getLogger().severe("Failed to register event listeners: " + e.getMessage());
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
            getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }
    }

    private void setupIntegrations() {
        try {
            // Initialize external integrations (PlaceholderAPI, LuckPerms, Vault)
            IntegrationUtil.setupIntegrations();
        } catch (NoClassDefFoundError e) {
            getLogger().info("Some external integrations not available - features will be disabled");
        } catch (Exception e) {
            getLogger().warning("Failed to setup external integrations: " + e.getMessage());
        }

        try {
            // Initialize metrics
            this.metrics = new Metrics(this, 25786);
            getLogger().info("Metrics initialized successfully");
        } catch (NoClassDefFoundError e) {
            getLogger().info("Metrics not available - metrics will be disabled");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize metrics: " + e.getMessage());
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
            getLogger().warning("Failed to setup DiscordSRV integration: " + e.getMessage());
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
            getLogger().warning("Failed to initialize update checker: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
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
            getLogger().warning("Error during plugin shutdown: " + e.getMessage());
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

            if (commandService != null) {
                commandService.reloadCommands();
            }
            
            getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to reload configuration: " + e.getMessage());
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

            // Reload commands and channels
            if (commandService != null) {
                commandService.reloadCommands();
            }

            if (chatManager != null) {
                chatManager.reloadChannels();
            }

            // Reinitialize LinkDetector with updated messages
            if (configService != null) {
                LinkDetector.initialize(configService.getMessages());
            }

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
            getLogger().severe("Failed to reload services: " + e.getMessage());
            throw new RuntimeException("Failed to reload services", e);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        try {
            List<ArmorStand> playerBubbles = bubbles.remove(event.getPlayer());
            if (playerBubbles != null) {
                BubblePacketUtil.removeBubbles(playerBubbles);
            }
        } catch (Exception e) {
            getLogger().fine("Error handling player teleport: " + e.getMessage());
        }
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

    public void logCommand(String command, String[] args) {
        try {
            if (debugger != null) {
                debugger.debug("Commands", command + " Args: " + Arrays.toString(args));
            }
        } catch (Exception e) {
            getLogger().fine("Failed to log command: " + e.getMessage());
        }
    }

    public void logResponse(String response) {
        try {
            if (debugger != null) {
                debugger.debug("API", "Response: " + response);
            }
        } catch (Exception e) {
            getLogger().fine("Failed to log response: " + e.getMessage());
        }
    }

    public void logError(String error) {
        try {
            if (debugger != null) {
                debugger.error("System", "Error occurred", new Exception(error));
            }
        } catch (Exception e) {
            getLogger().fine("Failed to log error: " + e.getMessage());
        }
    }

    public void logPlaceholder(String placeholder, String result) {
        try {
            if (debugger != null) {
                debugger.debug("Placeholders", placeholder + " -> " + result);
            }
        } catch (Exception e) {
            getLogger().fine("Failed to log placeholder: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------------
    public ChatManager getChatManager() {
        return chatManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}

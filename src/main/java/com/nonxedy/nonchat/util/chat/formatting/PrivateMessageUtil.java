package com.nonxedy.nonchat.util.chat.formatting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.bukkit.entity.Player;

import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;
import com.nonxedy.nonchat.util.integration.external.IntegrationUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Utility class for handling private message formatting with hover effects
 * Supports separate sender and receiver formats with interactive hover text
 */
public class PrivateMessageUtil {

    /**
     * Creates a formatted private message component for the sender
     * 
     * @param config Plugin configuration
     * @param sender Message sender
     * @param target Message recipient  
     * @param message The message content
     * @return Formatted component with hover effects
     */
    public static Component createSenderMessage(PluginConfig config, Player sender, Player target, String message) {
        String format = config.getPrivateChatSenderFormat();
        String senderName = sender != null ? sender.getName() : "Console";
        
        // Replace placeholders in format
        String formattedMessage = format
            .replace("{sender}", senderName)
            .replace("{receiver}", target.getName())  
            .replace("{message}", message);
            
        // Process PlaceholderAPI if available
        if (sender != null) {
            formattedMessage = IntegrationUtil.processPlaceholders(sender, formattedMessage);
        }
        
        // Parse colors and create base component
        Component baseComponent = ColorUtil.parseComponent(formattedMessage);
        
        // Add hover and click events if enabled
        if (config.isPrivateChatSenderHoverEnabled()) {
            baseComponent = addSenderInteractivity(config, baseComponent, sender, target);
        }
        
        return baseComponent;
    }
    
    /**
     * Creates a formatted private message component for the receiver
     * 
     * @param config Plugin configuration
     * @param sender Message sender
     * @param target Message recipient
     * @param message The message content
     * @return Formatted component with hover effects
     */
    public static Component createReceiverMessage(PluginConfig config, Player sender, Player target, String message) {
        String format = config.getPrivateChatReceiverFormat();
        String senderName = sender != null ? sender.getName() : "Console";
        
        // Replace placeholders in format
        String formattedMessage = format
            .replace("{sender}", senderName)
            .replace("{receiver}", target.getName())
            .replace("{message}", message);
            
        // Process PlaceholderAPI for target player
        formattedMessage = IntegrationUtil.processPlaceholders(target, formattedMessage);
        
        // Parse colors and create base component
        Component baseComponent = ColorUtil.parseComponent(formattedMessage);
        
        // Add hover and click events if enabled
        if (config.isPrivateChatReceiverHoverEnabled()) {
            baseComponent = addReceiverInteractivity(config, baseComponent, sender, target);
        }
        
        return baseComponent;
    }
    
    /**
     * Adds hover text and click events for sender message
     * 
     * @param config Plugin configuration
     * @param component Base component to add interactivity to
     * @param sender Message sender
     * @param target Message recipient
     * @return Component with added hover and click events
     */
    private static Component addSenderInteractivity(PluginConfig config, Component component, Player sender, Player target) {
        List<String> hoverLines = config.getPrivateChatSenderHover();
        
        if (hoverLines.isEmpty()) {
            return component;
        }
        
        // Build hover text
        Component hoverComponent = buildHoverText(hoverLines, sender, target);
        
        // Add hover event
        Component resultComponent = component.hoverEvent(HoverEvent.showText(hoverComponent));
        
        // Add click event based on configuration
        if (config.isPrivateChatClickActionsEnabled()) {
            String clickCommand = config.getPrivateChatReplyCommand()
                .replace("{sender}", sender != null ? sender.getName() : "Console")
                .replace("{receiver}", target.getName());
            ClickEvent clickEvent = ClickEvent.suggestCommand(clickCommand);
            resultComponent = resultComponent.clickEvent(clickEvent);
        }
        
        return resultComponent;
    }
    
    /**
     * Adds hover text and click events for receiver message
     * 
     * @param config Plugin configuration
     * @param component Base component to add interactivity to
     * @param sender Message sender
     * @param target Message recipient
     * @return Component with added hover and click events
     */
    private static Component addReceiverInteractivity(PluginConfig config, Component component, Player sender, Player target) {
        List<String> hoverLines = config.getPrivateChatReceiverHover();
        
        if (hoverLines.isEmpty()) {
            return component;
        }
        
        // Build hover text
        Component hoverComponent = buildHoverText(hoverLines, sender, target);
        
        // Add hover event
        Component resultComponent = component.hoverEvent(HoverEvent.showText(hoverComponent));
        
        // Add click event based on configuration
        if (config.isPrivateChatClickActionsEnabled()) {
            String clickCommand = config.getPrivateChatReplyCommand()
                .replace("{sender}", sender != null ? sender.getName() : "Console")
                .replace("{receiver}", target.getName());
            
            ClickEvent clickEvent = ClickEvent.suggestCommand(clickCommand);
            resultComponent = resultComponent.clickEvent(clickEvent);
        }
        
        return resultComponent;
    }
    
    /**
     * Builds hover text component from configuration lines
     * 
     * @param hoverLines List of hover text lines from config
     * @param sender Message sender
     * @param target Message recipient
     * @return Formatted hover text component
     */
    private static Component buildHoverText(List<String> hoverLines, Player sender, Player target) {
        TextComponent.Builder builder = Component.text();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        for (int i = 0; i < hoverLines.size(); i++) {
            String line = hoverLines.get(i);
            String senderName = sender != null ? sender.getName() : "Console";
            
            // Replace placeholders
            line = line
                .replace("{sender}", senderName)
                .replace("{receiver}", target.getName())
                .replace("{time}", currentTime);
                
            // Process PlaceholderAPI if available
            if (sender != null) {
                line = IntegrationUtil.processPlaceholders(sender, line);
            } else {
                // If sender is Console, process placeholders for the target (receiver)
                line = IntegrationUtil.processPlaceholders(target, line);
            }
            
            // Parse colors and add to hover text
            Component lineComponent = ColorUtil.parseComponent(line);
            builder.append(lineComponent);
            
            // Add newline if not the last line
            if (i < hoverLines.size() - 1) {
                builder.append(Component.newline());
            }
        }
        
        return builder.build();
    }
}
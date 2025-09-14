package com.nonxedy.nonchat.util.chat.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nonxedy.nonchat.config.PluginMessages;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Utility class for detecting and converting URLs in text to clickable links
 */
public class LinkDetector {
    // Pattern to match URLs in text
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b((?:https?://|www\\.)?\\S+\\.[a-z]{2,}\\S*)",
            Pattern.CASE_INSENSITIVE);
    
    // Static reference to messages for translation
    private static PluginMessages messages;

    /**
     * Initializes the LinkDetector with the messages system
     * @param messages The PluginMessages instance for translations
     */
    public static void initialize(PluginMessages messages) {
        LinkDetector.messages = messages;
    }

    /**
     * Converts plain text with URLs into a Component with clickable links
     * @param text The text that may contain URLs
     * @return Component with clickable links
     */
    public static Component makeLinksClickable(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Find all URLs in the text
        Matcher matcher = URL_PATTERN.matcher(text);
        
        // If no URLs found, just return the text as a component
        if (!matcher.find()) {
            return ColorUtil.parseComponent(text);
        }
        
        // Reset matcher to start from beginning
        matcher.reset();
        
        // Build the component with clickable links
        TextComponent.Builder builder = Component.text();
        int lastEnd = 0;
        
        while (matcher.find()) {
            // Add text before the URL
            String beforeUrl = text.substring(lastEnd, matcher.start());
            if (!beforeUrl.isEmpty()) {
                builder.append(ColorUtil.parseComponent(beforeUrl));
            }
            
            // Get the URL
            String url = matcher.group();
            
            // Create clickable link component
            Component linkComponent = createLinkComponent(url);
            
            builder.append(linkComponent);
            
            lastEnd = matcher.end();
        }
        
        // Add any remaining text after the last URL
        String afterLastUrl = text.substring(lastEnd);
        if (!afterLastUrl.isEmpty()) {
            builder.append(ColorUtil.parseComponent(afterLastUrl));
        }
        
        return builder.build();
    }

    private static Component createLinkComponent(String url) {
        String clickableUrl = url;
        if (url.toLowerCase().startsWith("www.")) {
            clickableUrl = "https://" + url;
        } else if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            clickableUrl = "https://" + url;
        }

        // Use translated message if available, fallback to hardcoded text
        Component hoverComponent;
        if (messages != null) {
            String hoverText = messages.getString("link-hover").replace("{url}", clickableUrl);
            hoverComponent = ColorUtil.parseComponent(hoverText);
        } else {
            hoverComponent = Component.text("Click to open: " + clickableUrl);
        }

        return Component.text(url)
                .clickEvent(ClickEvent.openUrl(clickableUrl))
                .hoverEvent(HoverEvent.showText(hoverComponent))
                .decoration(TextDecoration.UNDERLINED, true);
    }
}

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

        // Strip color codes from the text to avoid interference with URL detection
        String cleanText = ColorUtil.stripAllColors(text);

        // Find all URLs in the cleaned text
        Matcher matcher = URL_PATTERN.matcher(cleanText);

        // If no URLs found, just return the original text as a component
        if (!matcher.find()) {
            return ColorUtil.parseComponent(text);
        }

        // Reset matcher to start from beginning
        matcher.reset();

        // Build the component with clickable links
        TextComponent.Builder builder = Component.text();
        int lastEnd = 0;
        int originalLastEnd = 0;

        while (matcher.find()) {
            // Get the URL from cleaned text (without color codes)
            String cleanUrl = cleanText.substring(matcher.start(), matcher.end());

            // Find this URL in the original text starting from where we left off
            int originalUrlStart = text.indexOf(cleanUrl, originalLastEnd);
            if (originalUrlStart == -1) {
                // Fallback: use the clean URL if we can't find it in original
                originalUrlStart = lastEnd;
            }
            int originalUrlEnd = originalUrlStart + cleanUrl.length();

            // Add text before the URL (from original text with colors preserved)
            String beforeUrl = text.substring(originalLastEnd, originalUrlStart);
            if (!beforeUrl.isEmpty()) {
                builder.append(ColorUtil.parseComponent(beforeUrl));
            }

            // Create clickable link component using the clean URL
            Component linkComponent = createLinkComponent(cleanUrl);

            builder.append(linkComponent);

            lastEnd = matcher.end();
            originalLastEnd = originalUrlEnd;
        }

        // Add any remaining text after the last URL
        String afterLastUrl = text.substring(originalLastEnd);
        if (!afterLastUrl.isEmpty()) {
            builder.append(ColorUtil.parseComponent(afterLastUrl));
        }
        
        return builder.build();
    }

    private static Component createLinkComponent(String url) {
        // URL is already cleaned of color codes
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

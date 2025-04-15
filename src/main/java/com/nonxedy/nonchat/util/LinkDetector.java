package com.nonxedy.nonchat.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            "(?i)\\b((?:https?://|www\\.)\\S+\\.[a-z]{2,}\\S*)",
            Pattern.CASE_INSENSITIVE);

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
            String clickableUrl = url;
            
            // Ensure URL has proper protocol for clicking
            if (url.toLowerCase().startsWith("www.")) {
                clickableUrl = "https://" + url;
            }
            
            // Create clickable link component
            Component linkComponent = ColorUtil.parseComponent("&#3498db" + url)
                    .clickEvent(ClickEvent.openUrl(clickableUrl))
                    .hoverEvent(HoverEvent.showText(ColorUtil.parseComponent("&#2ecc71Click to open: &#ffffff" + clickableUrl)))
                    .decoration(TextDecoration.UNDERLINED, true);
            
            builder.append(linkComponent);
            
            lastEnd = matcher.end();
        }
        
        // Add any remaining text after the last URL
        if (lastEnd < text.length()) {
            builder.append(ColorUtil.parseComponent(text.substring(lastEnd)));
        }
        
        return builder.build();
    }

    /**
     * Checks if the given text contains any URLs
     * @param text The text to check
     * @return true if the text contains URLs, false otherwise
     */
    public static boolean containsLinks(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(text).find();
    }
    
    /**
     * Extracts all URLs from the given text
     * @param text The text to extract URLs from
     * @return List of URLs found in the text
     */
    public static List<String> extractLinks(String text) {
        List<String> links = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return links;
        }
        
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            links.add(matcher.group());
        }
        
        return links;
    }
}

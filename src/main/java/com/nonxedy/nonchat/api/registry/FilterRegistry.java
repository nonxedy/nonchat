package com.nonxedy.nonchat.api.registry;

import com.nonxedy.nonchat.api.MessageFilter;
import com.nonxedy.nonchat.api.MessageProcessor;
import com.nonxedy.nonchat.api.annotation.ChatFilter;
import com.nonxedy.nonchat.api.annotation.ChatProcessor;
import com.nonxedy.nonchat.api.util.AnnotationUtil;
import com.nonxedy.nonchat.nonchat;

import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for chat filters and processors.
 * Manages automatic registration and execution of annotated filters and processors.
 */
public class FilterRegistry {
    
    private final nonchat plugin;
    private final Map<String, List<FilterInfo>> channelFilters = new HashMap<>();
    private final Map<String, List<ProcessorInfo>> channelProcessors = new HashMap<>();
    
    /**
     * Creates new filter registry.
     * 
     * @param plugin Plugin instance
     */
    public FilterRegistry(nonchat plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Scans and registers filters and processors from package.
     * 
     * @param packageName Package to scan
     */
    public void scanAndRegister(String packageName) {
        plugin.logResponse("Scanning for filters and processors in package: " + packageName);
        
        try {
            Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
            
            // Register filters
            Set<Class<?>> filterClasses = reflections.getTypesAnnotatedWith(ChatFilter.class);
            registerFilters(filterClasses);
            
            // Register processors
            Set<Class<?>> processorClasses = reflections.getTypesAnnotatedWith(ChatProcessor.class);
            registerProcessors(processorClasses);
            
        } catch (Exception e) {
            plugin.logError("Failed to scan package " + packageName + ": " + e.getMessage());
            if (plugin.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Registers filter classes.
     */
    private void registerFilters(Set<Class<?>> filterClasses) {
        int registered = 0;
        
        for (Class<?> filterClass : filterClasses) {
            try {
                if (!MessageFilter.class.isAssignableFrom(filterClass)) {
                    plugin.logError("Filter class " + filterClass.getName() + " does not implement MessageFilter");
                    continue;
                }
                
                ChatFilter annotation = filterClass.getAnnotation(ChatFilter.class);
                List<String> errors = AnnotationUtil.validateChatFilter(annotation);
                if (!errors.isEmpty()) {
                    plugin.logError("Invalid filter annotation in " + filterClass.getName() + ": " + String.join(", ", errors));
                    continue;
                }
                
                MessageFilter filter = createFilterInstance(filterClass);
                if (filter != null) {
                    registerFilter(filter, annotation);
                    registered++;
                }
                
            } catch (Exception e) {
                plugin.logError("Failed to register filter " + filterClass.getName() + ": " + e.getMessage());
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        }
        
        plugin.logResponse("Registered " + registered + " chat filters");
    }
    
    /**
     * Registers processor classes.
     */
    private void registerProcessors(Set<Class<?>> processorClasses) {
        int registered = 0;
        
        for (Class<?> processorClass : processorClasses) {
            try {
                if (!MessageProcessor.class.isAssignableFrom(processorClass)) {
                    plugin.logError("Processor class " + processorClass.getName() + " does not implement MessageProcessor");
                    continue;
                }
                
                ChatProcessor annotation = processorClass.getAnnotation(ChatProcessor.class);
                List<String> errors = AnnotationUtil.validateChatProcessor(annotation);
                if (!errors.isEmpty()) {
                    plugin.logError("Invalid processor annotation in " + processorClass.getName() + ": " + String.join(", ", errors));
                    continue;
                }
                
                MessageProcessor processor = createProcessorInstance(processorClass);
                if (processor != null) {
                    registerProcessor(processor, annotation);
                    registered++;
                }
                
            } catch (Exception e) {
                plugin.logError("Failed to register processor " + processorClass.getName() + ": " + e.getMessage());
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        }
        
        plugin.logResponse("Registered " + registered + " chat processors");
    }
    
    /**
     * Creates filter instance.
     */
    @SuppressWarnings("unchecked")
    private MessageFilter createFilterInstance(Class<?> filterClass) {
        try {
            // Try constructor with plugin parameter
            try {
                Constructor<?> pluginConstructor = filterClass.getConstructor(nonchat.class);
                return (MessageFilter) pluginConstructor.newInstance(plugin);
            } catch (NoSuchMethodException ignored) {}
            
            // Try default constructor
            try {
                Constructor<?> defaultConstructor = filterClass.getConstructor();
                return (MessageFilter) defaultConstructor.newInstance();
            } catch (NoSuchMethodException ignored) {}
            
            plugin.logError("No suitable constructor found for filter class: " + filterClass.getName());
            return null;
            
        } catch (Exception e) {
            plugin.logError("Failed to create filter instance: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates processor instance.
     */
    @SuppressWarnings("unchecked")
    private MessageProcessor createProcessorInstance(Class<?> processorClass) {
        try {
            // Try constructor with plugin parameter
            try {
                Constructor<?> pluginConstructor = processorClass.getConstructor(nonchat.class);
                return (MessageProcessor) pluginConstructor.newInstance(plugin);
            } catch (NoSuchMethodException ignored) {}
            
            // Try default constructor
            try {
                Constructor<?> defaultConstructor = processorClass.getConstructor();
                return (MessageProcessor) defaultConstructor.newInstance();
            } catch (NoSuchMethodException ignored) {}
            
            plugin.logError("No suitable constructor found for processor class: " + processorClass.getName());
            return null;
            
        } catch (Exception e) {
            plugin.logError("Failed to create processor instance: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Registers filter with annotation data.
     */
    private void registerFilter(MessageFilter filter, ChatFilter annotation) {
        FilterInfo filterInfo = new FilterInfo(filter, annotation);
        
        String[] channels = annotation.channels();
        if (channels.length == 0) {
            // Register for all channels
            channelFilters.computeIfAbsent("*", k -> new ArrayList<>()).add(filterInfo);
        } else {
            // Register for specific channels
            for (String channel : channels) {
                channelFilters.computeIfAbsent(channel.toLowerCase(), k -> new ArrayList<>()).add(filterInfo);
            }
        }
        
        // Sort by priority
        channelFilters.values().forEach(list -> 
            list.sort(Comparator.comparingInt(f -> f.annotation.priority())));
        
        plugin.logResponse("Registered filter: " + filter.getName() + 
                         " for channels: " + (channels.length == 0 ? "all" : String.join(", ", channels)));
    }
    
    /**
     * Registers processor with annotation data.
     */
    private void registerProcessor(MessageProcessor processor, ChatProcessor annotation) {
        ProcessorInfo processorInfo = new ProcessorInfo(processor, annotation);
        
        String[] channels = annotation.channels();
        if (channels.length == 0) {
            // Register for all channels
            channelProcessors.computeIfAbsent("*", k -> new ArrayList<>()).add(processorInfo);
        } else {
            // Register for specific channels
            for (String channel : channels) {
                channelProcessors.computeIfAbsent(channel.toLowerCase(), k -> new ArrayList<>()).add(processorInfo);
            }
        }
        
        // Sort by priority
        channelProcessors.values().forEach(list -> 
            list.sort(Comparator.comparingInt(p -> p.annotation.priority())));
        
        plugin.logResponse("Registered processor: " + processor.getName() + 
                         " for channels: " + (channels.length == 0 ? "all" : String.join(", ", channels)));
    }
    
    /**
     * Checks if message should be filtered in channel.
     * 
     * @param player Player sending message
     * @param message Message content
     * @param channelId Channel identifier
     * @return true if message should be filtered
     */
    public boolean shouldFilterMessage(Player player, String message, String channelId) {
        List<FilterInfo> filters = getFiltersForChannel(channelId);
        
        for (FilterInfo filterInfo : filters) {
            MessageFilter filter = filterInfo.filter;
            ChatFilter annotation = filterInfo.annotation;
            
            // Check if filter is enabled
            if (!filter.isEnabled()) continue;
            
            // Check bypass permission
            if (!annotation.bypassPermission().isEmpty() && 
                player.hasPermission(annotation.bypassPermission())) {
                continue;
            }
            
            // Apply filter
            if (filter.shouldFilter(player, message)) {
                // Log filter action
                plugin.logResponse("Message filtered by " + filter.getName() + 
                                 " for player " + player.getName() + " in channel " + channelId);
                
                // Send notification to player if not silent
                if (!annotation.silent()) {
                    player.sendMessage("§c" + filter.getReason());
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Processes message through all processors for channel.
     * 
     * @param player Player sending message
     * @param message Original message
     * @param channelId Channel identifier
     * @return Processed message
     */
    public String processMessage(Player player, String message, String channelId) {
        List<ProcessorInfo> processors = getProcessorsForChannel(channelId);
        String processedMessage = message;
        
        for (ProcessorInfo processorInfo : processors) {
            MessageProcessor processor = processorInfo.processor;
            ChatProcessor annotation = processorInfo.annotation;
            
            // Check if processor is enabled
            if (!processor.isEnabled()) continue;
            
            // Check permission requirements
            if (annotation.permissionRequired() && 
                !annotation.permission().isEmpty() && 
                !player.hasPermission(annotation.permission())) {
                continue;
            }
            
            // Apply processor
            try {
                String newMessage = processor.process(player, processedMessage);
                if (newMessage != null) {
                    processedMessage = newMessage;
                }
            } catch (Exception e) {
                plugin.logError("Error in processor " + processor.getName() + ": " + e.getMessage());
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        }
        
        return processedMessage;
    }
    
    /**
     * Gets filters for specific channel.
     */
    private List<FilterInfo> getFiltersForChannel(String channelId) {
        List<FilterInfo> filters = new ArrayList<>();
        
        // Add global filters
        filters.addAll(channelFilters.getOrDefault("*", Collections.emptyList()));
        
        // Add channel-specific filters
        filters.addAll(channelFilters.getOrDefault(channelId.toLowerCase(), Collections.emptyList()));
        
        // Sort by priority
        filters.sort(Comparator.comparingInt(f -> f.annotation.priority()));
        
        return filters;
    }
    
    /**
     * Gets processors for specific channel.
     */
    private List<ProcessorInfo> getProcessorsForChannel(String channelId) {
        List<ProcessorInfo> processors = new ArrayList<>();
        
        // Add global processors
        processors.addAll(channelProcessors.getOrDefault("*", Collections.emptyList()));
        
        // Add channel-specific processors
        processors.addAll(channelProcessors.getOrDefault(channelId.toLowerCase(), Collections.emptyList()));
        
        // Sort by priority
        processors.sort(Comparator.comparingInt(p -> p.annotation.priority()));
        
        return processors;
    }
    
    /**
     * Gets all registered filters.
     * 
     * @return Map of channel to filters
     */
    public Map<String, List<String>> getRegisteredFilters() {
        return channelFilters.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                            .map(f -> f.filter.getName())
                            .collect(Collectors.toList())
                ));
    }
    
    /**
     * Gets all registered processors.
     * 
     * @return Map of channel to processors
     */
    public Map<String, List<String>> getRegisteredProcessors() {
        return channelProcessors.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                            .map(p -> p.processor.getName())
                            .collect(Collectors.toList())
                ));
    }
    
    /**
     * Clears all registered filters and processors.
     */
    public void clear() {
        channelFilters.clear();
        channelProcessors.clear();
        plugin.logResponse("Cleared all filters and processors");
    }
    
    /**
     * Filter information holder.
     */
    private static class FilterInfo {
        final MessageFilter filter;
        final ChatFilter annotation;
        
        FilterInfo(MessageFilter filter, ChatFilter annotation) {
            this.filter = filter;
            this.annotation = annotation;
        }
    }
    
    /**
     * Processor information holder.
     */
    private static class ProcessorInfo {
        final MessageProcessor processor;
        final ChatProcessor annotation;
        
        ProcessorInfo(MessageProcessor processor, ChatProcessor annotation) {
            this.processor = processor;
            this.annotation = annotation;
        }
    }
}

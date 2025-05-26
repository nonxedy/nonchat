package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Annotation for chat message processors.
 * Classes with this annotation will process messages in specified channels.
 * 
 * Example usage:
 * <pre>
 * {@code 
 * @ChatProcessor(channels = {"global"}, priority = 100)
 * public class ColorCodeProcessor implements MessageProcessor {
 *     @Override
 *     public String process(Player sender, String message) {
 *         return message.replace("&", "§");
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChatProcessor {
    /**
     * Channels to apply processor to (empty = all channels)
     */
    String[] channels() default {};
    
    /**
     * Processor priority (lower = higher priority)
     */
    int priority() default 0;
    
    /**
     * Required permission to use processor
     */
    String permission() default "";
    
    /**
     * Permission required for processor to work
     */
    boolean permissionRequired() default false;
}

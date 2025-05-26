package com.nonxedy.nonchat.api.annotation;

import java.lang.annotation.*;

/**
 * Annotation for command parameters.
 * Provides automatic parameter injection and conversion.
 * 
 * Example usage:
 * <pre>
 * {@code 
 * @CommandHandler
 * public void execute(
 *     @Parameter(type = ParameterType.SENDER) Player sender,
 *     @Parameter(type = ParameterType.PLAYER) Player target,
 *     @Parameter(type = ParameterType.STRING, joined = true) String message
 * ) {
 *     // Implementation
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parameter {
    /**
     * Parameter type
     */
    ParameterType type();
    
    /**
     * Join remaining arguments into single string
     */
    boolean joined() default false;
    
    /**
     * Parameter is optional
     */
    boolean optional() default false;
    
    /**
     * Default value for optional parameters
     */
    String defaultValue() default "";
    
    /**
     * Parameter types supported by the system
     */
    enum ParameterType {
        SENDER,     // CommandSender or Player
        PLAYER,     // Online player by name
        STRING,     // String argument
        INTEGER,    // Integer argument
        DOUBLE,     // Double argument
        BOOLEAN     // Boolean argument
    }
}

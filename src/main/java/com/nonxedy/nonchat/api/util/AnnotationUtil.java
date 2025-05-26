package com.nonxedy.nonchat.api.util;

import com.nonxedy.nonchat.api.annotation.ChatFilter;
import com.nonxedy.nonchat.api.annotation.ChatProcessor;
import com.nonxedy.nonchat.api.annotation.Command;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for working with annotations.
 * Provides helper methods for annotation processing and validation.
 */
public class AnnotationUtil {
    
    /**
     * Validates command annotation.
     * 
     * @param command Command annotation to validate
     * @return List of validation errors (empty if valid)
     */
    public static List<String> validateCommand(Command command) {
        List<String> errors = new ArrayList<>();
        
        if (command.name().trim().isEmpty()) {
            errors.add("Command name cannot be empty");
        }
        
        if (command.name().contains(" ")) {
            errors.add("Command name cannot contain spaces");
        }
        
        for (String alias : command.aliases()) {
            if (alias.trim().isEmpty()) {
                errors.add("Command alias cannot be empty");
            }
            if (alias.contains(" ")) {
                errors.add("Command alias '" + alias + "' cannot contain spaces");
            }
        }
        
        return errors;
    }
    
    /**
     * Validates chat filter annotation.
     * 
     * @param filter Filter annotation to validate
     * @return List of validation errors (empty if valid)
     */
    public static List<String> validateChatFilter(ChatFilter filter) {
        List<String> errors = new ArrayList<>();
        
        if (filter.priority() < 0) {
            errors.add("Filter priority cannot be negative");
        }
        
        for (String channel : filter.channels()) {
            if (channel.trim().isEmpty()) {
                errors.add("Channel name cannot be empty");
            }
        }
        
        return errors;
    }
    
    /**
     * Validates chat processor annotation.
     * 
     * @param processor Processor annotation to validate
     * @return List of validation errors (empty if valid)
     */
    public static List<String> validateChatProcessor(ChatProcessor processor) {
        List<String> errors = new ArrayList<>();
        
        if (processor.priority() < 0) {
            errors.add("Processor priority cannot be negative");
        }
        
        for (String channel : processor.channels()) {
            if (channel.trim().isEmpty()) {
                errors.add("Channel name cannot be empty");
            }
        }
        
        return errors;
    }
    
    /**
     * Gets all methods with specific annotation from class.
     * 
     * @param clazz Class to scan
     * @param annotationClass Annotation class to look for
     * @return List of methods with the annotation
     */
    public static List<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        List<Method> methods = new ArrayList<>();
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationClass)) {
                methods.add(method);
            }
        }
        
        return methods;
    }
    
    /**
     * Checks if class has specific annotation.
     * 
     * @param clazz Class to check
     * @param annotationClass Annotation class to look for
     * @return true if class has the annotation
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }
    
    /**
     * Gets annotation from class with null safety.
     * 
     * @param clazz Class to get annotation from
     * @param annotationClass Annotation class
     * @param <T> Annotation type
     * @return Annotation instance or null if not present
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationClass) {
        return clazz.getAnnotation(annotationClass);
    }
    
    /**
     * Gets annotation from method with null safety.
     * 
     * @param method Method to get annotation from
     * @param annotationClass Annotation class
     * @param <T> Annotation type
     * @return Annotation instance or null if not present
     */
    public static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }
    
    /**
     * Checks if arrays contain common elements.
     * 
     * @param array1 First array
     * @param array2 Second array
     * @return true if arrays have common elements
     */
    public static boolean hasCommonElements(String[] array1, String[] array2) {
        if (array1.length == 0 || array2.length == 0) {
            return true; // Empty array means "all"
        }
        
        return Arrays.stream(array1)
                .anyMatch(element1 -> Arrays.stream(array2)
                        .anyMatch(element2 -> element1.equalsIgnoreCase(element2)));
    }
    
    /**
     * Converts annotation array to list.
     * 
     * @param array Annotation array
     * @return List of strings
     */
    public static List<String> arrayToList(String[] array) {
        return Arrays.asList(array);
    }
}

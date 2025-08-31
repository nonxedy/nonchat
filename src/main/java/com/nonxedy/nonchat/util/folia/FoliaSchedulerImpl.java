package com.nonxedy.nonchat.util.folia;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Folia implementation of FoliaScheduler using reflection for compatibility
 */
public class FoliaSchedulerImpl implements FoliaScheduler {
    
    private final Plugin plugin;
    private final Object regionScheduler;
    private final Object entityScheduler;
    private final Object asyncScheduler;
    private final Object globalRegionScheduler;
    
    public FoliaSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
        
        try {
            // Get Folia schedulers through reflection
            Object server = plugin.getServer();
            
            // Get RegionScheduler
            Method getRegionScheduler = server.getClass().getMethod("getRegionScheduler");
            this.regionScheduler = getRegionScheduler.invoke(server);
            
            // Get AsyncScheduler
            Method getAsyncScheduler = server.getClass().getMethod("getAsyncScheduler");
            this.asyncScheduler = getAsyncScheduler.invoke(server);
            
            // Get GlobalRegionScheduler
            Method getGlobalRegionScheduler = server.getClass().getMethod("getGlobalRegionScheduler");
            this.globalRegionScheduler = getGlobalRegionScheduler.invoke(server);
            
            // EntityScheduler is not obtained from server in current Folia versions
            // We'll set it to null and handle it in the entity-specific methods
            this.entityScheduler = null;
            
            plugin.getLogger().info("Folia scheduler initialized successfully");
            plugin.getLogger().fine("RegionScheduler: " + (regionScheduler != null ? regionScheduler.getClass().getName() : "null"));
            plugin.getLogger().fine("AsyncScheduler: " + (asyncScheduler != null ? asyncScheduler.getClass().getName() : "null"));
            plugin.getLogger().fine("GlobalRegionScheduler: " + (globalRegionScheduler != null ? globalRegionScheduler.getClass().getName() : "null"));
            
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            plugin.getLogger().severe("Failed to initialize Folia scheduler: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Folia scheduler", e);
        }
    }
    
    @Override
    public BukkitTask runTask(Runnable task) {
        try {
            plugin.getLogger().fine("Trying to run task with " + globalRegionScheduler.getClass().getName());
            // Use dynamic discovery for basic tasks too
            return findAndInvokeMethod(globalRegionScheduler, "run", task, 0);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule task, executing immediately: " + e.getMessage());
            task.run();
            return createImmediateTask();
        }
    }
    
    @Override
    public BukkitTask runTaskAsynchronously(Runnable task) {
        try {
            plugin.getLogger().fine("Trying to run async task with " + asyncScheduler.getClass().getName());
            
            // Try the most common Folia async method first
            Method[] methods = asyncScheduler.getClass().getMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if ((methodName.equals("runNow") || methodName.equals("run") || methodName.equals("submit")) &&
                    method.getParameterCount() == 2) { // Plugin, Runnable
                    try {
                        plugin.getLogger().fine("Trying async method: " + method.getName());
                        Object scheduledTask = method.invoke(asyncScheduler, plugin, task);
                        return wrapScheduledTask(scheduledTask);
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        plugin.getLogger().fine("Async method failed: " + method.getName());
                        // Continue to next method
                    }
                }
            }
            
            plugin.getLogger().fine("Falling back to dynamic discovery for async task");
            // Fallback to dynamic discovery
            return findAndInvokeMethod(asyncScheduler, "async", task, 0);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to run async task, executing in new thread: " + e.getMessage());
            new Thread(task).start();
            return createImmediateTask();
        }
    }
    
    @Override
    public BukkitTask runTaskLater(Runnable task, long delay) {
        try {
            plugin.getLogger().fine("Trying to run delayed task with " + globalRegionScheduler.getClass().getName() + " delay: " + delay);
            // Discover available methods for delayed execution
            return findAndInvokeMethod(globalRegionScheduler, "delayed", task, delay);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule delayed task, executing immediately: " + e.getMessage());
            task.run();
            return createImmediateTask();
        }
    }
    
    @Override
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        try {
            plugin.getLogger().fine("Trying to run delayed async task with " + asyncScheduler.getClass().getName() + " delay: " + delay);
            // Discover available methods for delayed async execution
            return findAndInvokeMethod(asyncScheduler, "delayed", task, delay);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to run delayed async task, executing in new thread: " + e.getMessage());
            new Thread(task).start();
            return createImmediateTask();
        }
    }
    
    /**
     * Helper method to find and invoke appropriate scheduling method
     */
    private BukkitTask findAndInvokeMethod(Object scheduler, String operationType, Runnable task, long delay) throws Exception {
        Method[] methods = scheduler.getClass().getMethods();
        
        plugin.getLogger().fine("Searching for " + operationType + " methods in " + scheduler.getClass().getName() + 
                              " with " + methods.length + " available methods");
        
        // First try: Look for methods with specific names related to the operation
        BukkitTask result = trySpecificMethods(scheduler, operationType, task, delay, methods);
        if (result != null) {
            return result;
        }
        
        // Second try: Try ANY method that could possibly work
        result = tryAnyMethod(scheduler, task, delay, methods);
        if (result != null) {
            return result;
        }
        
        // Last resort: Execute the task immediately without scheduling (avoid recursion)
        if (operationType.contains("async")) {
            plugin.getLogger().fine("No async scheduling method found, executing task immediately");
            new Thread(task).start(); // Run in a new thread for async
            return createImmediateTask();
        } else {
            plugin.getLogger().fine("No scheduling method found, executing task immediately");
            task.run(); // Run synchronously
            return createImmediateTask();
        }
    }
    
    /**
     * Creates an immediate task wrapper for tasks executed without scheduling
     */
    private BukkitTask createImmediateTask() {
        plugin.getLogger().fine("Creating immediate task wrapper");
        
        return new BukkitTask() {
            private volatile boolean cancelled = false;
            
            @Override
            public int getTaskId() {
                return -1; // No real task ID for immediate execution
            }
            
            @Override
            public Plugin getOwner() {
                return plugin;
            }
            
            @Override
            public boolean isSync() {
                return true;
            }
            
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
            
            @Override
            public void cancel() {
                cancelled = true;
                plugin.getLogger().fine("Immediate task cancelled");
            }
        };
    }
    
    /**
     * Try methods with specific names related to the operation
     */
    private BukkitTask trySpecificMethods(Object scheduler, String operationType, Runnable task, long delay, Method[] methods) {
        String[] possibleMethodNames = {
            "run", "execute", "submit", "schedule", 
            "runDelayed", "runLater", "delay",
            "runAt", "scheduleDelayed", "runAfter",
            "runNow", "runAsync", "async", "timed", "repeating"
        };
        
        plugin.getLogger().fine("Trying specific method names: " + String.join(", ", possibleMethodNames));
        
        for (String methodName : possibleMethodNames) {
            for (Method method : methods) {
                if (method.getName().toLowerCase().contains(methodName.toLowerCase()) || 
                    method.getName().toLowerCase().contains(operationType.toLowerCase())) {
                    
                    plugin.getLogger().fine("Found potential method: " + method.getName() + 
                                         " with " + method.getParameterCount() + " parameters");
                    
                    try {
                        Object[] args = tryInvokeMethod(method, plugin, task, delay);
                        if (args != null) {
                            plugin.getLogger().fine("Successfully matched parameters for method: " + method.getName());
                            Object scheduledTask = method.invoke(scheduler, args);
                            return wrapScheduledTask(scheduledTask);
                        } else {
                            plugin.getLogger().fine("Parameter mismatch for method: " + method.getName());
                        }
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        plugin.getLogger().fine("Method invocation failed for: " + method.getName());
                        // Continue to next method
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Try ANY method that could possibly work
     */
    private BukkitTask tryAnyMethod(Object scheduler, Runnable task, long delay, Method[] methods) {
        plugin.getLogger().fine("Trying fallback methods for scheduling");
        
        for (Method method : methods) {
            String methodName = method.getName().toLowerCase();
            
            // Skip methods that are clearly not scheduling-related
            if (methodName.contains("get") || methodName.contains("set") || 
                methodName.contains("is") || methodName.contains("has") ||
                methodName.contains("cancel") || methodName.contains("remove") ||
                methodName.contains("equals") || methodName.contains("hashcode") ||
                methodName.contains("tostring") || methodName.contains("notify") ||
                methodName.contains("wait") || methodName.contains("call") ||
                methodName.contains("register") || methodName.contains("unregister") ||
                methodName.length() < 3) {
                continue;
            }
            
            // Only try methods that could potentially be scheduling-related
            if (!methodName.contains("run") && !methodName.contains("exec") && 
                !methodName.contains("schedule") && !methodName.contains("task") &&
                !methodName.contains("submit") && !methodName.contains("call") &&
                !methodName.contains("invoke") && !methodName.contains("start")) {
                continue;
            }
            
            plugin.getLogger().fine("Trying fallback method: " + method.getName());
            
            try {
                Object[] args = tryInvokeMethod(method, plugin, task, delay);
                if (args != null) {
                    // Test invoke the method to see if it works
                    Object scheduledTask = method.invoke(scheduler, args);
                    plugin.getLogger().log(Level.INFO, "Using fallback method: {0}", method.getName());
                    return wrapScheduledTask(scheduledTask);
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                plugin.getLogger().fine("Fallback method failed: " + method.getName());
                // Continue to next method
            }
        }
        return null;
    }
    
    /**
     * Try to invoke method with different parameter combinations
     */
    private Object[] tryInvokeMethod(Method method, Plugin plugin, Runnable task, long delay) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        plugin.getLogger().fine("Trying to match parameters for method: " + method.getName() + 
                              " with types: " + getParameterTypesString(paramTypes));
        
        // Extended parameter combinations to try
        Object[][] possibleArgs = {
            {plugin, task, delay},
            {plugin, task},
            {task, delay},
            {task},
            {plugin, task, delay, delay},
            {plugin, task, (Object) null, delay},
            {task, plugin, delay},
            {delay, plugin, task},
            {plugin, delay, task},
            {task, delay, plugin},
            {plugin, task, delay, delay, delay},
            {plugin, (Runnable) task},
            {task, (long) delay},
            {(Runnable) task, (long) delay},
            {(Plugin) plugin, (Runnable) task, (long) delay}
        };
        
        for (Object[] args : possibleArgs) {
            if (args.length == paramTypes.length) {
                boolean typesMatch = true;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null) {
                        // Handle primitive types specially
                        if (paramTypes[i].isPrimitive()) {
                            if (args[i] instanceof Long && paramTypes[i] == long.class) {
                            } else if (args[i] instanceof Integer && paramTypes[i] == int.class) {
                            } else {
                                typesMatch = false;
                                break;
                            }
                        } else if (!paramTypes[i].isInstance(args[i])) {
                            typesMatch = false;
                            break;
                        }
                    }
                }
                if (typesMatch) {
                    plugin.getLogger().fine("Parameter match found for method: " + method.getName());
                    return args;
                }
            }
        }
        
        plugin.getLogger().fine("No parameter match found for method: " + method.getName());
        return null;
    }
    
    @Override
    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        try {
            plugin.getLogger().fine("Trying to run timer task with " + globalRegionScheduler.getClass().getName() + 
                                  " delay: " + delay + " period: " + period);
            // Use dynamic discovery for timer tasks
            return findAndInvokeTimerMethod(globalRegionScheduler, "timer", task, delay, period);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule timer task, executing immediately: " + e.getMessage());
            task.run();
            return createImmediateTask();
        }
    }
    
    /**
     * Helper method to find and invoke appropriate timer scheduling method
     */
    private BukkitTask findAndInvokeTimerMethod(Object scheduler, String operationType, Runnable task, long delay, long period) throws Exception {
        Method[] methods = scheduler.getClass().getMethods();
        
        plugin.getLogger().fine("Searching for timer methods in " + scheduler.getClass().getName() + 
                              " with " + methods.length + " available methods");
        
        // First try: Look for methods with specific names related to timer operations
        BukkitTask result = trySpecificTimerMethods(scheduler, operationType, task, delay, period, methods);
        if (result != null) {
            return result;
        }
        
        // Second try: Try ANY method that could possibly work for timers
        result = tryAnyTimerMethod(scheduler, task, delay, period, methods);
        if (result != null) {
            return result;
        }
        
        // Last resort: Execute the task immediately without scheduling
        plugin.getLogger().warning("No timer scheduling method found in " + scheduler.getClass().getName() + 
                                 ", executing task immediately");
        task.run();
        return createImmediateTask();
    }
    
    /**
     * Try methods with specific names for timer operations
     */
    private BukkitTask trySpecificTimerMethods(Object scheduler, String operationType, Runnable task, long delay, long period, Method[] methods) {
        String[] possibleMethodNames = {
            "runAtFixedRate", "runTimer", "scheduleAtFixedRate", "runRepeating",
            "scheduleRepeating", "runPeriodically", "schedulePeriodically",
            "timer", "repeating", "periodic", "fixedRate"
        };
        
        plugin.getLogger().fine("Trying specific timer method names: " + String.join(", ", possibleMethodNames));
        
        for (String methodName : possibleMethodNames) {
            for (Method method : methods) {
                if (method.getName().toLowerCase().contains(methodName.toLowerCase()) || 
                    method.getName().toLowerCase().contains(operationType.toLowerCase())) {
                    
                    plugin.getLogger().fine("Found potential timer method: " + method.getName() + 
                                         " with " + method.getParameterCount() + " parameters");
                    
                    try {
                        Object[] args = tryInvokeTimerMethod(method, plugin, task, delay, period);
                        if (args != null) {
                            plugin.getLogger().fine("Successfully matched parameters for method: " + method.getName());
                            Object scheduledTask = method.invoke(scheduler, args);
                            return wrapScheduledTask(scheduledTask);
                        } else {
                            plugin.getLogger().fine("Parameter mismatch for method: " + method.getName());
                        }
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        plugin.getLogger().fine("Method invocation failed for: " + method.getName());
                        // Continue to next method
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Try ANY method that could possibly work for timers
     */
    private BukkitTask tryAnyTimerMethod(Object scheduler, Runnable task, long delay, long period, Method[] methods) {
        plugin.getLogger().fine("Trying fallback methods for timer scheduling");
        
        for (Method method : methods) {
            String methodName = method.getName().toLowerCase();
            
            // Skip methods that are clearly not scheduling-related
            if (methodName.contains("get") || methodName.contains("set") || 
                methodName.contains("is") || methodName.contains("has") ||
                methodName.contains("cancel") || methodName.contains("remove") ||
                methodName.contains("equals") || methodName.contains("hashcode") ||
                methodName.contains("tostring") || methodName.contains("notify") ||
                methodName.contains("wait") || methodName.contains("class") ||
                methodName.contains("register") || methodName.contains("unregister") ||
                methodName.length() < 3) {
                continue;
            }
            
            // Only try methods that could potentially be timer-related
            if (!methodName.contains("run") && !methodName.contains("exec") && 
                !methodName.contains("schedule") && !methodName.contains("task") &&
                !methodName.contains("submit") && !methodName.contains("call") &&
                !methodName.contains("invoke") && !methodName.contains("start") &&
                !methodName.contains("timer") && !methodName.contains("repeat")) {
                continue;
            }
            
            plugin.getLogger().fine("Trying fallback method: " + method.getName());
            
            try {
                Object[] args = tryInvokeTimerMethod(method, plugin, task, delay, period);
                if (args != null) {
                    // Test invoke the method to see if it works
                    Object scheduledTask = method.invoke(scheduler, args);
                    plugin.getLogger().log(Level.INFO, "Using fallback timer method: {0}", method.getName());
                    return wrapScheduledTask(scheduledTask);
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                plugin.getLogger().fine("Fallback method failed: " + method.getName());
                // Continue to next method
            }
        }
        return null;
    }
    
    /**
     * Try to invoke timer method with different parameter combinations
     */
    private Object[] tryInvokeTimerMethod(Method method, Plugin plugin, Runnable task, long delay, long period) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        plugin.getLogger().fine("Trying to match parameters for method: " + method.getName() + 
                              " with types: " + getParameterTypesString(paramTypes));
        
        // Extended parameter combinations to try for timers
        Object[][] possibleArgs = {
            {plugin, task, delay, period},
            {plugin, task, period, delay},
            {task, delay, period},
            {task, period, delay},
            {plugin, task, delay, period, delay},
            {plugin, task, (Object) null, delay, period},
            {task, plugin, delay, period},
            {delay, period, plugin, task},
            {plugin, delay, period, task},
            {task, delay, period, plugin},
            {plugin, task, delay, period, delay, delay},
            {plugin, (Runnable) task, (long) delay, (long) period},
            {task, (long) delay, (long) period},
            {(Runnable) task, (long) delay, (long) period},
            {(Plugin) plugin, (Runnable) task, (long) delay, (long) period}
        };
        
        for (Object[] args : possibleArgs) {
            if (args.length == paramTypes.length) {
                boolean typesMatch = true;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null) {
                        // Handle primitive types specially
                        if (paramTypes[i].isPrimitive()) {
                            if (args[i] instanceof Long && paramTypes[i] == long.class) {
                            } else if (args[i] instanceof Integer && paramTypes[i] == int.class) {
                            } else {
                                typesMatch = false;
                                break;
                            }
                        } else if (!paramTypes[i].isInstance(args[i])) {
                            typesMatch = false;
                            break;
                        }
                    }
                }
                if (typesMatch) {
                    plugin.getLogger().fine("Parameter match found for method: " + method.getName());
                    return args;
                }
            }
        }
        
        plugin.getLogger().fine("No parameter match found for method: " + method.getName());
        return null;
    }
    
    /**
     * Helper method to get parameter types as string for logging
     */
    private String getParameterTypesString(Class<?>[] paramTypes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i].getSimpleName());
        }
        return sb.toString();
    }
    
    @Override
    public BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        try {
            plugin.getLogger().fine("Trying to run async timer task with " + asyncScheduler.getClass().getName() + 
                                  " delay: " + delay + " period: " + period);
            // Use dynamic discovery for async timer tasks
            return findAndInvokeTimerMethod(asyncScheduler, "asyncTimer", task, delay, period);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule async timer task, executing in new thread: " + e.getMessage());
            new Thread(task).start();
            return createImmediateTask();
        }
    }
    
    @Override
    public BukkitTask runTaskForEntity(Entity entity, Runnable task) {
        try {
            plugin.getLogger().fine("Trying to run task for entity: " + entity.getType().name() + " at " + 
                                  entity.getLocation().getWorld().getName() + " " + entity.getLocation().getBlockX() + "," + 
                                  entity.getLocation().getBlockY() + "," + entity.getLocation().getBlockZ());
            
            // In current Folia versions, use entity's scheduler directly
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(entity);
            
            plugin.getLogger().fine("Using entity scheduler: " + entityScheduler.getClass().getName());
            
            // Try different method signatures for entity scheduling
            Method[] methods = entityScheduler.getClass().getMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if ((methodName.equals("run") || methodName.equals("runNow")) && 
                    method.getParameterCount() >= 2) {
                    try {
                        Object scheduledTask;
                        if (method.getParameterCount() == 2) {
                            // Try (Plugin, Runnable)
                            scheduledTask = method.invoke(entityScheduler, plugin, task);
                        } else if (method.getParameterCount() == 3) {
                            // Try (Plugin, Runnable, Object)
                            scheduledTask = method.invoke(entityScheduler, plugin, task, null);
                        } else if (method.getParameterCount() == 4) {
                            // Try (Plugin, Runnable, Object, long)
                            scheduledTask = method.invoke(entityScheduler, plugin, task, null, 0L);
                        } else {
                            continue;
                        }
                        
                        if (scheduledTask != null) {
                            plugin.getLogger().fine("Successfully scheduled entity task using method: " + method.getName());
                            return wrapScheduledTask(scheduledTask);
                        }
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        plugin.getLogger().fine("Entity scheduling method failed: " + method.getName());
                        // Continue to next method
                    }
                }
            }
            
            plugin.getLogger().warning("No entity scheduling method worked, falling back to global scheduler");
            // Fallback to global scheduler
            return runTask(task);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to run task for entity, falling back to global scheduler: " + e.getMessage());
            // Fallback to global scheduler
            return runTask(task);
        }
    }
    
    @Override
    public BukkitTask runTaskLaterForEntity(Entity entity, Runnable task, long delay) {
        try {
            plugin.getLogger().fine("Trying to run delayed task for entity: " + entity.getType().name() + " delay: " + delay + 
                                  " at " + entity.getLocation().getWorld().getName() + " " + entity.getLocation().getBlockX() + "," + 
                                  entity.getLocation().getBlockY() + "," + entity.getLocation().getBlockZ());
            
            // In current Folia versions, use entity's scheduler directly
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(entity);
            
            plugin.getLogger().fine("Using entity scheduler for delayed task: " + entityScheduler.getClass().getName());
            
            // Try different method signatures for delayed entity scheduling
            Method[] methods = entityScheduler.getClass().getMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if ((methodName.equals("run") || methodName.equals("runDelayed") || methodName.equals("runLater")) && 
                    method.getParameterCount() >= 3) {
                    try {
                        Object scheduledTask;
                        if (method.getParameterCount() == 3) {
                            // Try (Plugin, Runnable, long)
                            scheduledTask = method.invoke(entityScheduler, plugin, task, delay);
                        } else if (method.getParameterCount() == 4) {
                            // Try (Plugin, Runnable, Object, long)
                            scheduledTask = method.invoke(entityScheduler, plugin, task, null, delay);
                        } else {
                            continue;
                        }
                        
                        if (scheduledTask != null) {
                            plugin.getLogger().fine("Successfully scheduled delayed entity task using method: " + method.getName());
                            return wrapScheduledTask(scheduledTask);
                        }
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        plugin.getLogger().fine("Delayed entity scheduling method failed: " + method.getName());
                        // Continue to next method
                    }
                }
            }
            
            plugin.getLogger().warning("No delayed entity scheduling method worked, falling back to global scheduler");
            // Fallback to global scheduler
            return runTaskLater(task, delay);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to run delayed task for entity, falling back to global scheduler: " + e.getMessage());
            // Fallback to global scheduler
            return runTaskLater(task, delay);
        }
    }
    
    @Override
    public BukkitTask runTaskAtLocation(Location location, Runnable task) {
        try {
            plugin.getLogger().fine("Trying to run location task with " + regionScheduler.getClass().getName() + 
                                  " at " + location.getWorld().getName() + " " + location.getBlockX() + "," + 
                                  location.getBlockY() + "," + location.getBlockZ());
            // Use dynamic discovery for location-based tasks
            return findAndInvokeMethodWithLocation(regionScheduler, "location", task, 0, location);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule location task, executing immediately: " + e.getMessage());
            task.run();
            return createImmediateTask();
        }
    }
    
    @Override
    public BukkitTask runTaskLaterAtLocation(Location location, Runnable task, long delay) {
        try {
            plugin.getLogger().fine("Trying to run delayed location task with " + regionScheduler.getClass().getName() + 
                                  " delay: " + delay + " at " + location.getWorld().getName() + " " + location.getBlockX() + "," + 
                                  location.getBlockY() + "," + location.getBlockZ());
            // Use dynamic discovery for delayed location-based tasks
            return findAndInvokeMethodWithLocation(regionScheduler, "delayed", task, delay, location);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule delayed location task, executing immediately: " + e.getMessage());
            task.run();
            return createImmediateTask();
        }
    }
    
    /**
     * Helper method for location-based scheduling with dynamic discovery
     */
    private BukkitTask findAndInvokeMethodWithLocation(Object scheduler, String operationType, Runnable task, long delay, Location location) throws Exception {
        Method[] methods = scheduler.getClass().getMethods();
        
        plugin.getLogger().fine("Searching for " + operationType + " location methods in " + scheduler.getClass().getName() + 
                              " with " + methods.length + " available methods");
        
        // First try: Look for methods with specific names related to the operation
        BukkitTask result = trySpecificMethodsWithLocation(scheduler, operationType, task, delay, location, methods);
        if (result != null) {
            return result;
        }
        
        // Second try: Try ANY method that could possibly work
        result = tryAnyMethodWithLocation(scheduler, task, delay, location, methods);
        if (result != null) {
            return result;
        }
        
        // Last resort: Fall back to non-location based methods
        if (operationType.contains("delayed")) {
            plugin.getLogger().warning("No delayed location method found, using global method");
            return runTaskLater(task, delay);
        } else {
            plugin.getLogger().warning("No location method found, using global method");
            return runTask(task);
        }
    }
    
    /**
     * Try methods with specific names for location-based operations
     */
    private BukkitTask trySpecificMethodsWithLocation(Object scheduler, String operationType, Runnable task, long delay, Location location, Method[] methods) {
        String[] possibleMethodNames = {
            "run", "execute", "submit", "schedule", 
            "runDelayed", "runLater", "delay",
            "runAt", "scheduleDelayed", "runAfter"
        };
        
        plugin.getLogger().fine("Trying specific location method names: " + String.join(", ", possibleMethodNames));
        
        for (String methodName : possibleMethodNames) {
            for (Method method : methods) {
                if (method.getName().toLowerCase().contains(methodName.toLowerCase()) || 
                    method.getName().toLowerCase().contains(operationType.toLowerCase())) {
                    
                    plugin.getLogger().fine("Found potential location method: " + method.getName() + 
                                         " with " + method.getParameterCount() + " parameters");
                    
                    try {
                        Object[] args = tryInvokeMethodWithLocation(method, plugin, task, delay, location);
                        if (args != null) {
                            plugin.getLogger().fine("Successfully matched parameters for location method: " + method.getName());
                            Object scheduledTask = method.invoke(scheduler, args);
                            return wrapScheduledTask(scheduledTask);
                        } else {
                            plugin.getLogger().fine("Parameter mismatch for location method: " + method.getName());
                        }
                    } catch (IllegalAccessException | InvocationTargetException ignored) {
                        plugin.getLogger().fine("Location method invocation failed for: " + method.getName());
                        // Continue to next method
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Try ANY method that could possibly work for location-based operations
     */
    private BukkitTask tryAnyMethodWithLocation(Object scheduler, Runnable task, long delay, Location location, Method[] methods) {
        plugin.getLogger().fine("Trying fallback location methods for scheduling");
        
        for (Method method : methods) {
            String methodName = method.getName().toLowerCase();
            
            // Skip methods that are clearly not scheduling-related
            if (methodName.contains("get") || methodName.contains("is") || 
                methodName.contains("has") || methodName.contains("cancel") || 
                methodName.contains("remove") || methodName.contains("equals") || 
                methodName.contains("hashcode") || methodName.contains("tostring") || 
                methodName.contains("notify") || methodName.contains("wait") || 
                methodName.contains("class") || methodName.contains("register") || 
                methodName.contains("unregister") || methodName.length() < 3) {
                continue;
            }
            
            // Only try methods that could potentially be scheduling-related
            if (!methodName.contains("run") && !methodName.contains("exec") && 
                !methodName.contains("schedule") && !methodName.contains("task") &&
                !methodName.contains("submit") && !methodName.contains("call") &&
                !methodName.contains("invoke") && !methodName.contains("start")) {
                continue;
            }
            
            plugin.getLogger().fine("Trying fallback location method: " + method.getName());
            
            try {
                Object[] args = tryInvokeMethodWithLocation(method, plugin, task, delay, location);
                if (args != null) {
                    // Test invoke the method to see if it works
                    Object scheduledTask = method.invoke(scheduler, args);
                    plugin.getLogger().log(Level.INFO, "Using fallback location method: {0}", method.getName());
                    return wrapScheduledTask(scheduledTask);
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                plugin.getLogger().fine("Fallback location method failed: " + method.getName());
                // Continue to next method
            }
        }
        return null;
    }
    
    /**
     * Try to invoke method with different parameter combinations including location
     */
    private Object[] tryInvokeMethodWithLocation(Method method, Plugin plugin, Runnable task, long delay, Location location) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        plugin.getLogger().fine("Trying to match parameters for location method: " + method.getName() + 
                              " with types: " + getParameterTypesString(paramTypes));
        
        // Extended parameter combinations to try (including location)
        Object[][] possibleArgs = {
            {plugin, task, location},
            {plugin, task, location, delay},
            {task, location},
            {task, location, delay},
            {plugin, task, location, delay, delay},
            {location, plugin, task},
            {location, plugin, task, delay},
            {plugin, location, task},
            {plugin, location, task, delay}
        };
        
        for (Object[] args : possibleArgs) {
            if (args.length == paramTypes.length) {
                boolean typesMatch = true;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null) {
                        // Handle primitive types specially
                        if (paramTypes[i].isPrimitive()) {
                            if (args[i] instanceof Long && paramTypes[i] == long.class) {
                            } else if (args[i] instanceof Integer && paramTypes[i] == int.class) {
                            } else {
                                typesMatch = false;
                                break;
                            }
                        } else if (!paramTypes[i].isInstance(args[i])) {
                            typesMatch = false;
                            break;
                        }
                    }
                }
                if (typesMatch) {
                    plugin.getLogger().fine("Parameter match found for location method: " + method.getName());
                    return args;
                }
            }
        }
        
        plugin.getLogger().fine("No parameter match found for location method: " + method.getName());
        return null;
    }
    
    @Override
    public BukkitTask runTaskForPlayer(Player player, Runnable task) {
        plugin.getLogger().fine("Running task for player: " + player.getName());
        return runTaskForEntity(player, task);
    }
    
    @Override
    public BukkitTask runTaskLaterForPlayer(Player player, Runnable task, long delay) {
        plugin.getLogger().fine("Running delayed task for player: " + player.getName() + " with delay: " + delay);
        return runTaskLaterForEntity(player, task, delay);
    }
    
    /**
     * Wraps Folia's ScheduledTask into a BukkitTask for compatibility
     */
    private BukkitTask wrapScheduledTask(Object scheduledTask) {
        if (scheduledTask == null) {
            plugin.getLogger().warning("Cannot wrap null scheduled task, creating immediate task wrapper");
            return createImmediateTask();
        }
        
        plugin.getLogger().fine("Wrapping Folia ScheduledTask: " + scheduledTask.getClass().getName());
        
        return new BukkitTask() {
            @Override
            public int getTaskId() {
                try {
                    return (int) scheduledTask.getClass().getMethod("getTaskId").invoke(scheduledTask);
                } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    plugin.getLogger().fine("Failed to get task ID: " + e.getMessage());
                    return -1;
                }
            }
            
            @Override
            public Plugin getOwner() {
                return plugin;
            }
            
            @Override
            public boolean isSync() {
                return true; // Folia tasks are always region-based
            }
            
            @Override
            public boolean isCancelled() {
                try {
                    return (boolean) scheduledTask.getClass().getMethod("isCancelled").invoke(scheduledTask);
                } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    plugin.getLogger().fine("Failed to check if cancelled: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            public void cancel() {
                try {
                    scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                    plugin.getLogger().fine("Task cancelled successfully");
                } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    plugin.getLogger().fine("Failed to cancel task: " + e.getMessage());
                    // Ignore cancellation errors
                }
            }
        };
    }
}

/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.utils.jexl;

import com.percussion.error.PSNotFoundException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.*;
import com.percussion.server.PSServer;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.servlet.PSServletUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.tools.ToolManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This class contains shared functionality used by specific evaluators with
 * modern Java 11 features for enhanced performance and type safety.
 * <p>
 * The evaluator provides thread-safe access to JEXL and Velocity tools,
 * with efficient caching and proper resource management.
 *
 * @author dougrand
 */
public class PSServiceJexlEvaluatorBase extends PSJexlEvaluator implements IPSExtensionListener {

    /**
     * The interface implemented/specified by all JEXL extensions.
     */
    public static final String IPSJEXL_EXPRESSION = "com.percussion.extension.IPSJexlExpression";

    /**
     * Extension context constants
     */
    public static final String SYS_CONTEXT = "global/percussion/system/";
    public static final String USER_CONTEXT = "global/percussion/user/";
    public static final String TOOLS_CONTEXT = "global/percussion/velocity/";

    /**
     * Prefix constants for different extension types
     */
    public static final String RX_PREFIX = "$rx";
    public static final String USER_PREFIX = "$user";
    public static final String TOOLS_PREFIX = "$tools";

    private static final Logger ms_log = LogManager.getLogger(PSServiceJexlEvaluatorBase.class);

    /**
     * The toolbox manager with thread-safe access
     */
    private static volatile ToolManager ms_mgr = null;

    /**
     * Thread-safe cache for JEXL functions
     */
    private static final Map<String, Map<String, Object>> ms_functionCache = new ConcurrentHashMap<>();

    /**
     * Lock for managing initialization and cache updates
     */
    private static final ReadWriteLock ms_cacheLock = new ReentrantReadWriteLock();

    /**
     * Atomic flag for initialization state
     */
    private static volatile boolean ms_initialized = false;

    /**
     * Constructor with enhanced error handling and validation
     *
     * @param initfuncs if {@code true} then initialize the function bindings
     */
    public PSServiceJexlEvaluatorBase(boolean initfuncs) {
        if (initfuncs) {
            initializeFunctions();
        }
    }

    /**
     * Initialize function bindings with proper error handling
     */
    private void initializeFunctions() {
        try {
            var rxFunctions = getJexlFunctions(SYS_CONTEXT);
            var userFunctions = getJexlFunctions(USER_CONTEXT);
            var toolsBindings = getVelocityToolBindings();

            bind(RX_PREFIX, rxFunctions);
            bind(USER_PREFIX, userFunctions);
            bind(TOOLS_PREFIX, toolsBindings);

            ms_log.info("Successfully initialized JEXL function bindings");
        } catch (Exception e) {
            ms_log.error("Problem binding functions: {}", PSExceptionUtils.getMessageForLog(e), e);
            throw new RuntimeException("Failed to initialize JEXL functions", e);
        }
    }

    /**
     * Instantiate and bind all the velocity tools with modern file handling
     *
     * @return the bindings, never {@code null}
     * @throws FileNotFoundException if tools.xml is not found
     * @throws Exception if configuration fails
     */
    public Map<String, Object> getVelocityToolBindings() throws FileNotFoundException, Exception {
        if (ms_mgr == null) {
            synchronized (PSServiceJexlEvaluatorBase.class) {
                if (ms_mgr == null) {
                    ms_mgr = createToolManager();
                }
            }
        }

        return Optional.ofNullable(ms_mgr.createContext())
            .map(context -> context.getToolbox())
            .orElse(Collections.emptyMap());
    }

    /**
     * Create and configure the ToolManager with proper validation
     *
     * @return configured ToolManager
     * @throws FileNotFoundException if tools.xml is not found
     * @throws Exception if configuration fails
     */
    private ToolManager createToolManager() throws FileNotFoundException, Exception {
        var configDir = PSServletUtils.getConfigDir();
        var toolsPath = Paths.get(configDir, "velocity", "tools.xml");

        if (!Files.exists(toolsPath)) {
            throw new FileNotFoundException("Velocity tools configuration not found: " + toolsPath);
        }

        var manager = new ToolManager();
        manager.configure(toolsPath.toString());

        ms_log.info("Successfully configured ToolManager with tools from: {}", toolsPath);
        return manager;
    }

    /**
     * Lookup JEXL extensions for a particular context with enhanced caching
     *
     * @param context the context to search, not {@code null} or empty
     * @return map of extension functions, never {@code null}
     * @throws PSExtensionException if extension lookup fails
     */
    public Map<String, Object> getJexlFunctions(String context) throws PSExtensionException {
        Objects.requireNonNull(context, "Context cannot be null");
        if (context.trim().isEmpty()) {
            throw new IllegalArgumentException("Context cannot be empty");
        }

        // Try cache first with read lock
        ms_cacheLock.readLock().lock();
        try {
            var cached = ms_functionCache.get(context);
            if (cached != null) {
                ms_log.debug("Retrieved {} JEXL functions from cache for context: {}",
                    cached.size(), context);
                return new HashMap<>(cached); // Return defensive copy
            }
        } finally {
            ms_cacheLock.readLock().unlock();
        }

        // Cache miss - acquire write lock and populate cache
        ms_cacheLock.writeLock().lock();
        try {
            // Double-check pattern
            var cached = ms_functionCache.get(context);
            if (cached != null) {
                return new HashMap<>(cached);
            }

            var functions = loadJexlFunctions(context);
            ms_functionCache.put(context, functions);
            ms_log.info("Loaded and cached {} JEXL functions for context: {}",
                functions.size(), context);

            return new HashMap<>(functions);
        } finally {
            ms_cacheLock.writeLock().unlock();
        }
    }

    /**
     * Load JEXL functions from the extension manager
     *
     * @param context the context to search
     * @return map of extension functions
     * @throws PSExtensionException if extension lookup fails
     */
    private Map<String, Object> loadJexlFunctions(String context) throws PSExtensionException {
        var extMgr = PSServer.getExtensionManager(null);
        if (extMgr == null) {
            ms_log.warn("Extension manager not available for context: {}", context);
            return Collections.emptyMap();
        }

        try {
            var extensionRefs = extMgr.getExtensionNames(
                null, null, IPSJEXL_EXPRESSION, context);

            if (extensionRefs.isEmpty()) {
                ms_log.debug("No JEXL extensions found for context: {}", context);
                return Collections.emptyMap();
            }

            var functions = new HashMap<String, Object>();

            for (var ref : extensionRefs) {
                try {
                    var instance = extMgr.prepareExtension(ref, null);
                    if (instance != null) {
                        functions.put(ref.getExtensionName(), instance);
                        ms_log.trace("Loaded JEXL extension: {} for context: {}",
                            ref.getExtensionName(), context);
                    }
                } catch (Exception e) {
                    ms_log.warn("Failed to load JEXL extension {}: {}",
                        ref.getExtensionName(), e.getMessage());
                }
            }

            return functions;
        } catch (PSExtensionException e) {
            ms_log.error("Failed to retrieve extensions for context {}: {}",
                context, e.getMessage());
            throw e;
        }
    }

    /**
     * Get available contexts with their function counts
     *
     * @return map of context to function count
     */
    public Map<String, Integer> getAvailableContexts() {
        ms_cacheLock.readLock().lock();
        try {
            return ms_functionCache.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ));
        } finally {
            ms_cacheLock.readLock().unlock();
        }
    }

    /**
     * Check if a specific context has been loaded
     *
     * @param context the context to check
     * @return {@code true} if context is loaded, {@code false} otherwise
     */
    public boolean isContextLoaded(String context) {
        if (context == null) {
            return false;
        }

        ms_cacheLock.readLock().lock();
        try {
            return ms_functionCache.containsKey(context);
        } finally {
            ms_cacheLock.readLock().unlock();
        }
    }

    /**
     * Clear the function cache - useful for testing or reloading extensions
     */
    public void clearCache() {
        ms_cacheLock.writeLock().lock();
        try {
            ms_functionCache.clear();
            ms_initialized = false;
            ms_log.info("Cleared JEXL function cache");
        } finally {
            ms_cacheLock.writeLock().unlock();
        }
    }

    @Override
    public void extensionAdded(PSExtensionRef ref) {
        if (isJexlExtension(ref)) {
            invalidateCache("Extension added: " + ref.getExtensionName());
        }
    }

    @Override
    public void extensionRemoved(PSExtensionRef ref) {
        if (isJexlExtension(ref)) {
            invalidateCache("Extension removed: " + ref.getExtensionName());
        }
    }

    @Override
    public void extensionUpdated(PSExtensionRef ref) {
        if (isJexlExtension(ref)) {
            invalidateCache("Extension updated: " + ref.getExtensionName());
        }
    }

    /**
     * Check if the extension reference is a JEXL extension
     *
     * @param ref the extension reference to check
     * @return {@code true} if it's a JEXL extension, {@code false} otherwise
     */
    private boolean isJexlExtension(PSExtensionRef ref) {
        if (ref == null) {
            return false;
        }

        try {
            var extMgr = PSServer.getExtensionManager(null);
            if (extMgr == null) {
                return false;
            }

            var def = extMgr.getExtensionDef(ref);
            return def != null &&
                   def.getInterfaces().contains(IPSJEXL_EXPRESSION);
        } catch (Exception e) {
            ms_log.debug("Error checking if extension {} is JEXL extension: {}",
                ref.getExtensionName(), e.getMessage());
            return false;
        }
    }

    /**
     * Invalidate the function cache when extensions change
     *
     * @param reason the reason for invalidation (for logging)
     */
    private void invalidateCache(String reason) {
        ms_cacheLock.writeLock().lock();
        try {
            ms_functionCache.clear();
            ms_log.info("Invalidated JEXL function cache: {}", reason);
        } finally {
            ms_cacheLock.writeLock().unlock();
        }
    }

    /**
     * Get cache statistics for monitoring and debugging
     *
     * @return map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        ms_cacheLock.readLock().lock();
        try {
            var stats = new HashMap<String, Object>();
            stats.put("contextsLoaded", ms_functionCache.size());
            stats.put("totalFunctions", ms_functionCache.values().stream()
                .mapToInt(Map::size).sum());
            stats.put("initialized", ms_initialized);
            stats.put("toolManagerInitialized", ms_mgr != null);

            return Collections.unmodifiableMap(stats);
        } finally {
            ms_cacheLock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        return String.format("PSServiceJexlEvaluatorBase[stats=%s]", getCacheStats());
    }
}

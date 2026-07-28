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
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.extension.*;
import com.percussion.server.PSServer;
import com.percussion.servlet_utils.servlet.PSServletUtils;
import com.percussion.utils.jexl.PSJexlEvaluator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.EasyFactoryConfiguration;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.apache.velocity.tools.generic.CollectionTool;
import org.apache.velocity.tools.generic.ComparisonDateTool;
import org.apache.velocity.tools.generic.ContextTool;
import org.apache.velocity.tools.generic.DisplayTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.LinkTool;
import org.apache.velocity.tools.generic.LoopTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.generic.RenderTool;
import org.apache.velocity.tools.generic.SortTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
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
        var toolsPath = Paths.get(configDir.toString(), "velocity", "tools.xml");

        if (!Files.exists(toolsPath)) {
            throw new FileNotFoundException("Velocity tools configuration not found: " + toolsPath);
        }

        var manager = new ToolManager(false, false);
        FactoryConfiguration config = loadVelocityToolsConfig(toolsPath);
        manager.configure(config);

        ms_log.info("Successfully configured ToolManager with tools from: {}", toolsPath);
        return manager;
    }

    /**
     * Load Velocity Tools 3.x factory config from product tools.xml.
     *
     * <p>Velocity Tools 3 XML config uses Commons Digester3. On Jetty the digester SAX parser
     * may be null ({@code Digester.getParser() == null}), so {@code ConfigurationUtils.read}
     * and {@code getDefaultTools()} both fail. Prefer product tools.xml when digester works;
     * otherwise build a programmatic toolbox (no digester) so assembly/preview/search indexing
     * can still run.
     */
    private FactoryConfiguration loadVelocityToolsConfig(Path toolsPath) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader webappLoader = PSServiceJexlEvaluatorBase.class.getClassLoader();
        try {
            if (webappLoader != null) {
                Thread.currentThread().setContextClassLoader(webappLoader);
            }
            try {
                URL toolsUrl = toolsPath.toUri().toURL();
                FactoryConfiguration productConfig = ConfigurationUtils.read(toolsUrl);
                if (productConfig != null) {
                    return productConfig;
                }
                ms_log.warn(
                    "Velocity tools.xml at {} could not be parsed (null config); "
                        + "using programmatic tool defaults.",
                    toolsPath);
            } catch (Exception e) {
                ms_log.warn(
                    "Failed to load Velocity tools.xml at {}: {}. Using programmatic tool defaults.",
                    toolsPath,
                    e.toString());
            }
            return createProgrammaticVelocityToolsConfig();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /**
     * Digester-free Velocity Tools config used when tools.xml cannot be parsed under Jetty.
     * Covers the common generic tools referenced by product templates.
     */
    private static FactoryConfiguration createProgrammaticVelocityToolsConfig() {
        EasyFactoryConfiguration config = new EasyFactoryConfiguration(false);
        config.number("TOOLS_VERSION", "3.1");
        config.toolbox("application")
            .tool(MathTool.class)
            .tool(NumberTool.class)
            .tool(ComparisonDateTool.class)
            .tool(DisplayTool.class)
            .tool(EscapeTool.class)
            .tool(CollectionTool.class)
            .tool(SortTool.class);
        config.toolbox("request")
            .tool(ContextTool.class)
            .tool(LinkTool.class)
            .tool(LoopTool.class)
            .tool(RenderTool.class);
        // Product list helper (replacement for dropped ListTool) when on classpath
        try {
            Class<?> listTool = Class.forName("com.percussion.extension.PSVelocityListTool");
            config.toolbox("application").tool("list", listTool);
        } catch (ClassNotFoundException ignored) {
            // optional
        }
        return config;
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
            Iterator<PSExtensionRef> extensionRefs = extMgr.getExtensionNames(
                null, null, IPSJEXL_EXPRESSION, context);

            if (extensionRefs == null || !extensionRefs.hasNext()) {
                ms_log.debug("No JEXL extensions found for context: {}", context);
                return Collections.emptyMap();
            }

            var functions = new HashMap<String, Object>();

            while (extensionRefs.hasNext()) {
                PSExtensionRef ref = extensionRefs.next();
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
    public void extensionAdded(PSExtensionRef ref, PSExtensionManager manager) {
        if (isJexlExtension(ref)) {
            invalidateCache("Extension added: " + ref.getExtensionName());
        }
    }

    @Override
    public void extensionShutdown(PSExtensionRef ref, IPSExtensionManager mgr) {
        if (isJexlExtension(ref)) {
            invalidateCache("Extension shutdown: " + ref.getExtensionName());
        }
    }

    @Override
    public void extensionRemoved(PSExtensionRef ref, IPSExtensionManager mgr) {
        if (isJexlExtension(ref)) {
            invalidateCache("Extension removed: " + ref.getExtensionName());
        }
    }

    @Override
    public void extensionUpdated(PSExtensionRef ref, IPSExtensionManager mgr) {
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
            if (def == null) {
                return false;
            }
            var interfaces = def.getInterfaces();
            if (interfaces == null) {
                return false;
            }
            while (interfaces.hasNext()) {
                if (IPSJEXL_EXPRESSION.equals(interfaces.next())) {
                    return true;
                }
            }
            return false;
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

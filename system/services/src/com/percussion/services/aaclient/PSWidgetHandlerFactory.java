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
// REFACTORED: CP-JAVA11
package com.percussion.services.aaclient;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Factory class for creating widget handlers in the Active Assembly client system.
 * This factory provides thread-safe access to widget handler instances based on
 * widget names, with support for lazy initialization and caching.
 *
 * @author Percussion Software
 */
public final class PSWidgetHandlerFactory {

    private static final Logger log = LogManager.getLogger(PSWidgetHandlerFactory.class);

    /**
     * Cache for widget handler instances to avoid repeated instantiation
     */
    private static final Map<String, IPSWidgetHandler> handlerCache = new ConcurrentHashMap<>();

    /**
     * Widget type enumeration with associated handler suppliers
     */
    public enum WidgetType {
        PAGE_TREE("pt", PSPageTree::new),
        ACTION_BAR("ab", PSActionBar::new),
        ACTION_EXECUTOR("ae", PSActionExecutor::new),
        HASHED_FILE("hf", PSHashedFileWidgetHandler::new);

        private final String widgetName;
        private final Supplier<IPSWidgetHandler> handlerSupplier;

        WidgetType(String widgetName, Supplier<IPSWidgetHandler> handlerSupplier) {
            this.widgetName = Objects.requireNonNull(widgetName, "Widget name cannot be null");
            this.handlerSupplier = Objects.requireNonNull(handlerSupplier, "Handler supplier cannot be null");
        }

        public String getWidgetName() {
            return widgetName;
        }

        public IPSWidgetHandler createHandler() {
            return handlerSupplier.get();
        }

        /**
         * Finds a widget type by its name (case-insensitive).
         *
         * @param widgetName the widget name to search for
         * @return Optional containing the matching WidgetType, or empty if not found
         */
        public static Optional<WidgetType> findByName(String widgetName) {
            if (StringUtils.isEmpty(widgetName)) {
                return Optional.empty();
            }

            var normalizedName = widgetName.toLowerCase().trim();
            for (var type : values()) {
                if (type.widgetName.equals(normalizedName)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    // Private constructor to prevent instantiation
    private PSWidgetHandlerFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets a widget handler instance for the specified widget name.
     * Handlers are cached for performance and thread safety.
     *
     * @param widgetName the name of the widget, must not be null or empty
     * @return the appropriate widget handler instance, never null
     * @throws IllegalArgumentException if widgetName is null, empty, or no handler exists
     */
    public static IPSWidgetHandler getHandler(String widgetName) {
        if (StringUtils.isEmpty(widgetName)) {
            throw new IllegalArgumentException("Widget name must not be null or empty");
        }

        var normalizedName = widgetName.toLowerCase().trim();

        return handlerCache.computeIfAbsent(normalizedName, name ->
            WidgetType.findByName(name)
                .map(type -> {
                    log.debug("Creating new handler for widget: {}", name);
                    return type.createHandler();
                })
                .orElseThrow(() -> {
                    log.warn("No handler available for widget: {}", name);
                    return new IllegalArgumentException(
                        "No handler is available for widget named '" + name + "'");
                })
        );
    }

    /**
     * Checks if a handler is available for the specified widget name.
     *
     * @param widgetName the widget name to check
     * @return true if a handler is available, false otherwise
     */
    public static boolean isHandlerAvailable(String widgetName) {
        return WidgetType.findByName(widgetName).isPresent();
    }

    /**
     * Clears the handler cache. Useful for testing or when handlers need to be recreated.
     */
    public static void clearCache() {
        log.debug("Clearing widget handler cache");
        handlerCache.clear();
    }

    /**
     * Gets the number of cached handler instances.
     *
     * @return the cache size
     */
    public static int getCacheSize() {
        return handlerCache.size();
    }

    /*
     * Constants for backward compatibility
     */

    /**
     * Active Assembly Page Tree widget identifier
     */
    public static final String WIDGET_PAGETREE = WidgetType.PAGE_TREE.getWidgetName();

    /**
     * Action Bar widget identifier
     */
    public static final String WIDGET_ACTIONBAR = WidgetType.ACTION_BAR.getWidgetName();

    /**
     * Action Executor widget identifier
     */
    public static final String WIDGET_ACTIONEXECUTOR = WidgetType.ACTION_EXECUTOR.getWidgetName();

    /**
     * Hashed File widget identifier
     */
    public static final String WIDGET_HASHEDFILE = WidgetType.HASHED_FILE.getWidgetName();
}

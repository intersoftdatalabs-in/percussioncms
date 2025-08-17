// REFACTORED: CP-JAVA11
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
package com.percussion.services.contentmgr;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Optional;

/**
 * Modern Java 11 locator for the content manager implementation.
 *
 * <p>This class provides thread-safe access to the content manager service using
 * the double-checked locking pattern with volatile field for optimal performance.
 * It follows the singleton pattern and includes both traditional and Optional-based
 * access methods for enhanced null safety.
 *
 * <p>The locator is implemented as a utility class with static methods only,
 * preventing instantiation and ensuring proper resource management.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public final class PSContentMgrLocator extends PSBaseServiceLocator {

    /**
     * The bean name for the content manager service in the Spring context.
     */
    private static final String CONTENT_MANAGER_BEAN_NAME = "sys_contentManager";

    /**
     * Cached content manager instance with volatile for thread safety.
     */
    private static volatile IPSContentMgr contentManager = null;

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always, as this class cannot be instantiated
     */
    private PSContentMgrLocator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the content manager instance using thread-safe lazy initialization.
     *
     * <p>This method uses the double-checked locking pattern for optimal performance
     * while ensuring thread safety. The content manager is loaded from the Spring
     * context on first access and cached for subsequent calls.
     *
     * @return the content manager instance, never null
     * @throws RuntimeException if the content manager service cannot be located
     */
    public static IPSContentMgr getContentMgr() {
        var localRef = contentManager; // Local reference for performance
        if (localRef == null) {
            synchronized (PSContentMgrLocator.class) {
                localRef = contentManager;
                if (localRef == null) {
                    localRef = loadContentManager();
                    contentManager = localRef;
                }
            }
        }
        return localRef;
    }

    /**
     * Safely gets the content manager instance with Optional wrapper.
     *
     * <p>This method provides null-safe access to the content manager service.
     * It never throws exceptions and returns an empty Optional if the service
     * cannot be located.
     *
     * @return an Optional containing the content manager, or empty if not available
     */
    public static Optional<IPSContentMgr> getContentMgrSafely() {
        try {
            return Optional.of(getContentMgr());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if the content manager service is available.
     *
     * @return true if the content manager can be located, false otherwise
     */
    public static boolean isContentMgrAvailable() {
        return getContentMgrSafely().isPresent();
    }

    /**
     * Gets the content manager instance if already cached, without triggering initialization.
     *
     * @return an Optional containing the cached content manager, or empty if not yet initialized
     */
    public static Optional<IPSContentMgr> getCachedContentMgr() {
        return Optional.ofNullable(contentManager);
    }

    /**
     * Resets the cached content manager instance, forcing reinitialization on next access.
     *
     * <p><strong>Warning:</strong> This method should only be used for testing purposes
     * or when reconfiguring the Spring context. In production environments, the content
     * manager should remain cached for optimal performance.
     */
    public static synchronized void reset() {
        contentManager = null;
    }

    /**
     * Loads the content manager from the Spring context.
     *
     * @return the content manager instance
     * @throws RuntimeException if the content manager service cannot be located
     */
    private static IPSContentMgr loadContentManager() {
        var manager = (IPSContentMgr) getBean(CONTENT_MANAGER_BEAN_NAME);
        if (manager == null) {
            throw new RuntimeException(
                String.format("Content manager service '%s' not found in Spring context",
                    CONTENT_MANAGER_BEAN_NAME));
        }
        return manager;
    }
}

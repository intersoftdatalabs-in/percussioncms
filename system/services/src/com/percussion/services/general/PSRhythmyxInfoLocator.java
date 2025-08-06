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
package com.percussion.services.general;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Optional;

/**
 * Modern Java 11 service locator for the Rhythmyx Information service.
 *
 * <p>This class provides thread-safe access to the {@link IPSRhythmyxInfo} service using
 * the double-checked locking pattern with volatile field for optimal performance.
 * It follows the singleton pattern and includes both traditional and Optional-based
 * access methods for enhanced null safety.
 *
 * <p>The locator is implemented as a utility class with static methods only,
 * preventing instantiation and ensuring proper resource management.
 *
 * @since Java 11 Modernization
 */
public class PSRhythmyxInfoLocator extends PSBaseServiceLocator {

    /**
     * The bean name for the Rhythmyx info service in the Spring context.
     */
    private static final String RHYTHMYX_INFO_BEAN_NAME = "sys_rhythmyxinfo";

    /**
     * Cached Rhythmyx info service instance with volatile for thread safety.
     */
    private static volatile IPSRhythmyxInfo rhythmyxInfo = null;

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always, as this class cannot be instantiated
     */
    private PSRhythmyxInfoLocator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the Rhythmyx info service using thread-safe lazy initialization.
     *
     * <p>This method uses the double-checked locking pattern for optimal performance
     * while ensuring thread safety. The service is loaded from the Spring context
     * on first access and cached for subsequent calls.
     *
     * <p>The service provides access to basic server configuration information
     * such as installation directory, listener ports, version, and testing mode flags.
     *
     * @return the Rhythmyx info service, never null on a correctly configured server
     * @throws PSMissingBeanConfigurationException if the Spring bean cannot be found
     */
    public static IPSRhythmyxInfo getRhythmyxInfo() throws PSMissingBeanConfigurationException {
        var localRef = rhythmyxInfo; // Local reference for performance
        if (localRef == null) {
            synchronized (PSRhythmyxInfoLocator.class) {
                localRef = rhythmyxInfo;
                if (localRef == null) {
                    localRef = loadRhythmyxInfo();
                    rhythmyxInfo = localRef;
                }
            }
        }
        return localRef;
    }

    /**
     * Safely gets the Rhythmyx info service with Optional wrapper.
     *
     * <p>This method provides null-safe access to the Rhythmyx info service.
     * It never throws exceptions and returns an empty Optional if the service
     * cannot be located.
     *
     * @return an Optional containing the Rhythmyx info service, or empty if not available
     */
    public static Optional<IPSRhythmyxInfo> getRhythmyxInfoSafely() {
        try {
            return Optional.of(getRhythmyxInfo());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if the Rhythmyx info service is available.
     *
     * @return true if the service can be located, false otherwise
     */
    public static boolean isRhythmyxInfoAvailable() {
        return getRhythmyxInfoSafely().isPresent();
    }

    /**
     * Gets the Rhythmyx info service instance if already cached, without triggering initialization.
     *
     * @return an Optional containing the cached service, or empty if not yet initialized
     */
    public static Optional<IPSRhythmyxInfo> getCachedRhythmyxInfo() {
        return Optional.ofNullable(rhythmyxInfo);
    }

    /**
     * Resets the cached service instance, forcing reinitialization on next access.
     *
     * <p><strong>Warning:</strong> This method should only be used for testing purposes
     * or when reconfiguring the Spring context. In production environments, the service
     * should remain cached for optimal performance.
     */
    public static synchronized void reset() {
        rhythmyxInfo = null;
    }

    /**
     * Loads the Rhythmyx info service from the Spring context.
     *
     * @return the Rhythmyx info service instance
     * @throws PSMissingBeanConfigurationException if the service cannot be located
     */
    private static IPSRhythmyxInfo loadRhythmyxInfo() throws PSMissingBeanConfigurationException {
        var service = (IPSRhythmyxInfo) getBean(RHYTHMYX_INFO_BEAN_NAME);
        if (service == null) {
            throw new PSMissingBeanConfigurationException(
                String.format("Rhythmyx info service '%s' not found in Spring context",
                    RHYTHMYX_INFO_BEAN_NAME));
        }
        return service;
    }

    /**
     * Convenience method to get server root directory.
     *
     * @return an Optional containing the root directory, or empty if service unavailable
     */
    public static Optional<String> getRootDirectory() {
        return getRhythmyxInfoSafely()
                .flatMap(IPSRhythmyxInfo::getRootDirectory);
    }

    /**
     * Convenience method to get HTTP listener port.
     *
     * @return an Optional containing the HTTP port, or empty if service unavailable
     */
    public static Optional<Integer> getListenerPort() {
        return getRhythmyxInfoSafely()
                .flatMap(IPSRhythmyxInfo::getListenerPort);
    }

    /**
     * Convenience method to get HTTPS listener port.
     *
     * @return an Optional containing the HTTPS port, or empty if service unavailable
     */
    public static Optional<Integer> getSslListenerPort() {
        return getRhythmyxInfoSafely()
                .flatMap(IPSRhythmyxInfo::getSslListenerPort);
    }

    /**
     * Convenience method to get server version.
     *
     * @return an Optional containing the version string, or empty if service unavailable
     */
    public static Optional<String> getVersion() {
        return getRhythmyxInfoSafely()
                .flatMap(IPSRhythmyxInfo::getVersion);
    }

    /**
     * Convenience method to check if server is in unit testing mode.
     *
     * @return true if unit testing mode is enabled, false otherwise or if service unavailable
     */
    public static boolean isUnitTestingMode() {
        return getRhythmyxInfoSafely()
                .map(IPSRhythmyxInfo::isUnitTestingMode)
                .orElse(false);
    }
}

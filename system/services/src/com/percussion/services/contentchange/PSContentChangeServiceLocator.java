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
package com.percussion.services.contentchange;

import com.percussion.services.PSBaseServiceLocator;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service locator for the Content Change Service with enhanced Java 11 support.
 *
 * <p>This class provides thread-safe access to the {@link IPSContentChangeService} service
 * using modern Java concurrency patterns and Optional-based error handling.
 *
 * <p>The locator uses atomic references and lazy initialization to ensure
 * thread safety without synchronization overhead on subsequent access.
 *
 * @author JaySeletz
 * @since Java 11 Modernization
 */

public class PSContentChangeServiceLocator extends PSBaseServiceLocator {

    private static final AtomicReference<IPSContentChangeService> SERVICE_REF =
        new AtomicReference<>();

    private static final String BEAN_NAME = "sys_contentChangeService";

    /**
     * Private constructor to prevent external instantiation.
     */
    private PSContentChangeServiceLocator() {
        // Utility class - no instantiation
    }

    /**
     * Retrieves the Content Change Service instance using thread-safe lazy initialization.
     *
     * <p>This method uses atomic references to ensure thread safety without
     * the overhead of synchronization blocks on subsequent calls.
     *
     * @return the content change service instance, never null
     * @throws RuntimeException if the service cannot be initialized
     */
    public static IPSContentChangeService getContentChangeService() {
        return SERVICE_REF.updateAndGet(current -> {
            if (current != null) {
                return current;
            }
            try {
                return (IPSContentChangeService) getBean(BEAN_NAME);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Content Change Service", e);
            }
        });
    }

    /**
     * Safely retrieves the Content Change Service instance wrapped in an Optional.
     *
     * <p>This method provides a safe way to access the service without
     * throwing exceptions, returning an empty Optional if the service is not available.
     *
     * @return an Optional containing the service instance, or empty if not available
     */
    public static Optional<IPSContentChangeService> getContentChangeServiceSafely() {
        try {
            return Optional.of(getContentChangeService());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

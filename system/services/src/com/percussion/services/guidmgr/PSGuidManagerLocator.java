/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.guidmgr;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service locator for the GUID manager service with enhanced Java 11 support.
 *
 * <p>This class provides thread-safe access to the {@link IPSGuidManager} service
 * using modern Java concurrency patterns and Optional-based error handling.
 *
 * <p>The locator uses lazy initialization with atomic references to ensure
 * thread safety without synchronization overhead on subsequent access.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
@ThreadSafe
public class PSGuidManagerLocator extends PSBaseServiceLocator {

    private static final AtomicReference<IPSGuidManager> GUID_MANAGER_REF =
        new AtomicReference<>();

    private static final String BEAN_NAME = "sys_guidmanager";

    /**
     * Private constructor to prevent external instantiation.
     */
    private PSGuidManagerLocator() {
        // Utility class - no instantiation
    }

    /**
     * Retrieves the GUID manager service instance using thread-safe lazy initialization.
     *
     * @return the GUID manager service instance
     * @throws PSMissingBeanConfigurationException if the service bean is not configured
     */
    public static IPSGuidManager getGuidMgr() throws PSMissingBeanConfigurationException {
        return GUID_MANAGER_REF.updateAndGet(current -> {
            if (current != null) {
                return current;
            }
            try {
                return (IPSGuidManager) getBean(BEAN_NAME);
            } catch (PSMissingBeanConfigurationException e) {
                throw new RuntimeException("Failed to initialize GUID manager", e);
            }
        });
    }

    /**
     * Safely retrieves the GUID manager service instance wrapped in an Optional.
     *
     * <p>This method provides a safe way to access the GUID manager without
     * throwing exceptions, returning an empty Optional if the service is not available.
     *
     * @return an Optional containing the GUID manager service, or empty if not available
     */
    public static Optional<IPSGuidManager> getGuidMgrSafely() {
        try {
            return Optional.of(getGuidMgr());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

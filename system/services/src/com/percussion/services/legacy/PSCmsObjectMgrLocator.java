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
package com.percussion.services.legacy;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service locator for CMS object manager with Java 11 enhancements.
 * Provides thread-safe access to the CMS object manager instance with
 * enhanced error handling and modern concurrency patterns.
 *
 * @author dougrand
 */
public class PSCmsObjectMgrLocator extends PSBaseServiceLocator {

    /**
     * Thread-safe reference to the CMS object manager instance.
     */
    private static final AtomicReference<IPSCmsObjectMgr> objectMgrRef = new AtomicReference<>();

    /**
     * Bean name for the CMS object manager in the Spring context.
     */
    private static final String CMS_OBJECT_MGR_BEAN = "sys_cmsObjectMgr";

    /**
     * Gets the CMS object manager instance with enhanced thread safety and error handling.
     * Uses double-checked locking pattern with AtomicReference for optimal performance.
     *
     * @return the CMS object manager instance, never null
     * @throws PSMissingBeanConfigurationException if the bean configuration is missing or invalid
     * @throws IllegalStateException if the service locator is not properly initialized
     */
    public static IPSCmsObjectMgr getObjectManager() throws PSMissingBeanConfigurationException {
        var objectMgr = objectMgrRef.get();
        if (objectMgr == null) {
            synchronized (PSCmsObjectMgrLocator.class) {
                objectMgr = objectMgrRef.get();
                if (objectMgr == null) {
                    try {
                        objectMgr = (IPSCmsObjectMgr) getBean(CMS_OBJECT_MGR_BEAN);
                        Objects.requireNonNull(objectMgr,
                                "CMS object manager bean cannot be null");
                        objectMgrRef.set(objectMgr);
                    } catch (Exception e) {
                        var errorMessage = String.format(
                                "Failed to locate CMS object manager bean '%s': %s",
                                CMS_OBJECT_MGR_BEAN, e.getMessage());
                        throw new PSMissingBeanConfigurationException(errorMessage, e);
                    }
                }
            }
        }
        return objectMgr;
    }

    /**
     * Clears the cached object manager instance.
     * Useful for testing or when the Spring context is refreshed.
     */
    public static synchronized void clearCache() {
        objectMgrRef.set(null);
    }

    /**
     * Checks if the object manager is currently cached.
     *
     * @return true if the object manager instance is cached, false otherwise
     */
    public static boolean isCached() {
        return objectMgrRef.get() != null;
    }
}

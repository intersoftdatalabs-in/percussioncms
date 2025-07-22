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
package com.percussion.services.linkmanagement;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for managed link DAO using modern Java 11 patterns.
 * Provides thread-safe access to managed link DAO instances with enhanced caching
 * and validation for reliable link management operations.
 *
 * @author JaySeletz
 */
public final class PSManagedLinkDaoLocator {

    /**
     * Bean name for the managed link DAO.
     */
    public static final String MANAGED_LINK_DAO_BEAN = "sys_managedLinkDao";

    /**
     * Thread-safe reference to the managed link DAO instance.
     */
    private static final AtomicReference<IPSManagedLinkDao> MANAGED_LINK_DAO_REF =
        new AtomicReference<>();

    /**
     * Lazy supplier for thread-safe initialization.
     */
    private static final Supplier<IPSManagedLinkDao> DAO_SUPPLIER = () -> {
        var dao = (IPSManagedLinkDao) PSBaseServiceLocator.getBean(MANAGED_LINK_DAO_BEAN);
        Objects.requireNonNull(dao, "Managed link DAO bean cannot be null");
        return dao;
    };

    /**
     * Private constructor to prevent instantiation.
     */
    private PSManagedLinkDaoLocator() {
        // Utility class - no instantiation
    }

    /**
     * Gets the managed link DAO with thread-safe lazy initialization.
     * Uses double-checked locking pattern with atomic reference for optimal performance.
     *
     * @return the managed link DAO, never null
     * @throws IllegalStateException if the DAO cannot be initialized
     */
    public static IPSManagedLinkDao getManagedLinkDao() {
        var dao = MANAGED_LINK_DAO_REF.get();
        if (dao == null) {
            dao = MANAGED_LINK_DAO_REF.updateAndGet(current ->
                current != null ? current : DAO_SUPPLIER.get());
        }
        return dao;
    }

    /**
     * Gets the managed link DAO with safe access using Optional for null-safe operations.
     *
     * @return Optional containing the managed link DAO if available, empty if not found
     */
    public static Optional<IPSManagedLinkDao> findManagedLinkDao() {
        try {
            return Optional.of(getManagedLinkDao());
        } catch (Exception e) {
            // Log the exception if needed, but return empty Optional
            return Optional.empty();
        }
    }

    /**
     * Creates a supplier for lazy managed link DAO retrieval.
     *
     * @return Supplier that provides the managed link DAO when called
     */
    public static Supplier<IPSManagedLinkDao> createManagedLinkDaoSupplier() {
        return () -> getManagedLinkDao();
    }

    /**
     * Clear the cached DAO instance for testing or reconfiguration purposes.
     * This method is primarily intended for unit testing scenarios.
     */
    public static void clearCache() {
        MANAGED_LINK_DAO_REF.set(null);
    }

    /**
     * Check if the managed link DAO is currently cached.
     *
     * @return true if the DAO instance is cached, false otherwise
     */
    public static boolean isCached() {
        return MANAGED_LINK_DAO_REF.get() != null;
    }

    /**
     * Get the managed link DAO bean name for configuration purposes.
     *
     * @return the bean name used for service lookup
     */
    public static String getServiceBeanName() {
        return MANAGED_LINK_DAO_BEAN;
    }
}

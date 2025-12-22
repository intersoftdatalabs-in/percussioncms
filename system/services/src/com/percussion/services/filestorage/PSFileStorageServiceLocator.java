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
package com.percussion.services.filestorage;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for the file storage service using modern Java 11 patterns.
 *
 * <p>This locator provides thread-safe access to the file storage service
 * with atomic reference patterns and Optional-based safe access methods.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSFileStorageServiceLocator {

    /**
     * Thread-safe reference to the file storage service instance.
     */
    private static final AtomicReference<IPSFileStorageService> FILE_STORAGE_SERVICE_REF =
        new AtomicReference<>();

    /**
     * Bean name for the file storage service.
     */
    public static final String FILESTORAGE_SERVICE_BEAN = "sys_fileStorageService";

    /**
     * Lazy service supplier for thread-safe initialization.
     */
    private static final Supplier<IPSFileStorageService> SERVICE_SUPPLIER = () ->
        (IPSFileStorageService) PSBaseServiceLocator.getBean(FILESTORAGE_SERVICE_BEAN);

    /**
     * Gets the file storage service using modern lazy initialization patterns.
     *
     * @return the file storage service, never {@code null}
     */
    public static IPSFileStorageService getFileStorageService() {
        return FILE_STORAGE_SERVICE_REF.updateAndGet(existing ->
            existing != null ? existing : SERVICE_SUPPLIER.get());
    }

    /**
     * Gets the file storage service safely with Optional wrapper.
     *
     * @return Optional containing the file storage service, or empty if not available
     */
    public static Optional<IPSFileStorageService> getFileStorageServiceSafely() {
        try {
            return Optional.of(getFileStorageService());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Clears the cached file storage service instance - primarily for testing purposes.
     * This method is thread-safe and will force reinitialization on next access.
     */
    public static void clearCache() {
        FILE_STORAGE_SERVICE_REF.set(null);
    }
}

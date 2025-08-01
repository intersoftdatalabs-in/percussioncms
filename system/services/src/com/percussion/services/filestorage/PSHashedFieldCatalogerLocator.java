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

package com.percussion.services.filestorage;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for the hashed field cataloger service using modern Java 11 patterns.
 *
 * <p>This service helps to locate and store a record of database columns containing
 * references to binaries by hash. This is used to make sure we can accurately and
 * safely remove unused binaries. We have to be careful as we do not want an error
 * to cause us to miss a reference and then remove more binaries than we should.</p>
 *
 * @author stephenbolton
 * @since 6.0
 */
public class PSHashedFieldCatalogerLocator {

    /**
     * Thread-safe reference to the hashed field cataloger service instance.
     */
    private static final AtomicReference<IPSHashedFieldCataloger> HASHED_FIELD_CATALOGER_REF =
        new AtomicReference<>();

    /**
     * Bean name for the hashed field cataloger service.
     */
    public static final String HASHED_FIELD_CATALOGER_BEAN = "sys_hashedFieldCatalogerService";

    /**
     * Lazy service supplier for thread-safe initialization.
     */
    private static final Supplier<IPSHashedFieldCataloger> SERVICE_SUPPLIER = () ->
        (IPSHashedFieldCataloger) PSBaseServiceLocator.getBean(HASHED_FIELD_CATALOGER_BEAN);

    /**
     * Gets the hashed field cataloger service using modern lazy initialization patterns.
     *
     * @return the singleton bean instance, never {@code null}
     */
    public static IPSHashedFieldCataloger getHashedFileCatalogerService() {
        return HASHED_FIELD_CATALOGER_REF.updateAndGet(existing ->
            existing != null ? existing : SERVICE_SUPPLIER.get());
    }

    /**
     * Gets the hashed field cataloger service safely with Optional wrapper.
     *
     * @return Optional containing the cataloger service, or empty if not available
     */
    public static Optional<IPSHashedFieldCataloger> getHashedFileCatalogerServiceSafely() {
        try {
            return Optional.of(getHashedFileCatalogerService());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Clears the cached cataloger service instance - primarily for testing purposes.
     * This method is thread-safe and will force reinitialization on next access.
     */
    public static void clearCache() {
        HASHED_FIELD_CATALOGER_REF.set(null);
    }
}

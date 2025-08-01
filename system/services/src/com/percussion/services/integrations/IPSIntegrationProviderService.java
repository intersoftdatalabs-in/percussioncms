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

package com.percussion.services.integrations;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for integration provider services used by REST endpoints with Java 11 enhancements.
 * Provides modern type-safe operations for third-party integrations with enhanced error handling.
 */
public interface IPSIntegrationProviderService {

    /**
     * Validates the provided credentials with enhanced type safety.
     *
     * @param credentials the credentials to validate, never null
     * @return true if credentials are valid, false otherwise
     * @throws IntegrationProviderException if validation cannot be completed
     */
    boolean validateCredentials(Map<String, String> credentials) throws IntegrationProviderException;

    /**
     * Retrieves site information from the third-party service.
     *
     * @param siteName the site name to retrieve information for, never null or empty
     * @param credentials authentication credentials for the third-party API, never null
     * @return site information from the associated service, wrapped in Optional
     * @throws IntegrationProviderException if retrieval fails
     */
    Optional<String> retrieveSiteInfo(String siteName, Map<String, String> credentials)
            throws IntegrationProviderException;

    /**
     * Updates third-party information for the specified site.
     *
     * @param siteName the name of the site to update, never null or empty
     * @param credentials authentication credentials for the third-party API, never null
     * @throws IntegrationProviderException if update fails
     */
    void updateSiteInfo(String siteName, Map<String, String> credentials)
            throws IntegrationProviderException;

    /**
     * Retrieves page information from the third-party service.
     *
     * @param siteName the main site URL (e.g., https://www.percussion.com/), never null or empty
     * @param pageURL the absolute URL to the page (e.g., https://www.percussion.com/products), never null or empty
     * @param credentials authentication credentials for the third-party API, never null
     * @return page information from the third party, wrapped in Optional
     * @throws IntegrationProviderException if retrieval fails
     */
    Optional<String> retrievePageInfo(String siteName, String pageURL, Map<String, String> credentials)
            throws IntegrationProviderException;

    /**
     * Updates page information in the third-party service.
     *
     * @param siteName the name of the site containing the page, never null or empty
     * @param pageURL the URL of the page to update, never null or empty
     * @param credentials authentication credentials for the third-party API, never null
     * @throws IntegrationProviderException if update fails
     */
    void updatePageInfo(String siteName, String pageURL, Map<String, String> credentials)
            throws IntegrationProviderException;

    /**
     * Validates credentials asynchronously for improved performance.
     *
     * @param credentials the credentials to validate, never null
     * @return CompletableFuture containing validation result
     */
    default CompletableFuture<Boolean> validateCredentialsAsync(Map<String, String> credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return validateCredentials(credentials);
            } catch (IntegrationProviderException e) {
                throw new RuntimeException("Async credential validation failed", e);
            }
        });
    }

    /**
     * Custom exception for integration provider operations.
     */
    class IntegrationProviderException extends Exception {
        public IntegrationProviderException(String message) {
            super(message);
        }

        public IntegrationProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

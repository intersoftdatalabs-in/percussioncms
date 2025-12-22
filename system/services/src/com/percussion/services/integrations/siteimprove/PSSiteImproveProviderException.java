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

package com.percussion.services.integrations.siteimprove;

import com.percussion.services.integrations.IPSIntegrationProviderService.IntegrationProviderException;

import java.util.Objects;
import java.util.Optional;

/**
 * Exception for SiteImprove provider operations with Java 11 enhancements.
 * Provides enhanced error handling and context information for SiteImprove integration failures.
 */
public class PSSiteImproveProviderException extends IntegrationProviderException {

    /**
     * Error codes for specific SiteImprove provider failures.
     */
    public enum ErrorCode {
        AUTHENTICATION_FAILED("Authentication with SiteImprove API failed"),
        API_TIMEOUT("SiteImprove API request timed out"),
        INVALID_SITE_URL("Invalid site URL provided"),
        SITE_NOT_FOUND("Site not found in SiteImprove"),
        PAGE_NOT_FOUND("Page not found in SiteImprove"),
        API_RATE_LIMIT("SiteImprove API rate limit exceeded"),
        NETWORK_ERROR("Network error while connecting to SiteImprove"),
        INVALID_CREDENTIALS("Invalid SiteImprove API credentials"),
        UNKNOWN_ERROR("Unknown SiteImprove provider error");

        private final String description;

        ErrorCode(String description) {
            this.description = Objects.requireNonNull(description, "Error description cannot be null");
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorCode errorCode;
    private final String contextInfo;

    /**
     * Creates a new SiteImprove provider exception with a message.
     *
     * @param message the error message, never null
     */
    public PSSiteImproveProviderException(String message) {
        super(Objects.requireNonNull(message, "Exception message cannot be null"));
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.contextInfo = null;
    }

    /**
     * Creates a new SiteImprove provider exception with a cause.
     *
     * @param cause the underlying cause, never null
     */
    public PSSiteImproveProviderException(Throwable cause) {
        super(Objects.requireNonNull(cause, "Exception cause cannot be null").getMessage(), cause);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.contextInfo = null;
    }

    /**
     * Creates a new SiteImprove provider exception with message and cause.
     *
     * @param message the error message, never null
     * @param cause the underlying cause, never null
     */
    public PSSiteImproveProviderException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "Exception message cannot be null"),
              Objects.requireNonNull(cause, "Exception cause cannot be null"));
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.contextInfo = null;
    }

    /**
     * Creates a new SiteImprove provider exception with error code and context.
     *
     * @param errorCode the specific error code, never null
     * @param contextInfo additional context information, may be null
     */
    public PSSiteImproveProviderException(ErrorCode errorCode, String contextInfo) {
        super(buildErrorMessage(errorCode, contextInfo));
        this.errorCode = Objects.requireNonNull(errorCode, "Error code cannot be null");
        this.contextInfo = contextInfo;
    }

    /**
     * Creates a new SiteImprove provider exception with error code, context, and cause.
     *
     * @param errorCode the specific error code, never null
     * @param contextInfo additional context information, may be null
     * @param cause the underlying cause, never null
     */
    public PSSiteImproveProviderException(ErrorCode errorCode, String contextInfo, Throwable cause) {
        super(buildErrorMessage(errorCode, contextInfo),
              Objects.requireNonNull(cause, "Exception cause cannot be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "Error code cannot be null");
        this.contextInfo = contextInfo;
    }

    /**
     * Gets the specific error code for this exception.
     *
     * @return the error code, never null
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the additional context information if available.
     *
     * @return context information wrapped in Optional
     */
    public Optional<String> getContextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Builds a comprehensive error message from error code and context.
     *
     * @param errorCode the error code, never null
     * @param contextInfo additional context, may be null
     * @return formatted error message
     */
    private static String buildErrorMessage(ErrorCode errorCode, String contextInfo) {
        var message = new StringBuilder(errorCode.getDescription());

        if (contextInfo != null && !contextInfo.trim().isEmpty()) {
            message.append(" - ").append(contextInfo.trim());
        }

        return message.toString();
    }

    /**
     * Creates a convenience exception for authentication failures.
     *
     * @param apiKey the API key that failed (for logging purposes), may be null
     * @return configured exception instance
     */
    public static PSSiteImproveProviderException authenticationFailed(String apiKey) {
        var context = apiKey != null ? "API Key: " + maskApiKey(apiKey) : "No API key provided";
        return new PSSiteImproveProviderException(ErrorCode.AUTHENTICATION_FAILED, context);
    }

    /**
     * Creates a convenience exception for site not found errors.
     *
     * @param siteUrl the site URL that was not found, never null
     * @return configured exception instance
     */
    public static PSSiteImproveProviderException siteNotFound(String siteUrl) {
        return new PSSiteImproveProviderException(ErrorCode.SITE_NOT_FOUND,
                "Site URL: " + Objects.requireNonNull(siteUrl, "Site URL cannot be null"));
    }

    /**
     * Masks an API key for safe logging (shows only first and last 4 characters).
     *
     * @param apiKey the API key to mask, may be null
     * @return masked API key string
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}

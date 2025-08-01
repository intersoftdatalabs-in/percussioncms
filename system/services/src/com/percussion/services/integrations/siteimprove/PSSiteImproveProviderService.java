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
// REFACTORED: CP-JAVA11
package com.percussion.services.integrations.siteimprove;

import com.percussion.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.services.integrations.IPSIntegrationProviderService;
import com.percussion.util.PSURLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * SiteImprove provider service modernized for Java 11 with enhanced HTTP client,
 * stream processing, and improved error handling.
 *
 * <p>Provides asynchronous operations for SiteImprove API integration with
 * comprehensive retry logic, proper resource management, and type-safe operations.
 */
public class PSSiteImproveProviderService implements IPSIntegrationProviderService {

    // API endpoints for the SiteImprove API
    private static final String NEW_SITEIMPROVE_BASE_URL = "https://api-gateway.siteimprove.com/cms-recheck";
    private static final String SITEIMPROVE_TOKEN_URL = "https://my2.siteimprove.com/auth/token?cms=";

    // Constants
    private static final String PERCUSSION_CM1_VERSION = "Percussion CMS " + PSServer.getVersion();
    private static final String SITEIMPROVE_RECRAWL_SITE = "recrawl";
    private static final String SITEIMPROVE_RECHECK_PAGE = "recheck";
    private static final String SITEIMPROVE_TOKEN = "token";

    // HTTP configuration
    private static final String APPLICATION_JSON = "application/json";
    private static final String DEFAULT_PROTOCOL = "http";
    private static final int MAX_RETRIES = 4;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(3);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    // Modern Java 11 HTTP client with optimal configuration
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(HTTP_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    // Thread-safe executor service with proper resource management
    private static final ExecutorService executorService =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final Logger logger = LogManager.getLogger(PSSiteImproveProviderService.class);

    // Static initialization with proper resource cleanup
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Gets a new SiteImprove token using modern HTTP client and enhanced error handling.
     *
     * @return the token wrapped in Optional, empty if not available
     */
    public Optional<String> getNewSiteImproveToken() {
        try {
            var encodedVersion = PSURLEncoder.encodeQuery(PERCUSSION_CM1_VERSION);
            var tokenUri = URI.create(SITEIMPROVE_TOKEN_URL + encodedVersion);

            var request = HttpRequest.newBuilder(tokenUri)
                .GET()
                .timeout(HTTP_TIMEOUT)
                .header("Accept", APPLICATION_JSON)
                .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("Failed to get SiteImprove token - HTTP {} response", response.statusCode());
                return Optional.empty();
            }

            var jsonObject = new JSONObject(response.body());
            var token = jsonObject.optString(SITEIMPROVE_TOKEN);

            return Optional.ofNullable(token)
                .filter(StringUtils::isNotBlank);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Unable to get new SiteImprove token: {}", PSExceptionUtils.getMessageForLog(e));
            logger.debug("Token retrieval error details", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error getting SiteImprove token: {}", PSExceptionUtils.getMessageForLog(e));
            return Optional.empty();
        }
    }

    @Override
    public boolean validateCredentials(Map<String, String> credentials) {
        Objects.requireNonNull(credentials, "Credentials cannot be null");

        var hasValidToken = Optional.ofNullable(credentials.get(SITEIMPROVE_TOKEN))
            .filter(StringUtils::isNotBlank)
            .isPresent();

        var hasValidSiteName = Optional.ofNullable(credentials.get("sitename"))
            .filter(StringUtils::isNotBlank)
            .isPresent();

        if (!hasValidToken || !hasValidSiteName) {
            return false;
        }

        // Set defaults for optional parameters using modern Map operations
        credentials.putIfAbsent("siteProtocol", DEFAULT_PROTOCOL);
        credentials.putIfAbsent("defaultDocument", "index.html");
        credentials.putIfAbsent("canonicalDist", "pages");

        return true;
    }

    @Override
    public Optional<String> retrieveSiteInfo(String siteName, Map<String, String> credentials)
            throws IntegrationProviderException {
        throw new IntegrationProviderException(
            "Site info retrieval not implemented - handled by front-end SiteImprove plugin");
    }

    @Override
    public Optional<String> retrievePageInfo(String siteName, String pageURL, Map<String, String> credentials)
            throws IntegrationProviderException {
        throw new IntegrationProviderException(
            "Page info retrieval not implemented - handled by front-end SiteImprove plugin");
    }

    @Override
    public void updateSiteInfo(String siteId, Map<String, String> credentials)
            throws IntegrationProviderException {
        validateInputs(siteId, credentials, "Site ID");

        CompletableFuture
            .runAsync(() -> performSiteUpdate(siteId, credentials), executorService)
            .exceptionally(throwable -> {
                logger.error("Async site update failed for site {}: {}",
                    siteId, PSExceptionUtils.getMessageForLog(throwable));
                return null;
            });
    }

    @Override
    public void updatePageInfo(String siteId, String pageURL, Map<String, String> credentials)
            throws IntegrationProviderException {
        validateInputs(siteId, credentials, "Site ID");
        validateInputs(pageURL, credentials, "Page URL");

        CompletableFuture
            .runAsync(() -> performPageUpdateWithRetries(pageURL, credentials), executorService)
            .exceptionally(throwable -> {
                logger.error("Async page update failed for page {} on site {}: {}",
                    pageURL, siteId, PSExceptionUtils.getMessageForLog(throwable));
                return null;
            });
    }

    /**
     * Performs the actual site update operation using modern HTTP client.
     */
    private void performSiteUpdate(String siteId, Map<String, String> credentials) {
        try {
            var jsonPayload = createSiteUpdatePayload(siteId, credentials);
            var request = createJsonPostRequest(NEW_SITEIMPROVE_BASE_URL, jsonPayload);

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PSSiteImproveProviderException(
                    PSSiteImproveProviderException.ErrorCode.API_TIMEOUT,
                    "Failed site update for ID: " + siteId + " (HTTP " + response.statusCode() + ")");
            }

            logger.debug("Site update successful for ID: {}", siteId);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(new PSSiteImproveProviderException(
                PSSiteImproveProviderException.ErrorCode.NETWORK_ERROR,
                "Network error during site update for ID: " + siteId, e));
        } catch (Exception e) {
            throw new RuntimeException(new PSSiteImproveProviderException(
                PSSiteImproveProviderException.ErrorCode.NETWORK_ERROR,
                "Site update error for ID: " + siteId, e));
        }
    }

    /**
     * Performs page update with retry logic using modern stream-based approach.
     */
    private void performPageUpdateWithRetries(String pageURL, Map<String, String> credentials) {
        var finalURL = preprocessPageURL(pageURL, credentials);
        var jsonPayload = createPageUpdatePayload(finalURL, credentials);

        var success = IntStream.range(0, MAX_RETRIES)
            .anyMatch(attempt -> {
                try {
                    var request = createJsonPostRequest(NEW_SITEIMPROVE_BASE_URL, jsonPayload);
                    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        logger.debug("Page update successful for URL: {} on attempt {}", pageURL, attempt + 1);
                        return true;
                    }

                    if (attempt < MAX_RETRIES - 1) {
                        Thread.sleep(RETRY_DELAY.toMillis());
                    }
                    return false;

                } catch (IOException | InterruptedException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    logger.warn("Page update attempt {} failed for URL {}: {}",
                        attempt + 1, pageURL, PSExceptionUtils.getMessageForLog(e));

                    if (attempt < MAX_RETRIES - 1) {
                        try {
                            Thread.sleep(RETRY_DELAY.toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    logger.error("Unexpected error during page update attempt {} for URL {}: {}",
                        attempt + 1, pageURL, PSExceptionUtils.getMessageForLog(e));
                    return false;
                }
            });

        if (!success) {
            throw new RuntimeException(new PSSiteImproveProviderException(
                PSSiteImproveProviderException.ErrorCode.API_TIMEOUT,
                "All retry attempts failed for page update: " + pageURL));
        }
    }

    /**
     * Creates a JSON POST request using modern HTTP client builder.
     */
    private HttpRequest createJsonPostRequest(String url, JSONObject payload) {
        return HttpRequest.newBuilder(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .timeout(HTTP_TIMEOUT)
            .header("Content-Type", APPLICATION_JSON)
            .header("Accept", APPLICATION_JSON)
            .build();
    }

    /**
     * Creates site update payload with enhanced validation.
     */
    private JSONObject createSiteUpdatePayload(String siteId, Map<String, String> credentials) {
        var payload = new JSONObject();
        payload.put("token", credentials.get(SITEIMPROVE_TOKEN));
        payload.put("url", buildSiteURL(siteId, credentials));
        payload.put("action", SITEIMPROVE_RECRAWL_SITE);
        return payload;
    }

    /**
     * Creates page update payload with enhanced validation.
     */
    private JSONObject createPageUpdatePayload(String pageURL, Map<String, String> credentials) {
        var payload = new JSONObject();
        payload.put("token", credentials.get(SITEIMPROVE_TOKEN));
        payload.put("url", pageURL);
        payload.put("action", SITEIMPROVE_RECHECK_PAGE);
        return payload;
    }

    /**
     * Builds the site URL using modern string processing.
     */
    private String buildSiteURL(String siteId, Map<String, String> credentials) {
        var protocol = credentials.getOrDefault("siteProtocol", DEFAULT_PROTOCOL);
        var defaultDoc = credentials.getOrDefault("defaultDocument", "index.html");

        return String.format("%s://%s/%s", protocol, siteId, defaultDoc);
    }

    /**
     * Preprocesses page URL with enhanced validation and modern string operations.
     */
    private String preprocessPageURL(String pageURL, Map<String, String> credentials) {
        var canonicalDist = credentials.getOrDefault("canonicalDist", "pages");

        return Optional.ofNullable(pageURL)
            .filter(StringUtils::isNotBlank)
            .map(url -> url.contains(canonicalDist) ? url : url + "/" + canonicalDist)
            .orElse(pageURL);
    }

    /**
     * Validates inputs with enhanced null safety.
     */
    private void validateInputs(String value, Map<String, String> credentials, String fieldName)
            throws IntegrationProviderException {

        if (StringUtils.isBlank(value)) {
            throw new IntegrationProviderException(fieldName + " cannot be null or empty");
        }

        Objects.requireNonNull(credentials, "Credentials cannot be null");

        if (!credentials.containsKey(SITEIMPROVE_TOKEN) ||
            StringUtils.isBlank(credentials.get(SITEIMPROVE_TOKEN))) {
            throw new IntegrationProviderException("SiteImprove token is required");
        }
    }
}

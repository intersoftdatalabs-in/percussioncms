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

package com.percussion.widgets.image.web.impl;

import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.services.ImageCacheManager;
import com.percussion.widgets.image.services.ImageCacheManagerLocator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Spring MVC controller for serving binary image data directly to clients.
 * Handles image retrieval from cache and streams binary content with appropriate headers.
 *
 * @since Java 11
 */
public class BinaryImageController extends AbstractController implements Controller {

    private static final Logger log = LogManager.getLogger(BinaryImageController.class);

    /** Pattern for extracting image key from URI */
    private static final Pattern KEY_EXTRACTION_PATTERN = Pattern.compile(".*/([^/]+)$");

    /** Default MIME type for unknown image types */
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /** Buffer size for streaming binary data */
    private static final int BUFFER_SIZE = 8192;

    private volatile ImageCacheManager cacheManager;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        try {
            initializeCacheManager();

            var imageKeyOpt = extractImageKeyFromUri(request.getRequestURI());
            if (imageKeyOpt.isEmpty()) {
                log.error("Could not extract image key from URI: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image request URI");
                return null;
            }

            var imageKey = imageKeyOpt.get();
            log.debug("Processing binary image request for key: {}", imageKey);

            var imageDataOpt = retrieveImageData(imageKey);
            if (imageDataOpt.isEmpty()) {
                log.info("Image not found for key: {}", imageKey);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return null;
            }

            var imageData = imageDataOpt.get();
            streamImageData(imageData, response);

            log.debug("Successfully served binary image for key: {}", imageKey);
            return null; // No view needed for binary response

        } catch (Exception e) {
            log.error("Error serving binary image", e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error serving image");
            }
            return null;
        }
    }

    /**
     * Initializes the cache manager if not already set.
     * Uses double-checked locking for thread safety.
     */
    private void initializeCacheManager() {
        if (cacheManager == null) {
            synchronized (this) {
                if (cacheManager == null) {
                    cacheManager = ImageCacheManagerLocator.getImageCacheManager();
                    log.debug("Initialized image cache manager");
                }
            }
        }
    }

    /**
     * Extracts the image key from the request URI.
     *
     * @param uri the request URI
     * @return Optional containing the image key, or empty if extraction fails
     */
    private Optional<String> extractImageKeyFromUri(String uri) {
        if (StringUtils.isBlank(uri)) {
            return Optional.empty();
        }

        log.debug("Extracting image key from URI: {}", uri);

        var matcher = KEY_EXTRACTION_PATTERN.matcher(uri);
        if (matcher.find()) {
            var key = matcher.group(1);
            return StringUtils.isNotBlank(key) ? Optional.of(key.trim()) : Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * Retrieves image data from the cache.
     *
     * @param imageKey the image key
     * @return Optional containing the image data, or empty if not found
     */
    private Optional<ImageData> retrieveImageData(String imageKey) {
        if (cacheManager == null) {
            log.error("Cache manager is not initialized");
            return Optional.empty();
        }

        if (!cacheManager.hasImage(imageKey)) {
            return Optional.empty();
        }

        try {
            var imageData = cacheManager.getImage(imageKey);
            return Optional.ofNullable(imageData);
        } catch (Exception e) {
            log.error("Error retrieving image data for key: {}", imageKey, e);
            return Optional.empty();
        }
    }

    /**
     * Streams image data to the HTTP response.
     *
     * @param imageData the image data to stream
     * @param response  the HTTP response
     * @throws IOException if streaming fails
     */
    private void streamImageData(ImageData imageData, HttpServletResponse response) throws IOException {
        var binaryDataOpt = imageData.getBinaryOptional();
        if (binaryDataOpt.isEmpty()) {
            log.warn("Image data contains no binary content");
            response.sendError(HttpServletResponse.SC_NO_CONTENT, "Image contains no data");
            return;
        }

        var binaryData = binaryDataOpt.get();
        var mimeType = Optional.ofNullable(imageData.getMimeType())
                .filter(StringUtils::isNotBlank)
                .orElse(DEFAULT_MIME_TYPE);

        // Set response headers
        response.setContentType(mimeType);
        response.setContentLength(binaryData.length);
        response.setStatus(HttpServletResponse.SC_OK);

        // Add caching headers for better performance
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setDateHeader("Expires", System.currentTimeMillis() + 3600000); // 1 hour

        log.debug("Streaming image data: {} bytes, MIME type: {}", binaryData.length, mimeType);

        // Stream the binary data
        try (var outputStream = response.getOutputStream()) {
            var offset = 0;
            while (offset < binaryData.length) {
                var length = Math.min(BUFFER_SIZE, binaryData.length - offset);
                outputStream.write(binaryData, offset, length);
                offset += length;
            }
            outputStream.flush();
        }

        response.flushBuffer();
    }

    /**
     * Gets the current cache manager.
     *
     * @return the cache manager, may be {@code null}
     */
    public ImageCacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Sets the cache manager for testing purposes.
     *
     * @param cacheManager the cache manager to set
     */
    public void setCacheManager(ImageCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Checks if the controller is properly configured.
     *
     * @return {@code true} if cache manager is available, {@code false} otherwise
     */
    public boolean isConfigured() {
        return cacheManager != null;
    }
}

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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.percussion.widgets.image.data.CachedImageMetaData;
import com.percussion.widgets.image.services.ImageCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;

/**
 * Spring MVC controller for handling image metadata requests.
 * Retrieves cached image metadata and returns it as JSON.
 *
 * @since Java 11
 */
public class ImageRequestController extends ParameterizableViewController implements Controller {

    private static final Logger log = LogManager.getLogger(ImageRequestController.class);

    /** Default model object name for the response */
    private static final String DEFAULT_MODEL_OBJECT_NAME = "results";

    /** Parameter name for image key in requests */
    private static final String IMAGE_KEY_PARAM = "imageKey";

    /** Error message for missing image key */
    private static final String MISSING_KEY_ERROR = "Image key parameter is required";

    /** Error message for image not found */
    private static final String IMAGE_NOT_FOUND_ERROR = "The requested image was not found";

    private String modelObjectName = DEFAULT_MODEL_OBJECT_NAME;
    private ImageCacheManager imageCacheManager;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        var mav = super.handleRequestInternal(request, response);

        try {
            var imageKeyOpt = extractImageKey(request);
            if (imageKeyOpt.isEmpty()) {
                log.error("Image key parameter is missing or blank");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, MISSING_KEY_ERROR);
                return null;
            }

            var imageKey = imageKeyOpt.get();
            log.debug("Processing request for image key: {}", imageKey);

            var imageDataOpt = retrieveImageData(imageKey);
            if (imageDataOpt.isEmpty()) {
                log.info("Image not found for key: {}", imageKey);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, IMAGE_NOT_FOUND_ERROR);
                return null;
            }

            var imageData = imageDataOpt.get();
            var cachedMetadata = new CachedImageMetaData(imageData, imageKey);
            var mapper = JsonMapper.builder().build();
            var json = mapper.convertValue(cachedMetadata, java.util.Map.class);

            mav.addObject(modelObjectName, json);
            log.debug("Successfully processed image request for key: {}", imageKey);

            return mav;

        } catch (Exception e) {
            log.error("Error processing image request", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "An error occurred while processing the image request");
            return null;
        }
    }

    /**
     * Extracts and validates the image key from the request.
     *
     * @param request the HTTP request
     * @return Optional containing the image key, or empty if not valid
     */
    private Optional<String> extractImageKey(HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter(IMAGE_KEY_PARAM))
            .filter(StringUtils::isNotBlank)
            .map(String::trim);
    }

    /**
     * Retrieves image data from the cache manager.
     *
     * @param imageKey the image key
     * @return Optional containing the image data, or empty if not found
     */
    private Optional<com.percussion.widgets.image.data.ImageData> retrieveImageData(String imageKey) {
        if (imageCacheManager == null) {
            log.error("ImageCacheManager is not configured");
            return Optional.empty();
        }

        if (!imageCacheManager.hasImage(imageKey)) {
            return Optional.empty();
        }

        try {
            var imageData = imageCacheManager.getImage(imageKey);
            return Optional.ofNullable(imageData);
        } catch (Exception e) {
            log.error("Error retrieving image data for key: {}", imageKey, e);
            return Optional.empty();
        }
    }

    /**
     * Gets the model object name used for the JSON response.
     *
     * @return the model object name, never {@code null}
     */
    public String getModelObjectName() {
        return modelObjectName;
    }

    /**
     * Sets the model object name for the JSON response.
     *
     * @param modelObjectName the model object name, defaults to "results" if null
     */
    public void setModelObjectName(String modelObjectName) {
        this.modelObjectName = StringUtils.isNotBlank(modelObjectName)
            ? modelObjectName.trim()
            : DEFAULT_MODEL_OBJECT_NAME;
    }

    /**
     * Gets the image cache manager.
     *
     * @return the image cache manager, may be {@code null}
     */
    public ImageCacheManager getImageCacheManager() {
        return imageCacheManager;
    }

    /**
     * Gets the image cache manager as an Optional.
     *
     * @return Optional containing the cache manager, or empty if not set
     */
    public Optional<ImageCacheManager> getImageCacheManagerOptional() {
        return Optional.ofNullable(imageCacheManager);
    }

    /**
     * Sets the image cache manager.
     *
     * @param imageCacheManager the image cache manager to set
     * @throws IllegalArgumentException if imageCacheManager is {@code null}
     */
    public void setImageCacheManager(ImageCacheManager imageCacheManager) {
        this.imageCacheManager = Objects.requireNonNull(imageCacheManager,
            "ImageCacheManager must not be null");
    }

    /**
     * Checks if the controller is properly configured.
     *
     * @return {@code true} if all required dependencies are set, {@code false} otherwise
     */
    public boolean isConfigured() {
        return imageCacheManager != null;
    }
}

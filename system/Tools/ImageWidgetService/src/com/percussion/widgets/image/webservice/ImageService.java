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

package com.percussion.widgets.image.webservice;

import com.percussion.widgets.image.data.CachedImageMetaData;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.services.ImageCacheManager;
import com.percussion.widgets.image.services.ImageResizeManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Core web service for image processing operations.
 * Provides image metadata retrieval, resizing, cropping, and rotation functionality
 * with comprehensive error handling and validation.
 *
 * @since Java 11
 */
public class ImageService {

    private static final Logger log = LogManager.getLogger(ImageService.class);

    private ImageResizeManager resizeManager;
    private ImageCacheManager cacheManager;

    /**
     * Default constructor.
     */
    public ImageService() {
        // Default constructor
    }

    /**
     * Constructor with required dependencies.
     *
     * @param cacheManager the image cache manager, must not be {@code null}
     * @param resizeManager the image resize manager, must not be {@code null}
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public ImageService(ImageCacheManager cacheManager, ImageResizeManager resizeManager) {
        setCacheManager(cacheManager);
        setResizeManager(resizeManager);
    }

    /**
     * Gets image metadata for the specified cache key.
     *
     * @param imageKey the image cache key, must not be {@code null} or blank
     * @return cached image metadata, or {@code null} if not found
     * @throws IllegalArgumentException if imageKey is {@code null} or blank
     */
    public CachedImageMetaData getImageMetadata(String imageKey) {
        if (StringUtils.isBlank(imageKey)) {
            throw new IllegalArgumentException("Image key must not be blank");
        }

        log.debug("Retrieving metadata for image key: {}", imageKey);

        var metadata = cacheManager.getImageMetaData(imageKey);
        if (metadata != null) {
            log.debug("Found metadata for image key: {}", imageKey);
        } else {
            log.debug("No metadata found for image key: {}", imageKey);
        }

        return metadata;
    }

    /**
     * Gets image metadata as an Optional to avoid null handling.
     *
     * @param imageKey the image cache key
     * @return Optional containing the metadata, or empty if not found
     */
    public Optional<CachedImageMetaData> getImageMetadataOptional(String imageKey) {
        try {
            return Optional.ofNullable(getImageMetadata(imageKey));
        } catch (Exception e) {
            log.debug("Failed to retrieve metadata for key '{}': {}", imageKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Resizes an image according to the specified request parameters.
     * Supports resizing, cropping, and rotation operations.
     *
     * @param request the resize request parameters, must not be {@code null}
     * @return cached metadata for the resized image
     * @throws IllegalArgumentException if request is {@code null} or invalid
     * @throws ImageServiceException if image processing fails
     */
    public CachedImageMetaData resizeImage(ResizeImageRequest request) throws ImageServiceException {
        Objects.requireNonNull(request, "Resize request must not be null");

        // Validate request
        var validationError = request.validate();
        if (validationError.isPresent()) {
            throw new IllegalArgumentException("Invalid resize request: " + validationError.get());
        }

        var imageKey = request.getImageKey();
        log.debug("Processing resize request for image key: {}", imageKey);

        try {
            // Get original image data
            var originalImageOpt = cacheManager.getImageOptional(imageKey);
            if (originalImageOpt.isEmpty()) {
                throw new ImageServiceException("Image not found for key: " + imageKey);
            }

            var originalImage = originalImageOpt.get();
            var binaryDataOpt = originalImage.getBinaryOptional();
            if (binaryDataOpt.isEmpty()) {
                throw new ImageServiceException("Image contains no binary data: " + imageKey);
            }

            // Build resize parameters
            var resizeParams = buildResizeParameters(request);

            // Configure resize manager
            configureResizeManager(originalImage);

            // Perform resize operation
            try (var inputStream = new ByteArrayInputStream(binaryDataOpt.get())) {
                var resizedImage = resizeManager.generateImage(
                    inputStream,
                    resizeParams.cropBox().orElse(null),
                    resizeParams.targetSize().orElse(null),
                    resizeParams.rotation()
                );

                var newKey = cacheManager.addImage(resizedImage);
                log.debug("Successfully resized image, new key: {}", newKey);

                return new CachedImageMetaData(resizedImage, newKey);
            }

        } catch (ImageServiceException e) {
            throw e; // Re-throw service exceptions as-is
        } catch (Exception e) {
            var errorMsg = String.format("Failed to resize image '%s': %s", imageKey, e.getMessage());
            log.error(errorMsg, e);
            throw new ImageServiceException(errorMsg, e);
        }
    }

    /**
     * Builds resize parameters from the request.
     *
     * @param request the resize request
     * @return resize parameters record
     */
    private ResizeParameters buildResizeParameters(ResizeImageRequest request) {
        var targetSize = request.getTargetDimensions();
        var cropBox = request.getCropBox();
        var rotation = request.getRotate();

        if (targetSize.isPresent()) {
            log.debug("Target size: {}", targetSize.get());
        }
        if (cropBox.isPresent()) {
            log.debug("Crop box: {}", cropBox.get());
        }
        if (rotation != 0) {
            log.debug("Rotation: {} degrees", rotation);
        }

        return new ResizeParameters(targetSize, cropBox, rotation);
    }

    /**
     * Configures the resize manager with image metadata.
     *
     * @param imageData the original image data
     */
    private void configureResizeManager(ImageData imageData) {
        Optional.ofNullable(imageData.getExt())
            .filter(StringUtils::isNotBlank)
            .ifPresent(ext -> {
                resizeManager.setExtension(ext);
                resizeManager.setImageFormat(ext);
            });

        Optional.ofNullable(imageData.getMimeType())
            .filter(StringUtils::isNotBlank)
            .ifPresent(resizeManager::setContentType);
    }

    /**
     * Checks if an image exists in the cache.
     *
     * @param imageKey the image cache key
     * @return {@code true} if the image exists, {@code false} otherwise
     */
    public boolean hasImage(String imageKey) {
        if (StringUtils.isBlank(imageKey)) {
            return false;
        }
        return cacheManager.hasImage(imageKey);
    }

    /**
     * Gets the raw image data for the specified key.
     *
     * @param imageKey the image cache key
     * @return Optional containing the image data, or empty if not found
     */
    public Optional<ImageData> getImageData(String imageKey) {
        if (StringUtils.isBlank(imageKey)) {
            return Optional.empty();
        }
        return cacheManager.getImageOptional(imageKey);
    }

    /**
     * Removes an image from the cache.
     *
     * @param imageKey the image cache key to remove
     * @return {@code true} if an image was removed, {@code false} if not found
     */
    public boolean removeImage(String imageKey) {
        if (StringUtils.isBlank(imageKey)) {
            return false;
        }

        var existed = cacheManager.hasImage(imageKey);
        if (existed) {
            cacheManager.removeImage(imageKey);
            log.debug("Removed image with key: {}", imageKey);
        }
        return existed;
    }

    // Getters and setters with validation

    public ImageResizeManager getResizeManager() {
        return resizeManager;
    }

    public void setResizeManager(ImageResizeManager resizeManager) {
        this.resizeManager = Objects.requireNonNull(resizeManager,
            "ImageResizeManager must not be null");
    }

    public ImageCacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(ImageCacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager,
            "ImageCacheManager must not be null");
    }

    /**
     * Checks if the service is properly configured with all required dependencies.
     *
     * @return {@code true} if configured, {@code false} otherwise
     */
    public boolean isConfigured() {
        return resizeManager != null && cacheManager != null;
    }

    /**
     * Record representing resize operation parameters.
     *
     * @param targetSize optional target dimensions
     * @param cropBox optional crop rectangle
     * @param rotation rotation angle in degrees
     */
    private record ResizeParameters(
        Optional<Dimension> targetSize,
        Optional<Rectangle> cropBox,
        int rotation
    ) {}

    /**
     * Exception thrown when image service operations fail.
     *
     * @since Java 11
     */
    public static class ImageServiceException extends Exception {

        private static final long serialVersionUID = 1L;

        public ImageServiceException(String message) {
            super(message);
        }

        public ImageServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public ImageServiceException(Throwable cause) {
            super(cause);
        }
    }
}

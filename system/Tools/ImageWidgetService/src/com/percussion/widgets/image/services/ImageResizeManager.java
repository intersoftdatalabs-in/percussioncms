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

package com.percussion.widgets.image.services;

import com.percussion.widgets.image.data.ImageData;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.InputStream;
import java.util.Optional;

/**
 * Service interface for image resizing operations with support for cropping and rotation.
 * Provides methods to generate resized images from input streams with various transformation options.
 * Implementations should be thread-safe and handle resources properly.
 *
 * @since Java 11
 */
public interface ImageResizeManager {

    /** Rotation constant for 90-degree counter-clockwise rotation */
    int ROTATE_LEFT = -1;

    /** Rotation constant for 90-degree clockwise rotation */
    int ROTATE_RIGHT = 1;

    /** No rotation constant */
    int NO_ROTATION = 0;

    /**
     * Gets the current filename for generated images.
     *
     * @return the filename, may be {@code null}
     */
    String getFileName();

    /**
     * Sets the filename for generated images.
     *
     * @param fileName the filename to set, may be {@code null}
     */
    void setFileName(String fileName);

    /**
     * Gets the current image format (e.g., "JPEG", "PNG").
     *
     * @return the image format, may be {@code null}
     */
    String getImageFormat();

    /**
     * Sets the image format for generated images.
     *
     * @param imageFormat the image format to set (e.g., "JPEG", "PNG"), may be {@code null}
     */
    void setImageFormat(String imageFormat);

    /**
     * Gets the file extension for generated images.
     *
     * @return the file extension without leading dot, may be {@code null}
     */
    String getExtension();

    /**
     * Sets the file extension for generated images.
     *
     * @param extension the file extension to set, may be {@code null}
     */
    void setExtension(String extension);

    /**
     * Gets the MIME content type for generated images.
     *
     * @return the content type, may be {@code null}
     */
    String getContentType();

    /**
     * Sets the MIME content type for generated images.
     *
     * @param contentType the content type to set, may be {@code null}
     */
    void setContentType(String contentType);

    /**
     * Generates an image with the specified transformations.
     *
     * @param inputStream the input stream containing the source image, must not be {@code null}
     * @param cropBox the cropping rectangle, or {@code null} for no cropping
     * @param targetSize the target size for resizing, or {@code null} to keep original size
     * @param rotationSteps the rotation steps (use ROTATE_LEFT, ROTATE_RIGHT, or NO_ROTATION)
     * @return the generated image data, never {@code null}
     * @throws IllegalArgumentException if inputStream is {@code null}
     * @throws RuntimeException if image generation fails
     */
    ImageData generateImage(InputStream inputStream, Rectangle cropBox, Dimension targetSize, int rotationSteps);

    /**
     * Generates an image from the input stream without any transformations.
     *
     * @param inputStream the input stream containing the source image, must not be {@code null}
     * @return the generated image data, never {@code null}
     * @throws IllegalArgumentException if inputStream is {@code null}
     * @throws RuntimeException if image generation fails
     */
    default ImageData generateImage(InputStream inputStream) {
        return generateImage(inputStream, null, null, NO_ROTATION);
    }

    /**
     * Generates an image with cropping and resizing, but no rotation.
     *
     * @param inputStream the input stream containing the source image, must not be {@code null}
     * @param cropBox the cropping rectangle, or {@code null} for no cropping
     * @param targetSize the target size for resizing, or {@code null} to keep original size
     * @return the generated image data, never {@code null}
     * @throws IllegalArgumentException if inputStream is {@code null}
     * @throws RuntimeException if image generation fails
     */
    default ImageData generateImage(InputStream inputStream, Rectangle cropBox, Dimension targetSize) {
        return generateImage(inputStream, cropBox, targetSize, NO_ROTATION);
    }

    /**
     * Gets the filename as an Optional.
     *
     * @return Optional containing the filename, or empty if not set
     */
    default Optional<String> getFileNameOptional() {
        return Optional.ofNullable(getFileName());
    }

    /**
     * Gets the image format as an Optional.
     *
     * @return Optional containing the image format, or empty if not set
     */
    default Optional<String> getImageFormatOptional() {
        return Optional.ofNullable(getImageFormat());
    }

    /**
     * Gets the file extension as an Optional.
     *
     * @return Optional containing the extension, or empty if not set
     */
    default Optional<String> getExtensionOptional() {
        return Optional.ofNullable(getExtension());
    }

    /**
     * Gets the content type as an Optional.
     *
     * @return Optional containing the content type, or empty if not set
     */
    default Optional<String> getContentTypeOptional() {
        return Optional.ofNullable(getContentType());
    }

    /**
     * Validates that the rotation steps value is valid.
     *
     * @param rotationSteps the rotation steps to validate
     * @return {@code true} if valid, {@code false} otherwise
     */
    static boolean isValidRotation(int rotationSteps) {
        return rotationSteps == ROTATE_LEFT || rotationSteps == ROTATE_RIGHT || rotationSteps == NO_ROTATION;
    }
}

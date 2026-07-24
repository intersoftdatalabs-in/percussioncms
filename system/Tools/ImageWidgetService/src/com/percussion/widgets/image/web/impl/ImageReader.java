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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for reading images using standard Java {@link ImageIO}.
 *
 * <p>With TwelveMonkeys ImageIO plugins on the classpath, this handles CMYK JPEGs,
 * Adobe markers, YCCK color spaces, TIFF, WebP, and other formats automatically
 * without requiring Apache Commons Imaging.
 *
 * @author robertjohansen
 * @deprecated Prefer {@link javax.imageio.ImageIO} directly with TwelveMonkeys plugins.
 */
@Deprecated(since = "Java 11", forRemoval = true)
public final class ImageReader {

    private static final Logger log = LogManager.getLogger(ImageReader.class);

    /**
     * Exception thrown when image reading operations fail.
     */
    public static final class ImageReaderException extends Exception {

        private static final long serialVersionUID = 1L;

        /** Constructs a new ImageReaderException with no detail message. */
        public ImageReaderException() {
            super();
        }

        /**
         * Constructs a new ImageReaderException with the specified detail message.
         *
         * @param message the detail message
         */
        public ImageReaderException(String message) {
            super(message);
        }

        /**
         * Constructs a new ImageReaderException with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause the cause
         */
        public ImageReaderException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new ImageReaderException with the specified cause.
         *
         * @param cause the cause
         */
        public ImageReaderException(Throwable cause) {
            super(cause);
        }
    }

    private ImageReader() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Reads an image byte array into a {@link BufferedImage} using Java ImageIO
     * with TwelveMonkeys plugins for extended format support.
     *
     * @param imageByteArray byte array containing image data, must not be {@code null}
     * @return a BufferedImage parsed from the byte array, or {@code null} if no
     *         registered ImageReader can decode the data
     * @throws ImageReaderException if an I/O error occurs while reading
     * @throws IllegalArgumentException if {@code imageByteArray} is {@code null}
     */
    public static BufferedImage read(byte[] imageByteArray) throws ImageReaderException {
        Objects.requireNonNull(imageByteArray, "Image byte array must not be null");

        if (imageByteArray.length == 0) {
            throw new ImageReaderException("Image byte array is empty");
        }

        try (var inputStream = new ByteArrayInputStream(imageByteArray)) {
            var image = ImageIO.read(inputStream);
            if (image == null) {
                log.warn("ImageIO.read returned null - no registered reader could decode the image data");
            } else {
                log.debug("Successfully read image using ImageIO: {}x{}",
                        image.getWidth(), image.getHeight());
            }
            return image;
        } catch (IOException e) {
            var errorMsg = "Failed to read image";
            log.error("{}: {}", errorMsg, e.getMessage());
            log.debug("Image read failure details", e);
            throw new ImageReaderException(errorMsg, e);
        }
    }

    /**
     * Reads image as {@link Optional} to avoid exception handling at call sites.
     *
     * @param imageByteArray byte array containing image data
     * @return Optional containing BufferedImage, or empty if reading fails or returns null
     */
    public static Optional<BufferedImage> readOptional(byte[] imageByteArray) {
        try {
            return Optional.ofNullable(read(imageByteArray));
        } catch (Exception e) {
            log.debug("Failed to read image: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Checks if the image byte array appears to be a valid image format.
     *
     * @param imageBytes the image data to validate
     * @return {@code true} if ImageIO can decode the data
     */
    public static boolean isValidImageData(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return false;
        }
        return readOptional(imageBytes).isPresent();
    }

    /**
     * Estimates memory usage for loading the image as RGBA (4 bytes per pixel).
     *
     * @param imageBytes the image data
     * @return estimated memory usage in bytes, or 0 if it cannot be determined
     */
    public static long estimateMemoryUsage(byte[] imageBytes) {
        return readOptional(imageBytes)
                .map(image -> (long) image.getWidth() * image.getHeight() * 4L)
                .orElse(0L);
    }
}

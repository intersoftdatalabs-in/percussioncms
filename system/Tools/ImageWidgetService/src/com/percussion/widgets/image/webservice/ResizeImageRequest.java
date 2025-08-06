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

package com.percussion.widgets.image.webservice;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/**
 * Web service request object for image resize operations.
 * Contains parameters for resizing, cropping, and rotating images in web service calls.
 * This class provides comprehensive validation and utility methods for image processing requests.
 *
 * @since Java 11
 */
public class ResizeImageRequest {

    private String imageKey;
    private int height = 0;
    private int width = 0;
    private int x = 0;
    private int y = 0;
    private int deltaX = 0;
    private int deltaY = 0;
    private int rotate = 0;

    /**
     * Default constructor with all numeric fields initialized to zero.
     */
    public ResizeImageRequest() {
        // All fields initialized with default values
    }

    /**
     * Constructor with all parameters for complete initialization.
     *
     * @param imageKey the image key, may be {@code null}
     * @param width the target width, must be >= 0
     * @param height the target height, must be >= 0
     * @param x the crop x coordinate, must be >= 0
     * @param y the crop y coordinate, must be >= 0
     * @param deltaX the crop width, must be >= 0
     * @param deltaY the crop height, must be >= 0
     * @param rotate the rotation angle in degrees
     * @throws IllegalArgumentException if any dimension is negative
     */
    public ResizeImageRequest(String imageKey, int width, int height,
                             int x, int y, int deltaX, int deltaY, int rotate) {
        setImageKey(imageKey);
        setWidth(width);
        setHeight(height);
        setX(x);
        setY(y);
        setDeltaX(deltaX);
        setDeltaY(deltaY);
        setRotate(rotate);
    }

    /**
     * Copy constructor for defensive copying.
     *
     * @param other the request to copy, must not be {@code null}
     * @throws IllegalArgumentException if other is {@code null}
     */
    public ResizeImageRequest(ResizeImageRequest other) {
        Objects.requireNonNull(other, "ResizeImageRequest to copy must not be null");

        this.imageKey = other.imageKey;
        this.width = other.width;
        this.height = other.height;
        this.x = other.x;
        this.y = other.y;
        this.deltaX = other.deltaX;
        this.deltaY = other.deltaY;
        this.rotate = other.rotate;
    }

    /**
     * Gets the image cache key.
     *
     * @return the image key, may be {@code null}
     */
    public String getImageKey() {
        return imageKey;
    }

    /**
     * Gets the image key as an Optional.
     *
     * @return Optional containing the image key, or empty if null or blank
     */
    public Optional<String> getImageKeyOptional() {
        return Optional.ofNullable(imageKey)
            .filter(StringUtils::isNotBlank);
    }

    /**
     * Sets the image cache key.
     *
     * @param imageKey the image key to set, may be {@code null}
     */
    public void setImageKey(String imageKey) {
        this.imageKey = StringUtils.isBlank(imageKey) ? null : imageKey.trim();
    }

    /**
     * Gets the target height for resizing.
     *
     * @return the height in pixels, always >= 0
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the target height for resizing.
     *
     * @param height the height in pixels, must be >= 0
     * @throws IllegalArgumentException if height is negative
     */
    public void setHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("Height must not be negative");
        }
        this.height = height;
    }

    /**
     * Gets the target width for resizing.
     *
     * @return the width in pixels, always >= 0
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the target width for resizing.
     *
     * @param width the width in pixels, must be >= 0
     * @throws IllegalArgumentException if width is negative
     */
    public void setWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Width must not be negative");
        }
        this.width = width;
    }

    /**
     * Gets the crop box x coordinate.
     *
     * @return the x coordinate in pixels, always >= 0
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the crop box x coordinate.
     *
     * @param x the x coordinate in pixels, must be >= 0
     * @throws IllegalArgumentException if x is negative
     */
    public void setX(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("X coordinate must not be negative");
        }
        this.x = x;
    }

    /**
     * Gets the crop box y coordinate.
     *
     * @return the y coordinate in pixels, always >= 0
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the crop box y coordinate.
     *
     * @param y the y coordinate in pixels, must be >= 0
     * @throws IllegalArgumentException if y is negative
     */
    public void setY(int y) {
        if (y < 0) {
            throw new IllegalArgumentException("Y coordinate must not be negative");
        }
        this.y = y;
    }

    /**
     * Gets the crop box width (delta X).
     *
     * @return the crop width in pixels, always >= 0
     */
    public int getDeltaX() {
        return deltaX;
    }

    /**
     * Sets the crop box width (delta X).
     *
     * @param deltaX the crop width in pixels, must be >= 0
     * @throws IllegalArgumentException if deltaX is negative
     */
    public void setDeltaX(int deltaX) {
        if (deltaX < 0) {
            throw new IllegalArgumentException("Delta X must not be negative");
        }
        this.deltaX = deltaX;
    }

    /**
     * Gets the crop box height (delta Y).
     *
     * @return the crop height in pixels, always >= 0
     */
    public int getDeltaY() {
        return deltaY;
    }

    /**
     * Sets the crop box height (delta Y).
     *
     * @param deltaY the crop height in pixels, must be >= 0
     * @throws IllegalArgumentException if deltaY is negative
     */
    public void setDeltaY(int deltaY) {
        if (deltaY < 0) {
            throw new IllegalArgumentException("Delta Y must not be negative");
        }
        this.deltaY = deltaY;
    }

    /**
     * Gets the rotation angle in degrees.
     *
     * @return the rotation angle in degrees
     */
    public int getRotate() {
        return rotate;
    }

    /**
     * Sets the rotation angle in degrees.
     * Negative values are allowed for counter-clockwise rotation.
     *
     * @param rotate the rotation angle in degrees
     */
    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    /**
     * Checks if this request has valid resize dimensions.
     *
     * @return {@code true} if width or height is greater than 0
     */
    public boolean hasResizeDimensions() {
        return width > 0 || height > 0;
    }

    /**
     * Checks if this request has crop parameters.
     *
     * @return {@code true} if any crop coordinate is non-zero
     */
    public boolean hasCropParameters() {
        return x > 0 || y > 0 || deltaX > 0 || deltaY > 0;
    }

    /**
     * Checks if this request has a valid crop box (all coordinates positive).
     *
     * @return {@code true} if all crop coordinates are positive
     */
    public boolean hasValidCropBox() {
        return x > 0 && y > 0 && deltaX > 0 && deltaY > 0;
    }

    /**
     * Checks if this request has rotation.
     *
     * @return {@code true} if rotation angle is non-zero
     */
    public boolean hasRotation() {
        return rotate != 0;
    }

    /**
     * Gets the target dimensions as a Dimension object.
     *
     * @return Optional containing Dimension if both width and height > 0, empty otherwise
     */
    public Optional<Dimension> getTargetDimensions() {
        return (width > 0 && height > 0)
            ? Optional.of(new Dimension(width, height))
            : Optional.empty();
    }

    /**
     * Gets the crop box as a Rectangle object.
     *
     * @return Optional containing Rectangle if valid crop box, empty otherwise
     */
    public Optional<Rectangle> getCropBox() {
        return hasValidCropBox()
            ? Optional.of(new Rectangle(x, y, deltaX, deltaY))
            : Optional.empty();
    }

    /**
     * Validates the request parameters.
     *
     * @return Optional containing error message, or empty if valid
     */
    public Optional<String> validate() {
        if (StringUtils.isBlank(imageKey)) {
            return Optional.of("Image key is required");
        }

        if (hasCropParameters() && !hasValidCropBox()) {
            return Optional.of("Invalid crop box parameters - all crop coordinates must be positive");
        }

        return Optional.empty();
    }

    /**
     * Checks if this request is valid.
     *
     * @return {@code true} if the request passes validation
     */
    public boolean isValid() {
        return validate().isEmpty();
    }

    /**
     * Creates a copy of this request.
     *
     * @return new ResizeImageRequest with copied values
     */
    public ResizeImageRequest copy() {
        return new ResizeImageRequest(this);
    }

    /**
     * Creates a builder for constructing ResizeImageRequest instances.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with values from this request.
     *
     * @return new builder instance with copied values
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var that = (ResizeImageRequest) obj;
        return height == that.height &&
               width == that.width &&
               x == that.x &&
               y == that.y &&
               deltaX == that.deltaX &&
               deltaY == that.deltaY &&
               rotate == that.rotate &&
               Objects.equals(imageKey, that.imageKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageKey, height, width, x, y, deltaX, deltaY, rotate);
    }

    @Override
    public String toString() {
        return String.format("ResizeImageRequest{imageKey='%s', size=%dx%d, crop=(%d,%d,%d,%d), rotate=%d}",
            imageKey, width, height, x, y, deltaX, deltaY, rotate);
    }

    /**
     * Builder class for constructing ResizeImageRequest instances.
     * Follows the builder pattern for easy and readable object creation.
     *
     * @since Java 11
     */
    public static class Builder {

        private String imageKey;
        private int height = 0;
        private int width = 0;
        private int x = 0;
        private int y = 0;
        private int deltaX = 0;
        private int deltaY = 0;
        private int rotate = 0;

        /**
         * Default constructor for builder.
         */
        public Builder() {
            // Default constructor
        }

        /**
         * Constructor that initializes builder with values from existing request.
         *
         * @param request the request to copy values from
         */
        public Builder(ResizeImageRequest request) {
            Objects.requireNonNull(request, "ResizeImageRequest must not be null");

            this.imageKey = request.imageKey;
            this.height = request.height;
            this.width = request.width;
            this.x = request.x;
            this.y = request.y;
            this.deltaX = request.deltaX;
            this.deltaY = request.deltaY;
            this.rotate = request.rotate;
        }

        public Builder imageKey(String imageKey) {
            this.imageKey = imageKey;
            return this;
        }

        public Builder dimensions(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder cropBox(int x, int y, int deltaX, int deltaY) {
            this.x = x;
            this.y = y;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            return this;
        }

        public Builder cropBox(Rectangle cropBox) {
            Objects.requireNonNull(cropBox, "Crop box must not be null");
            return cropBox(cropBox.x, cropBox.y, cropBox.width, cropBox.height);
        }

        public Builder rotate(int rotate) {
            this.rotate = rotate;
            return this;
        }

        /**
         * Builds the ResizeImageRequest instance.
         *
         * @return new ResizeImageRequest with configured values
         * @throws IllegalArgumentException if any configured value is invalid
         */
        public ResizeImageRequest build() {
            return new ResizeImageRequest(imageKey, width, height, x, y, deltaX, deltaY, rotate);
        }
    }
}

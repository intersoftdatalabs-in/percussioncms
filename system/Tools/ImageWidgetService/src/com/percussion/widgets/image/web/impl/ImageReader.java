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

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.jpeg.segments.Segment;
import org.apache.commons.imaging.formats.jpeg.segments.UnknownSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for reading and processing image metadata, particularly JPEG images.
 * This class provides methods for extracting image information, checking for Adobe markers,
 * and handling color space conversions.
 *
 * <p><strong>Note:</strong> This class is marked as deprecated and should be replaced
 * with more modern image processing libraries where possible.</p>
 *
 * @author robertjohansen
 * @since Java 11
 * @deprecated Use modern image processing libraries instead
 */
@Deprecated(since = "Java 11", forRemoval = true)
public final class ImageReader {

    private static final Logger log = LogManager.getLogger(ImageReader.class);

    /** Adobe marker bytes for JPEG detection */
    private static final byte[] ADOBE_MARKER = {'A', 'd', 'o', 'b', 'e'};

    /** YCCK transform value in Adobe APP14 segment */
    private static final int YCCK_TRANSFORM_VALUE = 2;

    /** Minimum size for Adobe APP14 segment data */
    private static final int MIN_ADOBE_SEGMENT_SIZE = 12;

    /** JPEG APP14 segment marker */
    private static final int APP14_MARKER = 0xffee;

    /** Position of transform byte in Adobe APP14 segment */
    private static final int TRANSFORM_BYTE_POSITION = 11;

    /**
     * Exception thrown when image reading operations fail.
     *
     * @since Java 11
     */
    public static final class ImageReaderException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new ImageReaderException with no detail message.
         */
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

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ImageReader() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets image information for a byte array containing image data.
     *
     * @param imageByteArray byte array containing image data, must not be {@code null}
     * @return ImageInfo for the byte array
     * @throws ImageReaderException if image reading fails
     * @throws IllegalArgumentException if imageByteArray is {@code null}
     */
    public static ImageInfo getImageInfo(byte[] imageByteArray) throws ImageReaderException {
        Objects.requireNonNull(imageByteArray, "Image byte array must not be null");

        if (imageByteArray.length == 0) {
            throw new ImageReaderException("Image byte array is empty");
        }

        try {
            var imageInfo = Imaging.getImageInfo(imageByteArray);
            log.debug("Successfully extracted image info: {}x{}, format: {}",
                imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getFormat());
            return imageInfo;
        } catch (Exception e) {
            var errorMsg = "Failed to extract image information";
            log.error(errorMsg, e);
            throw new ImageReaderException(errorMsg, e);
        }
    }

    /**
     * Gets image information as an Optional to avoid exception handling.
     *
     * @param imageByteArray byte array containing image data
     * @return Optional containing ImageInfo, or empty if extraction fails
     */
    public static Optional<ImageInfo> getImageInfoOptional(byte[] imageByteArray) {
        try {
            return Optional.of(getImageInfo(imageByteArray));
        } catch (Exception e) {
            log.debug("Failed to extract image info: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Reads image byte array into a BufferedImage using multiple fallback strategies.
     *
     * @param imageByteArray byte array containing image data, must not be {@code null}
     * @return BufferedImage parsed from the byte array
     * @throws ImageReaderException if image reading fails with all strategies
     * @throws IllegalArgumentException if imageByteArray is {@code null}
     */
    public static BufferedImage read(byte[] imageByteArray) throws ImageReaderException {
        Objects.requireNonNull(imageByteArray, "Image byte array must not be null");

        if (imageByteArray.length == 0) {
            throw new ImageReaderException("Image byte array is empty");
        }

        // Try ImageIO first (faster for most formats)
        var imageOptional = tryImageIO(imageByteArray);
        if (imageOptional.isPresent()) {
            return imageOptional.get();
        }

        // Fallback to Apache Commons Imaging
        var commonsImageOptional = tryCommonsImaging(imageByteArray);
        if (commonsImageOptional.isPresent()) {
            return commonsImageOptional.get();
        }

        throw new ImageReaderException("Failed to read image with all available readers");
    }

    /**
     * Attempts to read image using ImageIO.
     *
     * @param imageByteArray the image data
     * @return Optional containing BufferedImage, or empty if reading fails
     */
    private static Optional<BufferedImage> tryImageIO(byte[] imageByteArray) {
        try (var inputStream = new ByteArrayInputStream(imageByteArray)) {
            var image = ImageIO.read(inputStream);
            if (image != null) {
                log.debug("Successfully read image using ImageIO: {}x{}",
                    image.getWidth(), image.getHeight());
                return Optional.of(image);
            }
        } catch (Exception e) {
            log.debug("ImageIO failed to read image: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Attempts to read image using Apache Commons Imaging.
     *
     * @param imageByteArray the image data
     * @return Optional containing BufferedImage, or empty if reading fails
     */
    private static Optional<BufferedImage> tryCommonsImaging(byte[] imageByteArray) {
        try {
            var image = Imaging.getBufferedImage(imageByteArray);
            if (image != null) {
                log.debug("Successfully read image using Commons Imaging: {}x{}",
                    image.getWidth(), image.getHeight());
                return Optional.of(image);
            }
        } catch (Exception e) {
            log.debug("Commons Imaging failed to read image: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Reads image as Optional to avoid exception handling.
     *
     * @param imageByteArray byte array containing image data
     * @return Optional containing BufferedImage, or empty if reading fails
     */
    public static Optional<BufferedImage> readOptional(byte[] imageByteArray) {
        try {
            return Optional.of(read(imageByteArray));
        } catch (Exception e) {
            log.debug("Failed to read image: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Checks if a JPEG byte array contains the Adobe marker.
     *
     * @param imageBytes byte array containing JPEG data, must not be {@code null}
     * @return {@code true} if Adobe marker is present, {@code false} otherwise
     * @throws IOException if I/O error occurs
     * @throws ImageReadException if image parsing fails
     * @throws IllegalArgumentException if imageBytes is {@code null}
     */
    public static boolean hasAdobeMarker(byte[] imageBytes) throws IOException, ImageReadException {
        Objects.requireNonNull(imageBytes, "Image bytes must not be null");

        var segmentsOptional = getSegmentsOptional(imageBytes);
        if (segmentsOptional.isEmpty()) {
            return false;
        }

        var segments = segmentsOptional.get();
        if (segments.isEmpty()) {
            return false;
        }

        var app14Segment = (UnknownSegment) segments.get(0);
        var data = app14Segment.getSegmentData();

        return hasAdobeMarkerInData(data);
    }

    /**
     * Checks if segment data contains Adobe marker.
     *
     * @param data the segment data
     * @return {@code true} if Adobe marker is present
     */
    private static boolean hasAdobeMarkerInData(byte[] data) {
        if (data.length < MIN_ADOBE_SEGMENT_SIZE) {
            return false;
        }

        // Check for Adobe marker bytes
        for (int i = 0; i < ADOBE_MARKER.length; i++) {
            if (data[i] != ADOBE_MARKER[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a JPEG byte array uses YCCK color space.
     *
     * @param imageBytes byte array containing JPEG data, must not be {@code null}
     * @return {@code true} if YCCK color space is used, {@code false} otherwise
     * @throws IOException if I/O error occurs
     * @throws ImageReadException if image parsing fails
     * @throws IllegalArgumentException if imageBytes is {@code null}
     */
    public static boolean isYcck(byte[] imageBytes) throws IOException, ImageReadException {
        Objects.requireNonNull(imageBytes, "Image bytes must not be null");

        var segmentsOptional = getSegmentsOptional(imageBytes);
        if (segmentsOptional.isEmpty()) {
            return false;
        }

        var segments = segmentsOptional.get();
        if (segments.isEmpty()) {
            return false;
        }

        var app14Segment = (UnknownSegment) segments.get(0);
        var data = app14Segment.getSegmentData();

        if (!hasAdobeMarkerInData(data)) {
            return false;
        }

        var transform = data[TRANSFORM_BYTE_POSITION] & 0xff;
        return transform == YCCK_TRANSFORM_VALUE;
    }

    /**
     * Gets JPEG segments as Optional to avoid exception handling.
     *
     * @param imageBytes the JPEG data
     * @return Optional containing list of segments, or empty if extraction fails
     */
    private static Optional<List<Segment>> getSegmentsOptional(byte[] imageBytes) {
        try {
            var segments = getSegments(imageBytes);
            return Optional.ofNullable(segments);
        } catch (Exception e) {
            log.debug("Failed to extract JPEG segments: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets a list of segments from a JPEG byte array.
     *
     * @param imageBytes byte array containing JPEG data
     * @return list of segments
     * @throws ImageReadException if image parsing fails
     * @throws IOException if I/O error occurs
     */
    private static List<Segment> getSegments(byte[] imageBytes)
            throws ImageReadException, IOException {
        var parser = new JpegImageParser();

        try (var inputStream = new ByteArrayInputStream(imageBytes)) {
            var byteSource = new ByteSourceInputStream(inputStream, "");
            return parser.readSegments(byteSource, new int[]{APP14_MARKER}, true);
        }
    }

    /**
     * Handles color inversion for Adobe-based JPEGs.
     * This method inverts colors in CMYK images that use Adobe color encoding.
     *
     * @param raster the writable raster to process, must not be {@code null}
     * @throws IllegalArgumentException if raster is {@code null}
     */
    public static void convertInvertedColors(WritableRaster raster) {
        Objects.requireNonNull(raster, "Raster must not be null");

        var width = raster.getWidth();
        var height = raster.getHeight();
        var numBands = raster.getNumBands();

        log.debug("Converting inverted colors for raster: {}x{}, {} bands",
            width, height, numBands);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int b = 0; b < numBands; b++) {
                    var sample = raster.getSample(x, y, b);
                    raster.setSample(x, y, b, 255 - sample);
                }
            }
        }

        log.debug("Color inversion completed");
    }

    /**
     * Converts CMYK image to RGB color space.
     *
     * @param cmykImage the CMYK image to convert, must not be {@code null}
     * @return RGB BufferedImage
     * @throws IllegalArgumentException if cmykImage is {@code null}
     */
    public static BufferedImage convertCMYKtoRGB(BufferedImage cmykImage) {
        Objects.requireNonNull(cmykImage, "CMYK image must not be null");

        var rgbImage = new BufferedImage(
            cmykImage.getWidth(),
            cmykImage.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );

        var colorConvert = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            null
        );

        colorConvert.filter(cmykImage, rgbImage);

        log.debug("Converted CMYK image to RGB: {}x{}",
            rgbImage.getWidth(), rgbImage.getHeight());

        return rgbImage;
    }

    /**
     * Checks if the image byte array appears to be a valid image format.
     *
     * @param imageBytes the image data to validate
     * @return {@code true} if the data appears to be a valid image
     */
    public static boolean isValidImageData(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return false;
        }

        return getImageInfoOptional(imageBytes).isPresent();
    }

    /**
     * Gets the estimated memory usage for loading the image.
     *
     * @param imageBytes the image data
     * @return estimated memory usage in bytes, or 0 if cannot be determined
     */
    public static long estimateMemoryUsage(byte[] imageBytes) {
        return getImageInfoOptional(imageBytes)
            .map(info -> (long) info.getWidth() * info.getHeight() * 4) // Assume 4 bytes per pixel (RGBA)
            .orElse(0L);
    }
}

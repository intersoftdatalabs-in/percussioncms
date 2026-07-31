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
import com.percussion.server.PSServer;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.widgets.image.data.CachedImageMetaData;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.data.MimeUtils;
import com.percussion.widgets.image.services.ImageCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Spring REST controller for handling binary image file uploads.
 * Supports multipart file uploads with image validation, processing, and caching.
 *
 * @since Java 11
 */
@RestController
@PSBaseBean("imageWidgetBinaryUpload")
public class BinaryUploadController {

    private static final Logger log = LogManager.getLogger(BinaryUploadController.class);

    /** Default model object name for responses */
    private static final String DEFAULT_MODEL_OBJECT_NAME = "results";

    /** Default view name for JSON responses */
    private static final String DEFAULT_VIEW_NAME = "imageWidgetJSONView";

    /** Default thumbnail width */
    private static final String DEFAULT_THUMB_WIDTH = "50";

    /** Maximum file size in bytes (10MB) */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** Server property key for thumbnail width */
    private static final String THUMB_WIDTH_PROPERTY = "imageThumbnailWidth";

    // Error message templates
    private static final String MESSAGE_BAD_CONTENT_TYPE = "Invalid or unsupported image type \"{0}\"";
    private static final String MESSAGE_UNABLE_TO_COMPUTE_SIZE = "Possibly invalid image. Unable to determine image height and width.";
    private static final String MESSAGE_FILE_TOO_LARGE = "File size exceeds maximum allowed size of {0} MB";
    private static final String MESSAGE_EMPTY_FILE = "Uploaded file is empty";
    private static final String MESSAGE_NO_FILES = "No files found in request";

    @Autowired
    private ImageCacheManager cacheManager;

    private String modelObjectName = DEFAULT_MODEL_OBJECT_NAME;
    private String viewName = DEFAULT_VIEW_NAME;

    /**
     * Handles multipart file upload requests for images.
     *
     * @param request the HTTP request containing multipart data
     * @param response the HTTP response
     * @return ModelAndView containing upload results or error information
     * @throws PSBinaryUploadException if upload processing fails
     */
    @RequestMapping("/imageWidget/upload")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response)
            throws PSBinaryUploadException {

        var modelAndView = new ModelAndView(getViewName());

        // Add flash attributes if present
        Optional.ofNullable(RequestContextUtils.getInputFlashMap(request))
            .ifPresent(modelAndView::addAllObjects);

        try {
            var results = buildResults(request);
            modelAndView.addObject(getModelObjectName(), results);

            log.debug("Successfully processed {} file(s)", results.size());

        } catch (Exception ex) {
            var errorMessage = "Unexpected exception during file upload: " +
                Optional.ofNullable(ex.getMessage()).orElse("Unknown error");

            log.error("File upload failed", ex);

            var errorResponse = createErrorResponse(errorMessage);
            modelAndView.addObject(getModelObjectName(), errorResponse);
        }

        return modelAndView;
    }

    /**
     * Builds the results array from multipart request files.
     *
     * @param request the HTTP request
     * @return List containing upload results
     * @throws PSBinaryUploadException if processing fails
     */
    protected List<Object> buildResults(HttpServletRequest request) throws PSBinaryUploadException {
        var results = new ArrayList<Object>();
        var mapper = JsonMapper.builder().build();

        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new PSBinaryUploadException("Request is not a multipart request");
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        log.debug("Processing multipart form request");

        var fileMap = multipartRequest.getFileMap();
        if (fileMap.isEmpty()) {
            throw new PSBinaryUploadException(MESSAGE_NO_FILES);
        }

        for (var entry : fileMap.entrySet()) {
            var mpFile = entry.getValue();
            var filename = Optional.ofNullable(mpFile.getOriginalFilename())
                .filter(StringUtils::isNotBlank)
                .orElse("unknown");

            log.debug("Processing file: {}", filename);

            try {
                var validationResult = validateFile(mpFile);
                if (validationResult.isPresent()) {
                    results.add(buildError(validationResult.get()));
                    continue;
                }

                var cachedData = storeImage(mpFile);
                var json = mapper.convertValue(cachedData, Map.class);
                results.add(json);

                log.debug("Successfully processed file: {}", filename);

            } catch (Exception ex) {
                var errorMsg = String.format("Failed to process file '%s': %s",
                    filename, ex.getMessage());
                log.warn(errorMsg, ex);
                results.add(buildError(errorMsg));
            }
        }

        return results;
    }

    /**
     * Validates the uploaded file.
     *
     * @param file the multipart file to validate
     * @return Optional containing error message, or empty if valid
     */
    private Optional<String> validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            return Optional.of(MESSAGE_EMPTY_FILE);
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            var maxSizeMB = MAX_FILE_SIZE / (1024 * 1024);
            return Optional.of(MessageFormat.format(MESSAGE_FILE_TOO_LARGE, maxSizeMB));
        }

        // Validate MIME type
        var mimeType = file.getContentType();
        if (StringUtils.isBlank(mimeType) || !isValidImageMimeType(mimeType)) {
            return Optional.of(MessageFormat.format(MESSAGE_BAD_CONTENT_TYPE, mimeType));
        }

        return Optional.empty();
    }

    /**
     * Checks if the MIME type is a valid image type.
     *
     * @param mimeType the MIME type to check
     * @return {@code true} if valid image MIME type
     */
    private boolean isValidImageMimeType(String mimeType) {
        return mimeType.toLowerCase().startsWith("image/") &&
               MimeUtils.isSupportedMimeType(mimeType);
    }

    /**
     * Creates an error JSON object.
     *
     * @param message the error message
     * @return Map error object
     */
    protected Map<String, Object> buildError(String message) {
        var json = new HashMap<String, Object>();
        json.put("error", message);
        json.put("success", false);
        json.put("timestamp", System.currentTimeMillis());
        return json;
    }

    /**
     * Creates an error response for the entire request.
     *
     * @param message the error message
     * @return Map error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        var errorJson = new HashMap<String, Object>();
        errorJson.put("error", message);
        errorJson.put("success", false);
        errorJson.put("timestamp", System.currentTimeMillis());
        return errorJson;
    }

    /**
     * Stores the uploaded image in cache and creates metadata.
     *
     * @param mpFile the multipart file to store
     * @return cached image metadata
     * @throws PSBinaryUploadException if storage fails
     */
    protected CachedImageMetaData storeImage(MultipartFile mpFile) throws PSBinaryUploadException {
        Objects.requireNonNull(mpFile, "Multipart file must not be null");

        var imageData = new ImageData();

        try {
            // Set binary data
            imageData.setBinary(mpFile.getBytes());
            imageData.setSize(mpFile.getSize());

            // Set file information
            extractFileInformation(mpFile, imageData);

            // Extract image dimensions
            extractImageDimensions(mpFile, imageData);

            // Store in cache
            var key = cacheManager.addImage(imageData);
            log.debug("Stored image with cache key: {}", key);

            return new CachedImageMetaData(imageData, key);

        } catch (IOException e) {
            throw new PSBinaryUploadException("Failed to read file data", e);
        } catch (Exception e) {
            throw new PSBinaryUploadException("Failed to store image", e);
        }
    }

    /**
     * Extracts file information from multipart file.
     *
     * @param mpFile the multipart file
     * @param imageData the image data to populate
     */
    private void extractFileInformation(MultipartFile mpFile, ImageData imageData) {
        // Set filename and extension
        Optional.ofNullable(mpFile.getOriginalFilename())
            .filter(StringUtils::isNotBlank)
            .ifPresent(filename -> {
                imageData.setFilename(filename);

                // Extract extension
                var ext = StringUtils.substringAfterLast(filename, ".");
                if (StringUtils.isNotBlank(ext)) {
                    imageData.setExt(ext);
                }
            });

        // Set MIME type
        Optional.ofNullable(mpFile.getContentType())
            .filter(StringUtils::isNotBlank)
            .ifPresent(imageData::setMimeType);
    }

    /**
     * Extracts image dimensions from the file.
     *
     * @param mpFile the multipart file
     * @param imageData the image data to populate
     * @throws PSBinaryUploadException if dimension extraction fails
     */
    private void extractImageDimensions(MultipartFile mpFile, ImageData imageData)
            throws PSBinaryUploadException {

        try (var inputStream = mpFile.getInputStream()) {
            var image = ImageIO.read(inputStream);

            if (image == null) {
                throw new PSBinaryUploadException(MESSAGE_UNABLE_TO_COMPUTE_SIZE);
            }

            imageData.setWidth(image.getWidth());
            imageData.setHeight(image.getHeight());

            // Set thumbnail width from server properties
            var thumbWidth = getConfiguredThumbnailWidth();
            imageData.setThumbWidth(thumbWidth);

            log.debug("Extracted image dimensions: {}x{}, thumb width: {}",
                image.getWidth(), image.getHeight(), thumbWidth);

        } catch (IOException e) {
            throw new PSBinaryUploadException("Failed to read image dimensions", e);
        }
    }

    /**
     * Gets the configured thumbnail width from server properties.
     *
     * @return thumbnail width
     */
    private int getConfiguredThumbnailWidth() {
        return Optional.ofNullable(PSServer.getServerProps())
            .map(props -> props.getProperty(THUMB_WIDTH_PROPERTY, DEFAULT_THUMB_WIDTH))
            .filter(StringUtils::isNotBlank)
            .map(width -> {
                try {
                    return Integer.parseInt(width);
                } catch (NumberFormatException e) {
                    log.warn("Invalid thumbnail width '{}', using default", width);
                    return Integer.parseInt(DEFAULT_THUMB_WIDTH);
                }
            })
            .orElse(Integer.parseInt(DEFAULT_THUMB_WIDTH));
    }

    // Getters and setters with validation

    public ImageCacheManager getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(ImageCacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager,
            "ImageCacheManager must not be null");
    }

    public String getModelObjectName() {
        return modelObjectName;
    }

    public void setModelObjectName(String modelObjectName) {
        this.modelObjectName = StringUtils.isNotBlank(modelObjectName)
            ? modelObjectName.trim()
            : DEFAULT_MODEL_OBJECT_NAME;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = StringUtils.isNotBlank(viewName)
            ? viewName.trim()
            : DEFAULT_VIEW_NAME;
    }

    /**
     * Checks if the controller is properly configured.
     *
     * @return {@code true} if all required dependencies are set
     */
    public boolean isConfigured() {
        return cacheManager != null;
    }
}

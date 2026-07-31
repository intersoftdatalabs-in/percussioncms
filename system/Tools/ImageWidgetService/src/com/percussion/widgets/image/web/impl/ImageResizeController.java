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
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.widgets.image.data.CachedImageMetaData;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.services.ImageCacheManager;
import com.percussion.widgets.image.services.ImageResizeManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Spring MVC controller for handling image resize operations.
 * Processes resize requests with optional cropping and rotation parameters.
 *
 * @since Java 11
 */
@Controller
@RequestMapping("/imageWidget/resizeImage.do")
@PSBaseBean("imageWidgetResize")
public class ImageResizeController {

    private static final Logger log = LogManager.getLogger(ImageResizeController.class);

    /** Default view name for JSON responses */
    private static final String DEFAULT_VIEW_NAME = "imageWidgetJSONView";

    /** Default model object name */
    private static final String DEFAULT_MODEL_OBJECT_NAME = "results";

    private String viewName = DEFAULT_VIEW_NAME;
    private String modelObjectName = DEFAULT_MODEL_OBJECT_NAME;

    @Autowired
    private ImageCacheManager imageCacheManager;

    @Autowired
    private ImageResizeManager imageResizeManager;

    /**
     * Handles POST requests for image resizing operations.
     *
     * @param bean the resize parameters bean
     * @param result binding result for validation
     * @return ModelAndView containing the resize result or error information
     */
    @PostMapping
    public ModelAndView handle(@ModelAttribute("results") ResizeImageBean bean, BindingResult result) {
        var mav = new ModelAndView(viewName);
        var mapper = JsonMapper.builder().build();

        try {
            var validationError = validateResizeBean(bean);
            if (validationError.isPresent()) {
                log.error("Validation failed: {}", validationError.get());
                var errorJson = new java.util.HashMap<String, Object>();
                errorJson.put("error", validationError.get());
                mav.addObject(getModelObjectName(), errorJson);
                return mav;
            }

            var cachedMetadata = resizeImage(bean);
            var json = mapper.convertValue(cachedMetadata, java.util.Map.class);
            mav.addObject(getModelObjectName(), json);

            log.debug("Successfully processed resize request for image key: {}", bean.getImageKey());

        } catch (Exception ex) {
            var errorMessage = "Unexpected exception during image resize: " + PSExceptionUtils.getMessageForLog(ex);
            log.error(errorMessage, ex);

            var errorJson = new java.util.HashMap<String, Object>();
            errorJson.put("error", errorMessage);
            mav.addObject(getModelObjectName(), errorJson);
        }

        return mav;
    }

    /**
     * Validates the resize bean parameters.
     *
     * @param bean the resize bean to validate
     * @return Optional containing error message, or empty if valid
     */
    private Optional<String> validateResizeBean(ResizeImageBean bean) {
        if (bean == null) {
            return Optional.of("Resize bean must not be null");
        }

        if (StringUtils.isBlank(bean.getImageKey())) {
            return Optional.of("Image key is required");
        }

        // Validate crop box parameters - if any are specified, all must be valid
        if (hasCropParameters(bean) && !isValidCropBox(bean)) {
            return Optional.of("Invalid crop box parameters - all crop coordinates must be positive");
        }

        // Validate resize dimensions
        if ((bean.getWidth() < 0) || (bean.getHeight() < 0)) {
            return Optional.of("Width and height must be non-negative");
        }

        return Optional.empty();
    }

    /**
     * Checks if crop parameters are specified.
     */
    private boolean hasCropParameters(ResizeImageBean bean) {
        return bean.getX() != 0 || bean.getY() != 0 || bean.getDeltaX() != 0 || bean.getDeltaY() != 0;
    }

    /**
     * Validates crop box parameters.
     */
    private boolean isValidCropBox(ResizeImageBean bean) {
        return bean.getX() > 0 && bean.getY() > 0 && bean.getDeltaX() > 0 && bean.getDeltaY() > 0;
    }

    /**
     * Performs the image resize operation with optional cropping and rotation.
     *
     * @param bean the resize parameters
     * @return cached image metadata for the resized image
     * @throws Exception if resize operation fails
     */
    protected CachedImageMetaData resizeImage(ResizeImageBean bean) throws Exception {
        var imageKey = bean.getImageKey().trim();
        log.debug("Processing resize for image key: {}", imageKey);

        // Get original image data
        var originalImageOpt = imageCacheManager.getImageOptional(imageKey);
        if (originalImageOpt.isEmpty()) {
            throw new IllegalArgumentException("Image not found for key: " + imageKey);
        }

        var originalImage = originalImageOpt.get();
        var binaryDataOpt = originalImage.getBinaryOptional();
        if (binaryDataOpt.isEmpty()) {
            throw new IllegalArgumentException("Image contains no binary data");
        }

        // Build resize parameters
        var resizeParams = buildResizeParameters(bean);

        // Configure resize manager
        configureResizeManager(originalImage);

        try (var inputStream = new ByteArrayInputStream(binaryDataOpt.get())) {
            var resizedImage = imageResizeManager.generateImage(
                inputStream,
                resizeParams.getCropBox().orElse(null),
                resizeParams.getTargetSize().orElse(null),
                resizeParams.getRotation()
            );

            var newKey = imageCacheManager.addImage(resizedImage);
            log.debug("Created resized image with key: {}", newKey);

            return new CachedImageMetaData(resizedImage, newKey);

        } catch (IllegalArgumentException e) {
            log.warn("Cannot resize image with current format, returning original: {}",
                PSExceptionUtils.getMessageForLog(e));

            // Return original image if resize fails
            var fallbackKey = imageCacheManager.addImage(originalImage);
            return new CachedImageMetaData(originalImage, fallbackKey);
        }
    }

    /**
     * Builds resize parameters from the bean.
     */
    private ResizeParameters buildResizeParameters(ResizeImageBean bean) {
        var targetSize = Optional.<Dimension>empty();
        if (bean.getWidth() > 0 || bean.getHeight() > 0) {
            targetSize = Optional.of(new Dimension(bean.getWidth(), bean.getHeight()));
            log.debug("Target size: {}", targetSize.get());
        }

        var cropBox = Optional.<Rectangle>empty();
        if (isValidCropBox(bean)) {
            cropBox = Optional.of(new Rectangle(bean.getX(), bean.getY(), bean.getDeltaX(), bean.getDeltaY()));
            log.debug("Crop box: {}", cropBox.get());
        }

        var rotation = bean.getRotate();
        if (rotation != 0) {
            log.debug("Rotation: {} degrees", rotation);
        }

        return new ResizeParameters(targetSize, cropBox, rotation);
    }

    /**
     * Configures the resize manager with image metadata.
     */
    private void configureResizeManager(ImageData imageData) {
        Optional.ofNullable(imageData.getExt())
            .filter(StringUtils::isNotBlank)
            .ifPresent(ext -> {
                imageResizeManager.setExtension(ext);
                imageResizeManager.setImageFormat(ext);
            });

        Optional.ofNullable(imageData.getMimeType())
            .filter(StringUtils::isNotBlank)
            .ifPresent(imageResizeManager::setContentType);
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = StringUtils.isNotBlank(viewName) ? viewName.trim() : DEFAULT_VIEW_NAME;
    }

    public String getModelObjectName() {
        return modelObjectName;
    }

    public void setModelObjectName(String modelObjectName) {
        this.modelObjectName = StringUtils.isNotBlank(modelObjectName)
            ? modelObjectName.trim()
            : DEFAULT_MODEL_OBJECT_NAME;
    }

    public ImageCacheManager getImageCacheManager() {
        return imageCacheManager;
    }

    public void setImageCacheManager(ImageCacheManager imageCacheManager) {
        this.imageCacheManager = Objects.requireNonNull(imageCacheManager,
            "ImageCacheManager must not be null");
    }

    public ImageResizeManager getImageResizeManager() {
        return imageResizeManager;
    }

    public void setImageResizeManager(ImageResizeManager imageResizeManager) {
        this.imageResizeManager = Objects.requireNonNull(imageResizeManager,
            "ImageResizeManager must not be null");
    }

    /**
     * POJO representing resize parameters.
     */
    private static class ResizeParameters {
        private final Optional<Dimension> targetSize;
        private final Optional<Rectangle> cropBox;
        private final int rotation;

        public ResizeParameters(Optional<Dimension> targetSize, Optional<Rectangle> cropBox, int rotation) {
            this.targetSize = targetSize;
            this.cropBox = cropBox;
            this.rotation = rotation;
        }

        public Optional<Dimension> getTargetSize() {
            return targetSize;
        }

        public Optional<Rectangle> getCropBox() {
            return cropBox;
        }

        public int getRotation() {
            return rotation;
        }
    }
}

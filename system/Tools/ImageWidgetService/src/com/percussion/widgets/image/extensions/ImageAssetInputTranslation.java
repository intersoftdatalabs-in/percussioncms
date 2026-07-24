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

package com.percussion.widgets.image.extensions;
import com.percussion.data.PSConversionException;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSItemInputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionParams;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.server.PSServer;
import com.percussion.tools.PSCopyStream;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.services.ImageCacheManager;
import com.percussion.widgets.image.services.ImageCacheManagerLocator;
import com.percussion.widgets.image.services.ImageResizeManager;
import com.percussion.widgets.image.services.ImageResizeManagerLocator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static com.percussion.cms.IPSConstants.FALSE;

/**
 * Percussion CMS extension for processing image assets during input operations.
 * Handles image and thumbnail generation with comprehensive validation and error handling.
 * This extension implements the IPSItemInputTransformer interface to process uploaded images
 * and generate thumbnails automatically.
 *
 * @since Java 11
 */
public class ImageAssetInputTranslation extends PSDefaultExtension implements IPSItemInputTransformer {

    private static final Logger log = LogManager.getLogger(ImageAssetInputTranslation.class);

    /** Default parameter names */
    private static final String DEFAULT_IMAGE_PARAM = "img";
    private static final String DEFAULT_THUMB_PARAM = "img2";

    /** File name suffixes */
    private static final String FILENAME_SUFFIX = "_filename";
    private static final String TYPE_SUFFIX = "_type";
    private static final String ID_SUFFIX = "_id";
    private static final String DIRTY_SUFFIX = "_dirty";

    /** Default thumbnail width from server properties */
    private static final String THUMB_WIDTH_PROPERTY = "imageThumbnailWidth";
    private static final String DEFAULT_THUMB_WIDTH = "50";

    private volatile ImageCacheManager cacheManager;
    private volatile ImageResizeManager resizeManager;

    @Override
    public void init(IPSExtensionDef def, File file) throws PSExtensionException {
        super.init(def, file);
        initializeServices();
        log.debug("ImageAssetInputTranslation extension initialized");
    }

    /**
     * Initializes the image processing services.
     */
    private void initializeServices() {
        if (cacheManager == null) {
            cacheManager = ImageCacheManagerLocator.getImageCacheManager();
        }
        if (resizeManager == null) {
            resizeManager = ImageResizeManagerLocator.getImageResizeManager();
        }
    }

    @Override
    public void preProcessRequest(Object[] params, IPSRequestContext request)
            throws PSAuthorizationException, PSRequestValidationException,
                   PSParameterMismatchException, PSExtensionProcessingException {

        Objects.requireNonNull(request, "Request context must not be null");

        try {
            var extensionParams = new PSExtensionParams(params);
            var processingContext = createProcessingContext(extensionParams, request);

            log.debug("Processing image asset input for image: {}, thumbnail: {}",
                processingContext.getImageName(), processingContext.getThumbName());

            processImageAssets(processingContext, request);

        } catch (PSExtensionProcessingException e) {
            throw e; // Re-throw extension processing exceptions as-is
        } catch (Exception ex) {
            var errorMsg = "Unexpected exception during image asset processing: " +
                PSExceptionUtils.getMessageForLog(ex);
            log.error(errorMsg, ex);
            throw new PSExtensionProcessingException(getClass().getName(), ex);
        }
    }

    /**
     * Creates a processing context from extension parameters and request.
     *
     * @param params the extension parameters
     * @param request the request context
     * @return processing context record
     */
    private ProcessingContext createProcessingContext(PSExtensionParams params, IPSRequestContext request) throws PSConversionException {
        var imageName = params.getStringParam(0, DEFAULT_IMAGE_PARAM, false);
        var thumbName = params.getStringParam(1, DEFAULT_THUMB_PARAM, false);

        var imageFileName = request.getParameter(imageName + FILENAME_SUFFIX);
        var thumbFileName = Optional.ofNullable(request.getParameter(thumbName + FILENAME_SUFFIX))
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> getThumbnailFileName(imageFileName));

        return new ProcessingContext(imageName, thumbName, imageFileName, thumbFileName);
    }

    /**
     * Processes image assets based on whether thumbnail ID exists.
     *
     * @param context the processing context
     * @param request the request context
     * @throws Exception if processing fails
     */
    private void processImageAssets(ProcessingContext context, IPSRequestContext request) throws Exception {
        var thumbIdParam = context.getThumbName() + ID_SUFFIX;

        if (StringUtils.isBlank(request.getParameter(thumbIdParam))) {
            processNewImageUpload(context, request);
        } else {
            processExistingImages(context, request);
        }
    }

    /**
     * Processes a new image upload by generating image and thumbnail.
     *
     * @param context the processing context
     * @param request the request context
     * @throws Exception if processing fails
     */
    private void processNewImageUpload(ProcessingContext context, IPSRequestContext request) throws Exception {
        var imageFile = (PSPurgableTempFile) request.getParameterObject(context.getImageName());

        if (imageFile == null) {
            log.debug("No file found for parameter: {}", context.getImageName());
            return;
        }

        // Set source metadata if missing
        updateFileMetadata(imageFile, context, request);

        var mimeType = request.getParameter(context.getImageName() + TYPE_SUFFIX);

        // Generate and update both image and thumbnail
        var generatedImage = generateImage(imageFile, mimeType);
        var generatedThumbnail = generateThumbnail(imageFile);

        updateRequest(request, context.getImageName(), generatedImage);
        updateRequest(request, context.getThumbName(), generatedThumbnail);

        log.debug("Successfully processed new image upload for: {}", context.getImageName());
    }

    /**
     * Processes existing images that may have been modified.
     *
     * @param context the processing context
     * @param request the request context
     * @throws Exception if processing fails
     */
    private void processExistingImages(ProcessingContext context, IPSRequestContext request) throws Exception {
        processInputImage(request, context.getImageName());
        processInputImage(request, context.getThumbName());
    }

    /**
     * Updates file metadata if missing.
     *
     * @param imageFile the image file
     * @param context the processing context
     * @param request the request context
     */
    private void updateFileMetadata(PSPurgableTempFile imageFile, ProcessingContext context,
                                   IPSRequestContext request) {
        if (StringUtils.isEmpty(imageFile.getSourceFileName())) {
            imageFile.setSourceFileName(context.getImageFileName());
        }

        if (StringUtils.isEmpty(imageFile.getSourceContentType())) {
            var contentType = request.getParameter(context.getImageName() + TYPE_SUFFIX);
            imageFile.setSourceContentType(contentType);
        }
    }

    /**
     * Processes an individual input image if it has been marked as dirty.
     *
     * @param request the request context
     * @param baseName the base parameter name
     * @throws Exception if processing fails
     */
    protected void processInputImage(IPSRequestContext request, String baseName) throws Exception {
        var dirtyParam = baseName + DIRTY_SUFFIX;
        var isDirty = Optional.ofNullable(request.getParameter(dirtyParam))
            .filter(StringUtils::isNotBlank)
            .orElse(FALSE);

        if (FALSE.equals(isDirty)) {
            log.debug("Image {} is not dirty, skipping processing", baseName);
            return;
        }

        var imageKey = request.getParameter(baseName + ID_SUFFIX);
        if (StringUtils.isBlank(imageKey)) {
            log.debug("Image key is blank for {}", baseName);
            return;
        }

        var imageDataOpt = cacheManager.getImageOptional(imageKey);
        if (imageDataOpt.isEmpty()) {
            log.warn("Image data not found for key: {}", imageKey);
            return;
        }

        var imageData = imageDataOpt.get();
        log.debug("Processing dirty image: {} with key: {}", baseName, imageKey);

        // Generate physical file from cached image data
        var tempFile = createTempFileFromImageData(imageData);
        updateRequest(request, baseName, tempFile);
    }

    /**
     * Creates a temporary file from cached image data.
     *
     * @param imageData the image data
     * @return temporary file containing the image
     * @throws IOException if file creation fails
     */
    private PSPurgableTempFile createTempFileFromImageData(ImageData imageData) throws IOException {
        var binaryDataOpt = imageData.getBinaryOptional();
        if (binaryDataOpt.isEmpty()) {
            throw new IOException("Image data contains no binary content");
        }

        var tempFile = new PSPurgableTempFile("img", ".tmp", null);

        try (var fos = new FileOutputStream(tempFile);
             var bis = new ByteArrayInputStream(binaryDataOpt.get())) {

            PSCopyStream.copyStream(bis, fos);

            // Set metadata
            Optional.ofNullable(imageData.getFilename())
                .filter(StringUtils::isNotBlank)
                .ifPresent(tempFile::setSourceFileName);

            Optional.ofNullable(imageData.getMimeType())
                .filter(StringUtils::isNotBlank)
                .ifPresent(tempFile::setSourceContentType);

            log.debug("Created temporary file from image data: {} bytes", imageData.getSize());
            return tempFile;
        }
    }

    /**
     * Generates the main image processing result.
     *
     * @param imageFile the source image file
     * @param mimeType the MIME type
     * @return processed image file
     * @throws Exception if generation fails
     */
    protected PSPurgableTempFile generateImage(PSPurgableTempFile imageFile, String mimeType) throws Exception {
        Objects.requireNonNull(imageFile, "Image file must not be null");

        log.debug("Generating image from file: {}", imageFile.getSourceFileName());

        // For now, return the original file - can be extended for image processing
        return imageFile;
    }

    /**
     * Generates a thumbnail from the source image.
     *
     * @param imageFile the source image file
     * @return thumbnail image file
     * @throws Exception if thumbnail generation fails
     */
    protected PSPurgableTempFile generateThumbnail(PSPurgableTempFile imageFile) throws Exception {
        Objects.requireNonNull(imageFile, "Image file must not be null");

        log.debug("Generating thumbnail from file: {}", imageFile.getSourceFileName());

        var thumbnailWidth = getThumbnailWidth();

        try (var inputStream = new FileInputStream(imageFile)) {
            var originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new IOException("Cannot read image data");
            }

            var thumbnailImage = createThumbnail(originalImage, thumbnailWidth);
            return saveImageToTempFile(thumbnailImage, imageFile);
        }
    }

    /**
     * Creates a thumbnail image with the specified width while maintaining aspect ratio.
     *
     * @param originalImage the original image
     * @param thumbnailWidth the desired thumbnail width
     * @return thumbnail BufferedImage
     */
    private BufferedImage createThumbnail(BufferedImage originalImage, int thumbnailWidth) {
        var originalWidth = originalImage.getWidth();
        var originalHeight = originalImage.getHeight();

        var aspectRatio = (double) originalHeight / originalWidth;
        var thumbnailHeight = (int) (thumbnailWidth * aspectRatio);

        var thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        var graphics = thumbnail.createGraphics();

        // Enable high-quality rendering
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        graphics.dispose();

        log.debug("Created thumbnail: {}x{} from original: {}x{}",
            thumbnailWidth, thumbnailHeight, originalWidth, originalHeight);

        return thumbnail;
    }

    /**
     * Saves a BufferedImage to a temporary file.
     *
     * @param image the image to save
     * @param originalFile the original file for metadata reference
     * @return temporary file containing the image
     * @throws IOException if saving fails
     */
    private PSPurgableTempFile saveImageToTempFile(BufferedImage image, PSPurgableTempFile originalFile)
            throws IOException {

        var tempFile = new PSPurgableTempFile("thumb", ".jpg", null);

        try (var fos = new FileOutputStream(tempFile)) {
            if (!ImageIO.write(image, "JPEG", fos)) {
                throw new IOException("Failed to write thumbnail image");
            }

            // Copy metadata from original
            tempFile.setSourceFileName(getThumbnailFileName(originalFile.getSourceFileName()));
            tempFile.setSourceContentType("image/jpeg");

            return tempFile;
        }
    }

    /**
     * Gets the configured thumbnail width from server properties.
     *
     * @return thumbnail width in pixels
     */
    private int getThumbnailWidth() {
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

    /**
     * Generates a thumbnail filename from the original filename.
     *
     * @param originalFilename the original filename
     * @return thumbnail filename
     */
    protected String getThumbnailFileName(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            return "thumbnail.jpg";
        }

        var baseName = FilenameUtils.getBaseName(originalFilename);
        var extension = FilenameUtils.getExtension(originalFilename);

        if (StringUtils.isBlank(extension)) {
            extension = "jpg";
        }

        return baseName + "_thumb." + extension;
    }

    /**
     * Updates the request with the processed file.
     *
     * @param request the request context
     * @param parameterName the parameter name
     * @param file the processed file
     */
    protected void updateRequest(IPSRequestContext request, String parameterName, PSPurgableTempFile file) {
        request.setParameter(parameterName, file);
        log.debug("Updated request parameter: {} with processed file", parameterName);
    }

    /**
     * POJO representing the processing context for image asset operations.
     */
    private static class ProcessingContext {
        private final String imageName;
        private final String thumbName;
        private final String imageFileName;
        private final String thumbFileName;

        public ProcessingContext(String imageName, String thumbName, String imageFileName, String thumbFileName) {
            this.imageName = imageName;
            this.thumbName = thumbName;
            this.imageFileName = imageFileName;
            this.thumbFileName = thumbFileName;
        }

        public String getImageName() {
            return imageName;
        }

        public String getThumbName() {
            return thumbName;
        }

        public String getImageFileName() {
            return imageFileName;
        }

        public String getThumbFileName() {
            return thumbFileName;
        }
    }
}

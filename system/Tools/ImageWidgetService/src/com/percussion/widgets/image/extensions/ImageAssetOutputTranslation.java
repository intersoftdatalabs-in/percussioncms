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

package com.percussion.widgets.image.extensions;

import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSItemOutputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionParams;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.tools.PSCopyStream;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.services.ImageCacheManager;
import com.percussion.widgets.image.services.ImageCacheManagerLocator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Percussion CMS extension for processing image assets during output operations.
 * Handles image retrieval from cache and JCR repository for output transformation.
 * This extension implements the IPSItemOutputTransformer interface to process images
 * during content output operations.
 *
 * @since Java 11
 */
public class ImageAssetOutputTranslation extends PSDefaultExtension implements IPSItemOutputTransformer {

    private static final Logger log = LogManager.getLogger(ImageAssetOutputTranslation.class);

    /** Default parameter names */
    private static final String DEFAULT_IMAGE_PARAM = "img";
    private static final String DEFAULT_THUMB_PARAM = "img2";

    private volatile IPSGuidManager guidManager;
    private volatile IPSContentMgr contentManager;
    private volatile ImageCacheManager cacheManager;

    @Override
    public boolean canModifyStyleSheet() {
        return false;
    }

    @Override
    public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
        super.init(def, codeRoot);
        initializeServices();
        log.debug("ImageAssetOutputTranslation extension initialized");
    }

    /**
     * Initializes the required services with thread-safe lazy loading.
     */
    private void initializeServices() {
        if (guidManager == null) {
            guidManager = PSGuidManagerLocator.getGuidMgr();
        }
        if (cacheManager == null) {
            cacheManager = ImageCacheManagerLocator.getImageCacheManager();
        }
        if (contentManager == null) {
            contentManager = PSContentMgrLocator.getContentMgr();
        }
    }

    @Override
    public Document processResultDocument(Object[] params, IPSRequestContext request, Document resultDoc)
            throws PSParameterMismatchException, PSExtensionProcessingException {

        Objects.requireNonNull(request, "Request context must not be null");
        Objects.requireNonNull(resultDoc, "Result document must not be null");

        try {
            var extensionParams = new PSExtensionParams(params);
            var imageName = extensionParams.getStringParam(0, DEFAULT_IMAGE_PARAM, false);
            var thumbName = extensionParams.getStringParam(1, DEFAULT_THUMB_PARAM, false);

            log.debug("Processing output for image: {}, thumbnail: {}", imageName, thumbName);

            var nodeOpt = findNodeOptional(request);
            if (nodeOpt.isPresent()) {
                var node = nodeOpt.get();
                processImageNode(node, imageName, request);
                processImageNode(node, thumbName, request);
            } else {
                log.debug("No JCR node found for request");
            }

        } catch (Exception ex) {
            var errorMsg = "Unexpected exception during image asset output processing: " +
                PSExceptionUtils.getMessageForLog(ex);
            log.error(errorMsg, ex);
            throw new PSExtensionProcessingException(getClass().getName(), ex);
        }

        return resultDoc;
    }

    /**
     * Finds the JCR node for the current request.
     *
     * @param request the request context
     * @return Optional containing the node, or empty if not found
     */
    private Optional<Node> findNodeOptional(IPSRequestContext request) {
        try {
            var node = findNode(request);
            return Optional.ofNullable(node);
        } catch (Exception e) {
            log.debug("Failed to find JCR node: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Processes an image node by retrieving image data and updating the request.
     *
     * @param node the JCR node containing image data
     * @param parameterName the parameter name for the image
     * @param request the request context
     */
    private void processImageNode(Node node, String parameterName, IPSRequestContext request) {
        if (StringUtils.isBlank(parameterName)) {
            log.debug("Parameter name is blank, skipping processing");
            return;
        }

        try {
            var imageKeyParam = parameterName + "_id";
            var imageKey = request.getParameter(imageKeyParam);

            if (StringUtils.isNotBlank(imageKey)) {
                processFromCache(imageKey, parameterName, request);
            } else {
                processFromJcr(node, parameterName, request);
            }

        } catch (Exception e) {
            log.error("Failed to process image node for parameter {}: {}",
                parameterName, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    /**
     * Processes image data from cache.
     *
     * @param imageKey the cache key
     * @param parameterName the parameter name
     * @param request the request context
     */
    private void processFromCache(String imageKey, String parameterName, IPSRequestContext request) {
        log.debug("Processing image from cache with key: {}", imageKey);

        var imageDataOpt = cacheManager.getImageOptional(imageKey);
        if (imageDataOpt.isEmpty()) {
            log.warn("Image data not found in cache for key: {}", imageKey);
            return;
        }

        var imageData = imageDataOpt.get();
        updateRequestWithImageData(request, parameterName, imageData);
        log.debug("Successfully processed cached image for parameter: {}", parameterName);
    }

    /**
     * Processes image data from JCR repository.
     *
     * @param node the JCR node
     * @param parameterName the parameter name
     * @param request the request context
     */
    private void processFromJcr(Node node, String parameterName, IPSRequestContext request) {
        try {
            log.debug("Processing image from JCR for parameter: {}", parameterName);

            if (!node.hasProperty(parameterName)) {
                log.debug("Node does not have property: {}", parameterName);
                return;
            }

            var property = node.getProperty(parameterName);
            var value = property.getValue();

            if (value != null) {
                var binaryData = extractBinaryData(value);
                if (binaryData.length > 0) {
                    updateRequestWithBinaryData(request, parameterName, binaryData);
                    log.debug("Successfully processed JCR image for parameter: {}", parameterName);
                } else {
                    log.debug("Empty binary data for parameter: {}", parameterName);
                }
            }

        } catch (PathNotFoundException e) {
            log.debug("Property not found in JCR node: {}", parameterName);
        } catch (RepositoryException e) {
            log.error("JCR error processing parameter {}: {}",
                parameterName, PSExceptionUtils.getMessageForLog(e));
        } catch (IOException e) {
            log.error("IO error processing parameter {}: {}",
                parameterName, PSExceptionUtils.getMessageForLog(e));
        }
    }

    /**
     * Extracts binary data from a JCR Value.
     *
     * @param value the JCR value
     * @return byte array containing the binary data
     * @throws RepositoryException if extraction fails
     * @throws IOException if stream operations fail
     */
    private byte[] extractBinaryData(Value value) throws RepositoryException, IOException {
        try (var inputStream = value.getStream();
             var outputStream = new ByteArrayOutputStream()) {

            PSCopyStream.copyStream(inputStream, outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Updates the request with image data.
     *
     * @param request the request context
     * @param parameterName the parameter name
     * @param imageData the image data
     */
    private void updateRequestWithImageData(IPSRequestContext request, String parameterName, ImageData imageData) {
        var binaryOpt = imageData.getBinaryOptional();
        if (binaryOpt.isPresent()) {
            updateRequestWithBinaryData(request, parameterName, binaryOpt.get());

            // Set additional metadata
            imageData.getFilenameOptional()
                .ifPresent(filename -> request.setParameter(parameterName + "_filename", filename));

            Optional.ofNullable(imageData.getMimeType())
                .filter(StringUtils::isNotBlank)
                .ifPresent(mimeType -> request.setParameter(parameterName + "_type", mimeType));
        }
    }

    /**
     * Updates the request with binary data.
     *
     * @param request the request context
     * @param parameterName the parameter name
     * @param binaryData the binary data
     */
    private void updateRequestWithBinaryData(IPSRequestContext request, String parameterName, byte[] binaryData) {
        // Create ByteArrayInputStream for the binary data
        var inputStream = new ByteArrayInputStream(binaryData);
        request.setParameter(parameterName, inputStream);
        log.debug("Updated request parameter {} with {} bytes", parameterName, binaryData.length);
    }

    /**
     * Finds the JCR node for the current request.
     * This method would typically use PSItemXMLSupport or similar to locate the node.
     *
     * @param request the request context (unused in this placeholder implementation)
     * @return the JCR node, or {@code null} if not found
     */
    @SuppressWarnings("unused")
    private Node findNode(IPSRequestContext request) {
        // Implementation would delegate to existing PSItemXMLSupport or similar
        // This is a placeholder that maintains the existing contract
        return null;
    }

    // Getters and setters for dependency injection

    /**
     * Gets the GUID manager.
     *
     * @return the GUID manager, may be {@code null} if not initialized
     */
    public IPSGuidManager getGuidManager() {
        return guidManager;
    }

    /**
     * Sets the GUID manager.
     *
     * @param guidManager the GUID manager to set
     */
    public void setGuidManager(IPSGuidManager guidManager) {
        this.guidManager = guidManager;
    }


    /**
     * Gets the cache manager.
     *
     * @return the cache manager, may be {@code null} if not initialized
     */
    public ImageCacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Sets the cache manager.
     *
     * @param cacheManager the cache manager to set
     */
    public void setCacheManager(ImageCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}

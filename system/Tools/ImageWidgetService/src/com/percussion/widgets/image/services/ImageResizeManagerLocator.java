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

package com.percussion.widgets.image.services;

import com.percussion.services.PSBaseServiceLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Service locator for {@link ImageResizeManager} instances.
 * Provides thread-safe singleton access to the image resize manager service
 * using the service locator pattern with lazy initialization.
 *
 * @since Java 11
 */
public final class ImageResizeManagerLocator extends PSBaseServiceLocator {

    private static final Logger log = LogManager.getLogger(ImageResizeManagerLocator.class);

    /** Bean name for the image resize manager service */
    private static final String IMAGE_RESIZE_BEAN = "imageWidgetResizeManager";

    /** Volatile reference for thread-safe lazy initialization */
    private static volatile ImageResizeManager imageResizeManager;

    /** Lock object for synchronization */
    private static final Object LOCK = new Object();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ImageResizeManagerLocator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the singleton instance of ImageResizeManager.
     * Uses double-checked locking pattern for thread-safe lazy initialization.
     *
     * @return the ImageResizeManager instance, never {@code null}
     * @throws IllegalStateException if the service cannot be located or initialized
     */
    public static ImageResizeManager getImageResizeManager() {
        if (imageResizeManager == null) {
            synchronized (LOCK) {
                if (imageResizeManager == null) {
                    try {
                        var manager = (ImageResizeManager) getBean(IMAGE_RESIZE_BEAN);
                        if (manager == null) {
                            throw new IllegalStateException(
                                "ImageResizeManager bean '" + IMAGE_RESIZE_BEAN + "' not found in application context");
                        }
                        imageResizeManager = manager;
                        log.debug("Initialized ImageResizeManager: {}", imageResizeManager.getClass().getSimpleName());
                    } catch (Exception e) {
                        var errorMsg = "Failed to initialize ImageResizeManager from bean: " + IMAGE_RESIZE_BEAN;
                        log.error(errorMsg, e);
                        throw new IllegalStateException(errorMsg, e);
                    }
                }
            }
        }
        return imageResizeManager;
    }

    /**
     * Gets the ImageResizeManager as an Optional.
     * This method does not throw exceptions and returns empty Optional on failure.
     *
     * @return Optional containing the ImageResizeManager, or empty if initialization fails
     */
    public static Optional<ImageResizeManager> getImageResizeManagerOptional() {
        try {
            return Optional.of(getImageResizeManager());
        } catch (Exception e) {
            log.warn("Failed to get ImageResizeManager: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Checks if the ImageResizeManager is currently initialized.
     *
     * @return {@code true} if the manager is initialized, {@code false} otherwise
     */
    public static boolean isInitialized() {
        return imageResizeManager != null;
    }

    /**
     * Clears the cached ImageResizeManager instance.
     * This method is primarily intended for testing purposes to allow
     * re-initialization of the service.
     *
     * <p><strong>Warning:</strong> This method should not be called in production
     * code as it can cause inconsistent behavior across threads.</p>
     */
    static void clearCache() {
        synchronized (LOCK) {
            imageResizeManager = null;
            log.debug("Cleared ImageResizeManager cache");
        }
    }

    /**
     * Gets the bean name used for service lookup.
     *
     * @return the bean name for the ImageResizeManager service
     */
    public static String getBeanName() {
        return IMAGE_RESIZE_BEAN;
    }
}

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

import com.percussion.widgets.image.data.CachedImageMetaData;
import com.percussion.widgets.image.data.ImageData;

import java.util.Optional;
import java.util.Set;

/**
 * Service interface for managing cached image data and metadata.
 * Provides operations for storing, retrieving, and managing image cache entries
 * with thread-safe implementations expected.
 *
 * @since Java 11
 */
public interface ImageCacheManager {

    /**
     * Adds image data to the cache and returns a unique cache key.
     *
     * @param imageData the image data to cache, must not be {@code null}
     * @return unique cache key for the stored image, never {@code null}
     * @throws IllegalArgumentException if imageData is {@code null}
     * @throws RuntimeException if caching fails
     */
    String addImage(ImageData imageData);

    /**
     * Retrieves image data from the cache by key.
     *
     * @param key the cache key, must not be {@code null}
     * @return the cached image data, or {@code null} if not found
     * @throws IllegalArgumentException if key is {@code null}
     */
    ImageData getImage(String key);

    /**
     * Retrieves image data from the cache as an Optional.
     *
     * @param key the cache key, must not be {@code null}
     * @return Optional containing the image data, or empty if not found
     * @throws IllegalArgumentException if key is {@code null}
     */
    default Optional<ImageData> getImageOptional(String key) {
        return Optional.ofNullable(getImage(key));
    }

    /**
     * Retrieves cached image metadata by key.
     *
     * @param key the cache key, must not be {@code null}
     * @return the cached image metadata, or {@code null} if not found
     * @throws IllegalArgumentException if key is {@code null}
     */
    CachedImageMetaData getImageMetaData(String key);

    /**
     * Retrieves cached image metadata as an Optional.
     *
     * @param key the cache key, must not be {@code null}
     * @return Optional containing the cached metadata, or empty if not found
     * @throws IllegalArgumentException if key is {@code null}
     */
    default Optional<CachedImageMetaData> getImageMetaDataOptional(String key) {
        return Optional.ofNullable(getImageMetaData(key));
    }

    /**
     * Removes an image from the cache.
     *
     * @param key the cache key, must not be {@code null}
     * @throws IllegalArgumentException if key is {@code null}
     */
    void removeImage(String key);

    /**
     * Checks if an image exists in the cache.
     *
     * @param key the cache key, must not be {@code null}
     * @return {@code true} if the image exists, {@code false} otherwise
     * @throws IllegalArgumentException if key is {@code null}
     */
    boolean hasImage(String key);

    /**
     * Gets all cache keys currently stored.
     *
     * @return immutable set of all cache keys, never {@code null}
     */
    Set<String> getAllKeys();

    /**
     * Clears all cached images and metadata.
     */
    void clearCache();

    /**
     * Gets the current number of cached images.
     *
     * @return current cache size, always >= 0
     */
    long getCacheSize();
}

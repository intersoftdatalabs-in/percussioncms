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

package com.percussion.widgets.image.services.impl;

import com.percussion.widgets.image.data.CachedImageMetaData;
import com.percussion.widgets.image.data.ImageData;
import com.percussion.widgets.image.data.ImageMetaData;
import com.percussion.widgets.image.services.ImageCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link ImageCacheManager} using EhCache for storage.
 * Provides thread-safe caching operations for image data and metadata.
 *
 * @since Java 11
 */
public class ImageCacheManagerImpl implements ImageCacheManager {

    private static final Logger log = LogManager.getLogger(ImageCacheManagerImpl.class);

    private final AtomicLong counter = new AtomicLong(1L);
    private volatile Cache<String, ImageData> cache;

    /**
     * Default constructor initializing the cache manager.
     */
    public ImageCacheManagerImpl() {
        log.debug("Initializing ImageCacheManagerImpl");
    }

    @Override
    public String addImage(ImageData data) {
        Objects.requireNonNull(data, "ImageData must not be null");

        var imageKey = generateKey(data);
        log.debug("Generated new image key: {}", imageKey);

        cache.put(imageKey, data);

        log.debug("Successfully cached image with key: {}", imageKey);
        return imageKey;
    }

    @Override
    public ImageData getImage(String imageKey) {
        Objects.requireNonNull(imageKey, "Image key must not be null");

        var result = cache.get(imageKey);
        if (result == null) {
            log.debug("No image found for key: {}", imageKey);
            return null;
        }

        log.debug("Retrieved image data for key: {}", imageKey);
        return result;
    }

    @Override
    public CachedImageMetaData getImageMetaData(String imageKey) {
        Objects.requireNonNull(imageKey, "Image key must not be null");

        var data = getImage(imageKey);
        if (data != null) {
            log.debug("Retrieved metadata for image key: {}", imageKey);
            return new CachedImageMetaData(data, imageKey);
        }

        log.debug("No metadata found for key: {}", imageKey);
        return null;
    }

    @Override
    public boolean hasImage(String imageKey) {
        Objects.requireNonNull(imageKey, "Image key must not be null");

        var exists = cache.containsKey(imageKey);
        log.debug("Image key {} exists in cache: {}", imageKey, exists);
        return exists;
    }

    @Override
    public void removeImage(String imageKey) {
        Objects.requireNonNull(imageKey, "Image key must not be null");

        cache.remove(imageKey);
        log.debug("Removed image with key {}", imageKey);
    }

    @Override

    public Set<String> getAllKeys() {
        var keys = new java.util.HashSet<String>();
        cache.forEach(entry -> keys.add(entry.getKey()));
        log.debug("Retrieved {} cache keys", keys.size());
        return Set.copyOf(keys);
    }

    @Override
    public void clearCache() {
        cache.clear();
        log.info("Cache cleared successfully");
    }

    @Override
    public long getCacheSize() {
        // Ehcache 3.x Cache does not have a getSize() method;
        // iterate to count entries.
        long size = 0;
        for (var ignored : cache) {
            size++;
        }
        log.debug("Current cache size: {}", size);
        return size;
    }

    /**
     * Generates a unique cache key for the given image metadata.
     * The key is based on image properties and an incrementing counter.
     *
     * @param data the image metadata, must not be {@code null}
     * @return unique cache key as hexadecimal string
     */
    protected String generateKey(ImageMetaData data) {
        Objects.requireNonNull(data, "ImageMetaData must not be null");

        var value = data.getSize() + (long) data.getHeight() * 2;

        var filename = Optional.ofNullable(data.getFilename())
            .filter(StringUtils::isNotBlank)
            .orElse("default.img");

        value -= filename.hashCode();
        value = (value << 12) + counter.getAndIncrement();

        var key = Long.toHexString(value);
        log.debug("Generated key {} for image: {}", key, filename);
        return key;
    }

    /**
     * Gets the underlying Ehcache 3.x cache instance.
     *
     * @return the cache instance, may be {@code null} if not initialized
     */
    public Cache<String, ImageData> getCache() {
        return cache;
    }

    /**
     * Sets the Ehcache 3.x cache instance for this manager.
     *
     * @param cache the cache instance to set, must not be {@code null}
     */
    public void setCache(Cache<String, ImageData> cache) {
        this.cache = Objects.requireNonNull(cache, "Cache must not be null");
        log.info("Image cache instance set");
    }
}

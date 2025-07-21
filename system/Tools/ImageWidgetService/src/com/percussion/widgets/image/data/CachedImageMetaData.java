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

package com.percussion.widgets.image.data;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents cached image metadata with an associated cache key.
 * Extends {@link ImageMetaData} to provide caching capabilities for image processing
 * and retrieval operations.
 *
 * @since Java 11
 */
public class CachedImageMetaData extends ImageMetaData {

   private final String imageKey;

   /**
    * Creates a new CachedImageMetaData from existing metadata and cache key.
    *
    * @param metadata the image metadata to cache, must not be {@code null}
    * @param key the cache key, must not be {@code null} or blank
    * @throws IllegalArgumentException if metadata is {@code null} or key is blank
    */
   public CachedImageMetaData(ImageMetaData metadata, String key) {
      super(Objects.requireNonNull(metadata, "ImageMetaData must not be null"));

      if (StringUtils.isBlank(key)) {
         throw new IllegalArgumentException("Cache key must not be blank");
      }
      this.imageKey = key.trim();
   }

   /**
    * Copy constructor that creates a defensive copy of another CachedImageMetaData.
    *
    * @param other the CachedImageMetaData to copy, must not be {@code null}
    * @throws IllegalArgumentException if other is {@code null}
    */
   public CachedImageMetaData(CachedImageMetaData other) {
      super(other);
      Objects.requireNonNull(other, "CachedImageMetaData to copy must not be null");
      this.imageKey = other.imageKey;
   }

   /**
    * Gets the cache key for this image metadata.
    *
    * @return the cache key, never {@code null} or blank
    */
   public String getImageKey() {
      return imageKey;
   }

   /**
    * Gets the cache key as an Optional.
    *
    * @return Optional containing the cache key, never empty
    */
   public Optional<String> getImageKeyOptional() {
      return Optional.of(imageKey);
   }

   /**
    * Checks if this cached metadata has a valid cache key.
    *
    * @return {@code true} if the cache key is not blank, {@code false} otherwise
    */
   public boolean hasValidCacheKey() {
      return StringUtils.isNotBlank(imageKey);
   }

   /**
    * Creates a new CachedImageMetaData with the same metadata but a different cache key.
    *
    * @param newKey the new cache key, must not be {@code null} or blank
    * @return new CachedImageMetaData instance with the new key
    * @throws IllegalArgumentException if newKey is blank
    */
   public CachedImageMetaData withNewKey(String newKey) {
      return new CachedImageMetaData(this, newKey);
   }

   /**
    * Converts this cached metadata back to regular ImageMetaData (without cache key).
    *
    * @return new ImageMetaData instance with copied metadata
    */
   public ImageMetaData toRegularMetaData() {
      return new ImageMetaData(this);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      if (!super.equals(obj)) {
         return false;
      }
      var that = (CachedImageMetaData) obj;
      return Objects.equals(imageKey, that.imageKey);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), imageKey);
   }

   @Override
   public String toString() {
      return String.format("CachedImageMetaData{%s, cacheKey='%s'}",
         super.toString().replace("ImageMetaData{", "").replace("}", ""),
         imageKey);
   }
}

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
package com.percussion.pso.imageedit.services.cache.impl;

import com.percussion.pso.imageedit.data.ImageData;
import com.percussion.pso.imageedit.data.ImageMetaData;
import com.percussion.pso.imageedit.services.cache.ImageCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;

/**
 * ImageCacheManagerImpl class.
 */
public class ImageCacheManagerImpl implements ImageCacheManager {
  private static final Logger log = LogManager.getLogger(ImageCacheManagerImpl.class);
  private long counter;

  private Cache<String, ImageData> cache;

  /**
   * Creates a new ImageCacheManagerImpl.
   */
  public ImageCacheManagerImpl() {
    counter = 1;
  }

  /**
   * addImage operation.
   *
   * @param data the data
   * @return the result
   */
  public String addImage(ImageData data) {
    String imageKey = generateKey(data);
    log.debug("new image key is {}", imageKey);
    cache.put(imageKey, data);

    return imageKey;
  }

  /**
   * See referenced member.
   * @see ImageCacheManager#getImage(String)
   * @param imageKey the image key
   * @return the result
   */
  public ImageData getImage(String imageKey) {
    return cache.get(imageKey);
  }

  /**
   * Returns the image meta data.
   *
   * @param imageKey the image key
   * @return the result
   */
  public ImageMetaData getImageMetaData(String imageKey) {
    ImageData data = getImage(imageKey);
    if (data != null) {
      return new ImageMetaData(data);
    }
    return null;
  }

  /**
   * hasImage operation.
   *
   * @param imageKey the image key
   * @return the result
   */
  public boolean hasImage(String imageKey) {
    return cache.containsKey(imageKey);
  }

  /**
   * See referenced member.
   * @see ImageCacheManager#removeImage(String)
   * @param imageKey the image key
   */
  public void removeImage(String imageKey) {
    cache.remove(imageKey);
  }

  /**
   * generateKey operation.
   *
   * @param data the data
   * @return the result
   */
  protected String generateKey(ImageMetaData data) {
    long value = data.getSize() + data.getHeight() * 2;
    String fname = data.getFilename();
    if (StringUtils.isNotBlank(fname)) {
      value -= fname.hashCode();
    } else {
      value -= "abc.xyz".hashCode();
    }
    value = (value << 12) + counter++;
    return Long.toHexString(value);
  }

  /**
   * Returns the cache.
   * @return the cache
   */
  public Cache<String, ImageData> getCache() {
    return cache;
  }

  /**
   * Sets the cache.
   * @param cache the cache to set
   */
  public void setCache(Cache<String, ImageData> cache) {
    this.cache = cache;
  }
}

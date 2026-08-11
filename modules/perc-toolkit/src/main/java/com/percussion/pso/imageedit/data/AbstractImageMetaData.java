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
package com.percussion.pso.imageedit.data;

/**
 * Base holder for image editor session key and image metadata.
 */
public class AbstractImageMetaData {
  /** Session or cache key for the image being edited. */
  private String imageKey;
  /** Associated image metadata. */
  private ImageMetaData metaData;

  /**
   * Creates an empty metadata holder.
   */
  public AbstractImageMetaData() {
    super();
  }

  /**
   * Returns the image session key.
   *
   * @return the image key
   */
  public String getImageKey() {
    return imageKey;
  }

  /**
   * Sets the image session key.
   *
   * @param imageKey the image key to set
   */
  public void setImageKey(String imageKey) {
    this.imageKey = imageKey;
  }

  /**
   * Returns the image metadata.
   *
   * @return the metadata
   */
  public ImageMetaData getMetaData() {
    return metaData;
  }

  /**
   * Sets the image metadata.
   *
   * @param metaData the metadata to set
   */
  public void setMetaData(ImageMetaData metaData) {
    this.metaData = metaData;
  }
}

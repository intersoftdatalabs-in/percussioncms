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
package com.percussion.pso.imageedit.services.impl;

import com.percussion.pso.imageedit.data.ImageSizeDefinition;
import com.percussion.pso.imageedit.services.ImageSizeDefinitionManager;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service for getting defined image sizes.
 *
 * @author DavidBenua
 */
public class ImageSizeDefinitionManagerImpl implements ImageSizeDefinitionManager {

  private static final Logger log = LogManager.getLogger(ImageSizeDefinitionManagerImpl.class);

  private List<ImageSizeDefinition> sizes;

  private String sizedImageNodeName;

  private String sizedImagePropertyName;

  /** The path name of the image to be displayed when there is no image. */
  private String failureImagePath;

    /**
     * Creates a new ImageSizeDefinitionManagerImpl.
     */
    public ImageSizeDefinitionManagerImpl() {
    sizes = new ArrayList<ImageSizeDefinition>();
  }

  /**
   * See referenced member.
   * @see ImageSizeDefinitionManager#getAllImageSizes()
   * @return the result
   */
  public List<ImageSizeDefinition> getAllImageSizes() {
    return sizes;
  }

  /**
   * See referenced member.
   * @see ImageSizeDefinitionManager#getImageSize(String)
   * @param code the code
   * @return the result
   */
  public ImageSizeDefinition getImageSize(String code) {
    if (StringUtils.isEmpty(code)) {
      throw new IllegalArgumentException("image size code must not be null");
    }
    for (ImageSizeDefinition sz : sizes) {
      if (sz.getCode().equals(code)) {
        return sz;
      }
    }
    log.debug("request for image size {} not found", code);
    return null;
  }

  /**
   * Returns the sizes.
   * @return the sizes
   */
  public List<ImageSizeDefinition> getSizes() {
    return sizes;
  }

  /**
   * Sets the sizes.
   * @param sizes the sizes to set
   */
  public void setSizes(List<ImageSizeDefinition> sizes) {
    this.sizes = sizes;
  }

  /**
   * Returns the sizedImageNodeName.
   * @return the sizedImageNodeName
   */
  public String getSizedImageNodeName() {
    return sizedImageNodeName;
  }

  /**
   * Sets the sizedImageNodeName.
   * @param sizedImageNodeName the sizedImageNodeName to set
   */
  public void setSizedImageNodeName(String sizedImageNodeName) {
    this.sizedImageNodeName = sizedImageNodeName;
  }

  /**
   * Returns the sizedImagePropertyName.
   * @return the sizedImagePropertyName
   */
  public String getSizedImagePropertyName() {
    return sizedImagePropertyName;
  }

  /**
   * Sets the sizedImagePropertyName.
   * @param sizedImagePropertyName the sizedImagePropertyName to set
   */
  public void setSizedImagePropertyName(String sizedImagePropertyName) {
    this.sizedImagePropertyName = sizedImagePropertyName;
  }

  /**
   * Returns the failureImagePath.
   * @return the failureImagePath
   */
  public String getFailureImagePath() {
    return failureImagePath;
  }

  /**
   * Sets the failureImagePath.
   * @param failureImagePath the failureImagePath to set
   */
  public void setFailureImagePath(String failureImagePath) {
    this.failureImagePath = failureImagePath;
  }
}

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
 * Form/backing bean for image editor UI properties (title, crop, sizes).
 */
public class ImageBean {
  /** System title of the image item. */
  private String sysTitle;
  /** Display title shown in the UI. */
  private String displayTitle;
  /** Long description of the image. */
  private String description;
  /** Alternate text for accessibility. */
  private String alt;
  /** Encoded list of sized image variants. */
  private String sizedImages;
  /** Image width in pixels. */
  private int width;
  /** Image height in pixels. */
  private int height;
  /** Crop origin X coordinate. */
  private int x;
  /** Crop origin Y coordinate. */
  private int y;
  /** Whether aspect ratio is constrained. */
  private Boolean constraint = true;
  /** Whether the bean has unsaved edits. */
  private boolean dirty = false;

  /**
   * Creates an empty image bean.
   */
  public ImageBean() {
    super();
  }

  /**
   * Returns whether the item is dirty.
   *
   * @return the dirty flag
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Sets the dirty flag.
   *
   * @param dirty the dirty to set
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * Returns whether aspect ratio is constrained.
   *
   * @return {@code true} if constrained
   */
  public Boolean isConstraint() {
    return constraint;
  }

  /**
   * Sets whether aspect ratio is constrained.
   *
   * @param constraint the constraint flag
   */
  public void setConstraint(Boolean constraint) {
    this.constraint = constraint;
  }

  /**
   * Returns the system title.
   *
   * @return the system title
   */
  public String getSysTitle() {
    return sysTitle;
  }

  /**
   * Sets the system title.
   *
   * @param sysTitle the system title
   */
  public void setSysTitle(String sysTitle) {
    this.sysTitle = sysTitle;
  }

  /**
   * Returns the display title.
   *
   * @return the display title
   */
  public String getDisplayTitle() {
    return displayTitle;
  }

  /**
   * Sets the display title.
   *
   * @param displayTitle the display title
   */
  public void setDisplayTitle(String displayTitle) {
    this.displayTitle = displayTitle;
  }

  /**
   * Returns the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the alternate text.
   *
   * @return the alt text
   */
  public String getAlt() {
    return alt;
  }

  /**
   * Sets the alternate text.
   *
   * @param alt the alt text
   */
  public void setAlt(String alt) {
    this.alt = alt;
  }

  /**
   * Returns the sized-images payload.
   *
   * @return the sized images value
   */
  public String getSizedImages() {
    return sizedImages;
  }

  /**
   * Sets the sized-images payload.
   *
   * @param sizedImages the sized images value
   */
  public void setSizedImages(String sizedImages) {
    this.sizedImages = sizedImages;
  }

  /**
   * Returns the image width.
   *
   * @return width in pixels
   */
  public int getWidth() {
    return width;
  }

  /**
   * Sets the image width.
   *
   * @param width width in pixels
   */
  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Returns the image height.
   *
   * @return height in pixels
   */
  public int getHeight() {
    return height;
  }

  /**
   * Sets the image height.
   *
   * @param height height in pixels
   */
  public void setHeight(int height) {
    this.height = height;
  }

  /**
   * Returns the crop origin X coordinate.
   *
   * @return the X coordinate
   */
  public int getX() {
    return x;
  }

  /**
   * Sets the crop origin X coordinate.
   *
   * @param x the X coordinate
   */
  public void setX(int x) {
    this.x = x;
  }

  /**
   * Returns the crop origin Y coordinate.
   *
   * @return the Y coordinate
   */
  public int getY() {
    return y;
  }

  /**
   * Sets the crop origin Y coordinate.
   *
   * @param y the Y coordinate
   */
  public void setY(int y) {
    this.y = y;
  }
}

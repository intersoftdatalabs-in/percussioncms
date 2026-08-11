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
 * UserSessionData class.
 */
public class UserSessionData {
  /**
   * Creates a new UserSessionData.
   */
  public UserSessionData() {
    // default
  }


  MasterImageMetaData mimd = null;
  String[] pages = null;

  double scaleFactor = 1.0;

  boolean dirty = false;

  SimpleImageMetaData displayImage = null;

  // Map<String, Dimension> resizeDetails = new HashMap<String, Dimension>();

  /**
   * Returns the mimd.
   *
   * @return the result
   */
  public MasterImageMetaData getMimd() {
    return mimd;
  }

  /**
   * Sets the mimd.
   *
   * @param mimd the mimd
   */
  public void setMimd(MasterImageMetaData mimd) {
    this.mimd = mimd;
  }

  /**
   * Returns the pages.
   *
   * @return the result
   */
  public String[] getPages() {
    return pages;
  }

  /**
   * Sets the pages.
   *
   * @param pages the pages
   */
  public void setPages(String[] pages) {
    this.pages = pages;
  }

  /**
   * Returns the scaleFactor.
   * @return the scaleFactor
   */
  public double getScaleFactor() {
    return scaleFactor;
  }

  /**
   * Sets the scaleFactor.
   * @param scaleFactor the scaleFactor to set
   */
  public void setScaleFactor(double scaleFactor) {
    this.scaleFactor = scaleFactor;
  }

  /**
   * Returns the displayImage.
   * @return the displayImage
   */
  public SimpleImageMetaData getDisplayImage() {
    return displayImage;
  }

  /**
   * Sets the displayImage.
   * @param displayImage the displayImage to set
   */
  public void setDisplayImage(SimpleImageMetaData displayImage) {
    this.displayImage = displayImage;
  }

  /**
   * Returns whether dirty.
   *
   * @return the result
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Sets the dirty.
   *
   * @param dirty the dirty
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }
}

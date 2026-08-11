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

import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * ImageMetaData class.
 */
public class ImageMetaData implements Serializable {

  private static final long serialVersionUID = -13542359L;

  private String mimeType;
  private String ext;
  private String filename;

  private long size = 0L;
  private int width = 0;
  private int height = 0;

  /**
   * Default constructor.
   * Creates a new ImageMetaData.
   *
   */
  public ImageMetaData() {
    super();
  }

  /**
   * Copy constructor.
   *
   * @param o other
   */
  public ImageMetaData(ImageMetaData o) {
    this.filename = o.getFilename();
    this.ext = o.getExt();
    this.mimeType = o.getMimeType();
    this.size = o.getSize();
    this.height = o.getHeight();
    this.width = o.getWidth();
  }

  /**
   * Returns the mimeType.
   * @return the mimeType
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Sets the mimeType.
   * @param mimeType the mimeType to set
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Returns the ext.
   * @return the ext
   */
  public String getExt() {
    return ext;
  }

  /**
   * Sets the ext.
   * @param ext the ext to set
   */
  public void setExt(String ext) {
    this.ext = ext;
  }

  /**
   * Returns the filename.
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Sets the filename.
   * @param filename the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * Returns the size.
   * @return the size
   */
  public long getSize() {
    return size;
  }

  /**
   * Sets the size.
   * @param size the size to set
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * Returns the width.
   * @return the width
   */
  public int getWidth() {
    return width;
  }

  /**
   * Sets the width.
   * @param width the width to set
   */
  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Returns the height.
   * @return the height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Sets the height.
   * @param height the height to set
   */
  public void setHeight(int height) {
    this.height = height;
  }

  /**
   * toString operation.
   *
   * @return the result
   */
  public String toString() {
    return new ToStringBuilder(this)
        .append("filename", filename)
        .append("MIME type", mimeType)
        .append("size", size)
        .append("height", height)
        .append("width", width)
        .toString();
  }
}

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
// REFACTORED: CP-JAVA11

package com.percussion.assetmanagement.data;

import java.io.InputStream;
import org.apache.commons.lang3.StringUtils;

/**
 * Base class for all service requests to create assets during bulk upload. Provides common fields
 * and validation for asset creation.
 */
public abstract class PSAbstractAssetRequest {

  /** Specifies the type of asset to be created by a request. */
  public enum AssetType {
    /** Binary assets */
    FILE,
    IMAGE,
    /** Extracted assets */
    HTML,
    RICH_TEXT,
    SIMPLE_TEXT
  }

  /** Package-private for same-package subclass constructors (this-escape safe seed). */
  String folderPath;

  AssetType type;
  String fileName;
  InputStream fileContents;

  /**
   * Gets the type of asset this request will be used to create.
   *
   * @return the asset type, may be {@code null}.
   */
  public AssetType getType() {
    return type;
  }

  /**
   * Sets the asset type.
   *
   * @param type the asset type to set; must not be {@code null}.
   */
  protected void setType(AssetType type) {
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    this.type = type;
  }

  /**
   * Gets the folder path (finder) under which the asset will be created.
   *
   * @return the new asset folder path, may be {@code null}.
   */
  public String getFolderPath() {
    return folderPath;
  }

  /**
   * Sets the folder path (finder) under which the asset will be created.
   *
   * @param folderPath the new asset folder path; must not be {@code null} or empty.
   */
  protected final void setFolderPath(String folderPath) {
    if (StringUtils.isBlank(folderPath)) {
      throw new IllegalArgumentException("folderPath may not be blank");
    }
    this.folderPath = folderPath;
  }

  /**
   * Gets the name of the file for which the binary asset will be created.
   *
   * @return the file name, may be {@code null}.
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Sets the file name.
   *
   * @param fileName must not be {@code null} or empty.
   */
  protected final void setFileName(String fileName) {
    if (StringUtils.isBlank(fileName)) {
      throw new IllegalArgumentException("fileName may not be blank");
    }
    this.fileName = sanitizeFileName(fileName);
  }

  /**
   * Replaces spaces with hyphens for stored asset file names. Shared by setters and subclass
   * constructors so space sanitization stays consistent.
   */
  static String sanitizeFileName(String fileName) {
    return fileName.replace(' ', '-');
  }

  /**
   * Gets the contents of the file for which the binary asset will be created.
   *
   * @return the file contents, may be {@code null}.
   */
  public InputStream getFileContents() {
    return fileContents;
  }

  /**
   * Sets the file contents.
   *
   * @param fileContents may not be {@code null}.
   */
  protected final void setFileContents(InputStream fileContents) {
    this.fileContents = fileContents;
  }
}

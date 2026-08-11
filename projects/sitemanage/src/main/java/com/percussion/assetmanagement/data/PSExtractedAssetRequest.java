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

/** Used to request the creation of an asset whose content is extracted from an uploaded file. */
public class PSExtractedAssetRequest extends PSAbstractAssetRequest {

  private String selector;
  private boolean includeOuterHtml;

  /**
   * Constructs a new extracted asset request.
   *
   * @param folderPath see {@link #setFolderPath(String)}.
   * @param type see {@link #setType(AssetType)}.
   * @param fileName see {@link #setFileName(String)}.
   * @param fileContents see {@link #setFileContents(InputStream)}.
   * @param selector see {@link #setSelector(String)}.
   * @param includeOuterHtml see {@link #setIncludeOuterHtml(boolean)}.
   */
  public PSExtractedAssetRequest(
      String folderPath,
      AssetType type,
      String fileName,
      InputStream fileContents,
      String selector,
      boolean includeOuterHtml) {
    super();
    if (folderPath == null || folderPath.isBlank()) {
      throw new IllegalArgumentException("folderPath may not be blank");
    }
    if (type != AssetType.HTML && type != AssetType.RICH_TEXT && type != AssetType.SIMPLE_TEXT) {
      throw new IllegalArgumentException("unsupported asset type : " + type);
    }
    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("fileName may not be blank");
    }
    if (selector == null || selector.isBlank()) {
      throw new IllegalArgumentException("selector may not be blank");
    }
    this.folderPath = folderPath;
    this.type = type;
    this.fileName = fileName.replace("\\x20", "-");
    this.fileContents = fileContents;
    this.selector = selector;
    this.includeOuterHtml = includeOuterHtml;
  }

  @Override
  public final void setType(AssetType type) {
    if (type != AssetType.HTML && type != AssetType.RICH_TEXT && type != AssetType.SIMPLE_TEXT) {
      throw new IllegalArgumentException("unsupported asset type : " + type);
    }
    super.setType(type);
  }

  /**
   * Gets the CSS selector used to find content for extraction.
   *
   * @return the selector, may be {@code null}.
   */
  public String getSelector() {
    return selector;
  }

  /**
   * Sets the CSS selector for extraction.
   *
   * @param selector may not be {@code null} or empty.
   */
  public final void setSelector(String selector) {
    if (StringUtils.isBlank(selector)) {
      throw new IllegalArgumentException("selector may not be blank");
    }
    this.selector = selector;
  }

  /**
   * Determines whether or not the extracted content should include the selector element.
   *
   * @return {@code true} if the outer HTML should be included in the extracted content, {@code
   *     false} otherwise.
   */
  public boolean shouldIncludeOuterHtml() {
    return includeOuterHtml;
  }

  /**
   * Sets whether the selector element should be included in the extracted content.
   *
   * @param includeOuterHtml {@code true} if the selector element should be included, {@code false}
   *     otherwise.
   */
  public final void setIncludeOuterHtml(boolean includeOuterHtml) {
    this.includeOuterHtml = includeOuterHtml;
  }
}

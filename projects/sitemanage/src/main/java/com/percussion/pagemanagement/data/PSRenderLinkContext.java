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
package com.percussion.pagemanagement.data;

import com.percussion.sitemanage.data.PSSiteSummary;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * Holds all the information needed to create a link other than the resource or page. See
 * class-level Javadoc for details.
 *
 * @author adamgent
 */
public abstract class PSRenderLinkContext {

  private String folderPath;
  private boolean isDeliveryContext = false;

  public enum Mode {
    PUBLISH,
    PREVIEW
  }

  public enum OwnerType {
    PAGE,
    TEMPLATE,
    ASSET
  }

  /**
   * There are at least two modes of generating links.
   *
   * @return the mode for generating links, never {@code null}.
   */
  @NotNull
  public abstract Mode getMode();

  /**
   * @return the site, never {@code null} but may be an empty site for preview.
   */
  public abstract PSSiteSummary getSite();

  /**
   * The current CM system folder path. <strong>This is not the published URL or file path!</strong>
   *
   * @return never {@code null}.
   */
  @NotBlank
  @NotNull
  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * Determines if the context is delivery context or assembly context. The delivery context is used
   * to generate publishing locations. The assembly context is used to generate links within HTML
   * pages.
   */
  public boolean isDeliveryContext() {
    return isDeliveryContext;
  }

  public void setDeliveryContext(boolean context) {
    isDeliveryContext = context;
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (Exception e) {
      throw new RuntimeException("Cannot clone link legacyLinkContext", e);
    }
  }

  @Override
  public String toString() {
    var sb = new StringBuilder("PSRenderLinkContext{");
    sb.append("folderPath='").append(folderPath).append('\'');
    sb.append(", isDeliveryContext=").append(isDeliveryContext);
    sb.append('}');
    return sb.toString();
  }
}

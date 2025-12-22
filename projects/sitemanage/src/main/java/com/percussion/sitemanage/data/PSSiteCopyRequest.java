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
package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * Request object used for copying a site. The source and destination site names are required; the
 * asset folder path is optional.
 */
@XmlRootElement(name = "SiteCopyRequest")
@JsonRootName("SiteCopyRequest")
public class PSSiteCopyRequest {

  @NotNull @NotEmpty private String srcSite;

  @NotNull @NotEmpty private String copySite;

  private String assetFolder;

  public String getSrcSite() {
    return srcSite;
  }

  public void setSrcSite(String srcSite) {
    this.srcSite = srcSite;
  }

  public String getCopySite() {
    return copySite;
  }

  public void setCopySite(String copySite) {
    this.copySite = copySite;
  }

  /**
   * Gets the asset folder path, if present.
   *
   * @return Optional asset folder path.
   */
  public Optional<String> getAssetFolder() {
    return Optional.ofNullable(assetFolder);
  }

  public void setAssetFolder(String assetFolder) {
    this.assetFolder = assetFolder;
  }
}

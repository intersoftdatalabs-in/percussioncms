// REFACTORED: CP-JAVA11
/*
 * Copyright (c) 2024 Intersoft Data Labs, Inc.
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
package com.percussion.pathmanagement.data;

import com.percussion.share.data.PSAbstractDataObject;

/**
 * Options for generating a sitemap for a folder.
 *
 * @author yubingchen
 */
public class PSGenerateSiteMapOptions extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String generateSitemapExcludeImage;
  private String generateSitemap;

  /**
   * Gets the generate sitemap flag.
   *
   * @return the generate sitemap flag
   */
  public String getGenerateSitemap() {
    return generateSitemap;
  }

  /**
   * Sets the generate sitemap flag.
   *
   * @param generateSitemap the flag value
   */
  public void setGenerateSitemap(String generateSitemap) {
    this.generateSitemap = generateSitemap;
  }

  /**
   * Gets the flag for excluding images from the sitemap.
   *
   * @return the exclude image flag
   */
  public String getGenerateSitemapExcludeImage() {
    return generateSitemapExcludeImage;
  }

  /**
   * Sets the flag for excluding images from the sitemap.
   *
   * @param generateSitemapExcludeImage the flag value
   */
  public void setGenerateSitemapExcludeImage(String generateSitemapExcludeImage) {
    this.generateSitemapExcludeImage = generateSitemapExcludeImage;
  }
}

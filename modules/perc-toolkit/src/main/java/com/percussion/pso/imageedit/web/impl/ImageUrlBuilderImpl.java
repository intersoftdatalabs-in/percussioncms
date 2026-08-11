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
package com.percussion.pso.imageedit.web.impl;

import com.percussion.pso.imageedit.web.ImageUrlBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ImageUrlBuilderImpl class.
 */
public class ImageUrlBuilderImpl implements ImageUrlBuilder {
  /**
   * Creates a new ImageUrlBuilderImpl.
   */
  public ImageUrlBuilderImpl() {
    // default
  }

  private static final Logger log = LogManager.getLogger(ImageUrlBuilderImpl.class);

  private String baseUrl;
  private String suffix = "jpg";

  /**
   * See referenced member.
   * @see ImageUrlBuilder#buildUrl(String)
   * @param imageKey the image key
   * @return the result
   */
  public String buildUrl(String imageKey) {
    StringBuilder sb = new StringBuilder();
    sb.append(baseUrl);
    if (!baseUrl.endsWith("/")) {
      sb.append("/");
    }
    sb.append("img");
    sb.append(imageKey);
    sb.append(".");
    sb.append(suffix);
    return sb.toString();
  }

  /**
   * See referenced member.
   * @see ImageUrlBuilder#extractKey(String)
   * @param url the url
   * @return the result
   */
  public String extractKey(String url) {
    String emsg;
    if (StringUtils.isBlank(url)) {
      emsg = "image URL must not be blank";
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    String lastPart = StringUtils.substringAfterLast(url, "/");
    lastPart = StringUtils.substringBefore(lastPart, ".");
    String key = StringUtils.substringAfter(lastPart, "img");

    return key;
  }

  /**
   * Returns the baseUrl.
   * @return the baseUrl
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Sets the baseUrl.
   * @param baseUrl the baseUrl to set
   */
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Returns the suffix.
   * @return the suffix
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * Sets the suffix.
   * @param suffix the suffix to set
   */
  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }
}

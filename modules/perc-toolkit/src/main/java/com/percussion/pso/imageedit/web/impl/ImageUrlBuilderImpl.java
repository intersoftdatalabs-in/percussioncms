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

public class ImageUrlBuilderImpl implements ImageUrlBuilder {
  private static final Logger log = LogManager.getLogger(ImageUrlBuilderImpl.class);

  private String baseUrl;
  private String suffix = "jpg";

<<<<<<< HEAD
  /**
   * @see ImageUrlBuilder#buildUrl(String)
   */
=======
  /** @see ImageUrlBuilder#buildUrl(String) */
>>>>>>> development-8.1.x
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

<<<<<<< HEAD
  /**
   * @see ImageUrlBuilder#extractKey(String)
   */
=======
  /** @see ImageUrlBuilder#extractKey(String) */
>>>>>>> development-8.1.x
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

<<<<<<< HEAD
  /**
   * @return the baseUrl
   */
=======
  /** @return the baseUrl */
>>>>>>> development-8.1.x
  public String getBaseUrl() {
    return baseUrl;
  }

<<<<<<< HEAD
  /**
   * @param baseUrl the baseUrl to set
   */
=======
  /** @param baseUrl the baseUrl to set */
>>>>>>> development-8.1.x
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

<<<<<<< HEAD
  /**
   * @return the suffix
   */
=======
  /** @return the suffix */
>>>>>>> development-8.1.x
  public String getSuffix() {
    return suffix;
  }

<<<<<<< HEAD
  /**
   * @param suffix the suffix to set
   */
=======
  /** @param suffix the suffix to set */
>>>>>>> development-8.1.x
  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }
}

/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.pso.restservice.model;

import java.net.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;

public abstract class BaseHttpResponse {

  private Item existingItem;

  /** Package-visible for subclass single-shot constructors (avoids this-escape). */
  HttpHeaders headers;

  /***
   * Sets the HTTP Headers collection for this response.
   * @param headers
   */
  public final void setHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  /***
   * Gets the HTTP Headers collection for this response.
   *
   */
  public HttpHeaders getHeaders() {
    return headers;
  }

  /***
   * Will return the ETag header if it is set or the empty string.
   *
   */
  public String getETag() {
    if (headers == null) {
      return "";
    }
    return headers.firstValue("ETag").orElse("");
  }

  /***
   * Will return the last modified header if it is set or the empty string.
   *
   */
  public String getLastModified() {
    String ret = "";

    if (headers != null) {
      ret = headers.firstValue("Last-Modified").orElse("");
    }

    // If the last modified header isn't set, we'll use the date of the response.
    if (StringUtils.isEmpty(ret)) {
      if (headers != null) {
        ret = headers.firstValue("Date").orElse("");
      }
    }

    return ret;
  }

  public void setExistingItem(Item existingItem) {
    this.existingItem = existingItem;
  }

  public Item getExistingItem() {
    return existingItem;
  }
}

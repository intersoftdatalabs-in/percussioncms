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

/**
 * Base type for HTTP responses that carry headers and an optional existing item.
 */
public abstract class BaseHttpResponse {

  /** Optional previously loaded item associated with the response. */
  private Item existingItem;

  /** Package-visible for subclass single-shot constructors (avoids this-escape). */
  HttpHeaders headers;

  /**
   * Creates an empty HTTP response holder.
   */
  protected BaseHttpResponse() {
    // default
  }

  /**
   * Sets the HTTP headers collection for this response.
   *
   * @param headers the response headers
   */
  public final void setHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  /**
   * Gets the HTTP headers collection for this response.
   *
   * @return the headers, or {@code null} if not set
   */
  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Returns the ETag header if set, otherwise the empty string.
   *
   * @return the ETag value or empty string
   */
  public String getETag() {
    if (headers == null) {
      return "";
    }
    return headers.firstValue("ETag").orElse("");
  }

  /**
   * Returns the last-modified header if set, otherwise the Date header, otherwise empty string.
   *
   * @return the last-modified timestamp string or empty string
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

  /**
   * Sets the existing item associated with this response.
   *
   * @param existingItem the existing item
   */
  public void setExistingItem(Item existingItem) {
    this.existingItem = existingItem;
  }

  /**
   * Returns the existing item associated with this response.
   *
   * @return the existing item, or {@code null}
   */
  public Item getExistingItem() {
    return existingItem;
  }
}

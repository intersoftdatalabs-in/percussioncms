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

package com.percussion.delivery.metadata.data;

/**
 * Simple two-property holder used by the metadata REST layer to carry an opaque key together with a
 * target URL. Typically populated from request data and serialized verbatim to clients.
 */
public class HrefData {

  /** Opaque identifier for the link. */
  private String key;

  /** Destination URL the link points at. */
  private String url;

  /** No-arg constructor required by the JSON binding layer. */
  public HrefData() {}

  /**
   * Returns the opaque link key.
   *
   * @return the key, may be <code>null</code>.
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the opaque link key.
   *
   * @param key the key to set; may be <code>null</code>.
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Returns the destination URL.
   *
   * @return the URL, may be <code>null</code>.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the destination URL.
   *
   * @param url the URL to set; may be <code>null</code>.
   */
  public void setUrl(String url) {
    this.url = url;
  }
}

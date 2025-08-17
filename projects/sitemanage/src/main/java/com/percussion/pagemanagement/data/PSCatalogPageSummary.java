// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.data;

/**
 * Summary information for a cataloged page. Immutable data object for use in page listings.
 *
 * @author JaySeletz
 */
public class PSCatalogPageSummary {

  private String id;
  private String path;
  private String name;

  /**
   * Gets the unique ID of the page.
   *
   * @return the ID, may be {@code null} if not set.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique ID of the page.
   *
   * @param id the ID to set, may be {@code null}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the path of the page.
   *
   * @return the path, may be {@code null} if not set.
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path of the page.
   *
   * @param path the path to set, may be {@code null}.
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Gets the name of the page.
   *
   * @return the name, may be {@code null} if not set.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the page.
   *
   * @param name the name to set, may be {@code null}.
   */
  public void setName(String name) {
    this.name = name;
  }
}

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
package com.percussion.activity.data;

import java.util.List;
import java.util.Objects;

/** Name/path container for activity which also includes the content types of the active items. */
public class PSActivityNode {

  private String siteName;
  private String name;
  private String path;
  private List<String> contentTypes;

  public PSActivityNode(String siteName, String name, String path, List<String> contentTypes) {
    this.siteName = Objects.requireNonNull(siteName, "siteName must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.path = Objects.requireNonNull(path, "path must not be null");
    this.contentTypes =
        List.copyOf(Objects.requireNonNull(contentTypes, "contentTypes must not be null"));
  }

  public PSActivityNode(String siteName, String name, String path, String contentType) {
    this(
        siteName,
        name,
        path,
        List.of(Objects.requireNonNull(contentType, "contentType must not be null")));
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @param path the path to set
   */
  public void setPath(String path) {
    this.path = Objects.requireNonNull(path, "path must not be null");
  }

  /**
   * @return the contentTypes
   */
  public List<String> getContentTypes() {
    return List.copyOf(contentTypes);
  }

  /**
   * @param contentTypes the contentTypes to set
   */
  public void setContentTypes(List<String> contentTypes) {
    this.contentTypes =
        List.copyOf(Objects.requireNonNull(contentTypes, "contentTypes must not be null"));
  }

  /**
   * @return the siteName
   */
  public String getSiteName() {
    return siteName;
  }

  /**
   * @param siteName the siteName to set
   */
  public void setSiteName(String siteName) {
    this.siteName = Objects.requireNonNull(siteName, "siteName must not be null");
  }
}

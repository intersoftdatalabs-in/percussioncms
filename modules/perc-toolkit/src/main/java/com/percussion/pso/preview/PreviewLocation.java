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
package com.percussion.pso.preview;

/**
 * Represents a site and folder location where a preview might take place.
 *
 * <p>The Natural Order of multiple locations is alphabetic by site name and then folder location.
 *
 * @author DavidBenua
 */
public class PreviewLocation implements Comparable<PreviewLocation> {
  String siteName;
  String path;
  String url;

  /**
   * Creates a new PreviewLocation.
   */
  public PreviewLocation() {}

  /**
   * See referenced member.
   * @see Comparable#compareTo(Object)
   * @param other the other
   * @return the result
   */
  public int compareTo(PreviewLocation other) {
    if (this == other) return 0;
    int s = this.siteName.compareTo(other.getSiteName());
    if (s != 0) {
      return s;
    }
    s = this.path.compareTo(other.getPath());
    return s;
  }

  /**
   * See referenced member.
   * @see Object#equals(Object)
   * @param obj the obj
   * @return the result
   */
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * hashCode operation.
   *
   * @return the result
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns the siteName.
   * @return the siteName
   */
  public String getSiteName() {
    return siteName;
  }

  /**
   * Sets the siteName.
   * @param siteName the siteName to set
   */
  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  /**
   * Returns the path.
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   * @param path the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Returns the url.
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url.
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }
}

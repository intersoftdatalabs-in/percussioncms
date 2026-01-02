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

  public PreviewLocation() {}

<<<<<<< HEAD
  /**
   * @see Comparable#compareTo(Object)
   */
=======
  /** @see Comparable#compareTo(Object) */
>>>>>>> development-8.1.x
  public int compareTo(PreviewLocation other) {
    if (this == other) return 0;
    int s = this.siteName.compareTo(other.getSiteName());
    if (s != 0) {
      return s;
    }
    s = this.path.compareTo(other.getPath());
    return s;
  }

<<<<<<< HEAD
  /**
   * @see Object#equals(Object)
   */
=======
  /** @see Object#equals(Object) */
>>>>>>> development-8.1.x
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

<<<<<<< HEAD
  /**
   * @return the siteName
   */
=======
  /** @return the siteName */
>>>>>>> development-8.1.x
  public String getSiteName() {
    return siteName;
  }

<<<<<<< HEAD
  /**
   * @param siteName the siteName to set
   */
=======
  /** @param siteName the siteName to set */
>>>>>>> development-8.1.x
  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

<<<<<<< HEAD
  /**
   * @return the path
   */
=======
  /** @return the path */
>>>>>>> development-8.1.x
  public String getPath() {
    return path;
  }

<<<<<<< HEAD
  /**
   * @param path the path to set
   */
=======
  /** @param path the path to set */
>>>>>>> development-8.1.x
  public void setPath(String path) {
    this.path = path;
  }

<<<<<<< HEAD
  /**
   * @return the url
   */
=======
  /** @return the url */
>>>>>>> development-8.1.x
  public String getUrl() {
    return url;
  }

<<<<<<< HEAD
  /**
   * @param url the url to set
   */
=======
  /** @param url the url to set */
>>>>>>> development-8.1.x
  public void setUrl(String url) {
    this.url = url;
  }
}

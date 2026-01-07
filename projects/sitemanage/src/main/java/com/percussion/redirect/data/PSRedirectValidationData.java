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

package com.percussion.redirect.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.xml.bind.annotation.XmlRootElement;

/** Data object for redirect validation requests. */
@XmlRootElement(name = "data")
@JsonRootName("data")
public class PSRedirectValidationData {

  private String fromPath;
  private String toPath;
  private RedirectPathType type;

  /**
   * @return the source path for the redirect
   */
  public String getFromPath() {
    return fromPath;
  }

  /** Sets the source path for the redirect. */
  public void setFromPath(String fromPath) {
    this.fromPath = fromPath;
  }

  /**
   * @return the destination path for the redirect
   */
  public String getToPath() {
    return toPath;
  }

  /** Sets the destination path for the redirect. */
  public void setToPath(String toPath) {
    this.toPath = toPath;
  }

  /**
   * @return the redirect path type
   */
  public RedirectPathType getType() {
    return type;
  }

  /** Sets the redirect path type. */
  public void setType(RedirectPathType type) {
    this.type = type;
  }

  /** Enum for redirect path types. */
  public enum RedirectPathType {
    PAGE,
    FOLDER,
    SECTION,
    SITE
  }
}

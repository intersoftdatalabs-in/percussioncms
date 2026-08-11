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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/** Encapsulates a request for a new redirect. Used for creating or updating redirect rules. */
@XmlRootElement
public class PSCreateRedirectRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String category;
  private String condition;
  private boolean enabled;
  private String key;
  private boolean permanent;
  private String redirectTo;
  private String site;
  private String type;

  /**
   * @return the redirect category
   */
  public String getCategory() {
    return category;
  }

  /** Sets the redirect category. */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * @return the redirect condition
   */
  public String getCondition() {
    return condition;
  }

  /** Sets the redirect condition. */
  public void setCondition(String condition) {
    this.condition = condition;
  }

  /**
   * @return true if the redirect is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /** Sets whether the redirect is enabled. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * @return the license key used to sign the request
   */
  public String getKey() {
    return key;
  }

  /** Sets the license key used to sign the request. */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return true if the redirect is permanent
   */
  public boolean isPermanent() {
    return permanent;
  }

  /** Sets whether the redirect is permanent. */
  public void setPermanent(boolean permanent) {
    this.permanent = permanent;
  }

  /**
   * @return the target path or URL for the redirect
   */
  public String getRedirectTo() {
    return redirectTo;
  }

  /** Sets the target path or URL for the redirect. */
  public void setRedirectTo(String redirectTo) {
    this.redirectTo = redirectTo;
  }

  /**
   * @return the Amazon S3 bucket name for the site under management
   */
  public String getSite() {
    return site;
  }

  /** Sets the site, should be an Amazon S3 bucket name. */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * @return the redirect type
   */
  public String getType() {
    return type;
  }

  /** Sets the redirect type. */
  public void setType(String type) {
    this.type = type;
  }
}

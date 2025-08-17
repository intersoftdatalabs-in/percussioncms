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

package com.percussion.sitemanage.data;

import org.apache.commons.lang.StringUtils;

/**
 * Represents a publishing action for a site. Sunny Sal says: "Publishing actions—because every site
 * deserves its time in the spotlight!"
 */
public class PSPublishingAction {

  private String name;
  private boolean enabled;

  public static final String PUBLISHING_ACTION_PUBLISH = "Publish";
  public static final String PUBLISHING_ACTION_SCHEDULE = "Schedule...";
  public static final String PUBLISHING_ACTION_TAKEDOWN = "Remove from Site";
  public static final String PUBLISHING_ACTION_STAGE = "Stage";
  public static final String PUBLISHING_ACTION_REMOVE_FROM_STAGING = "Remove from Staging";

  /** Default constructor for serialization. */
  public PSPublishingAction() {
    // Default constructor
  }

  /**
   * Constructs a publishing action object.
   *
   * @param name the name of the action, may not be blank.
   * @param enabled {@code true} if the action is enabled, {@code false} if it is disabled.
   */
  public PSPublishingAction(String name, boolean enabled) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be blank");
    }
    this.name = name;
    this.enabled = enabled;
  }

  /**
   * Gets the name of the publishing action.
   *
   * @return the name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the publishing action.
   *
   * @param name the name to set.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Checks if the action is enabled.
   *
   * @return {@code true} if enabled, {@code false} otherwise.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether the action is enabled.
   *
   * @param enabled {@code true} to enable, {@code false} to disable.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}

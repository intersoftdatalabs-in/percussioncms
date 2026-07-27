// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.delivery.comments.data;

/**
 * Represents the default moderation state configured for a site.
 */
public interface IPSDefaultModerationState {

  /**
   * Gets the site name the default moderation state is associated with.
   *
   * @return the site name, never {@code null} or empty.
   */
  public abstract String getSite();

  /**
   * Sets the site name the default moderation state is associated with.
   *
   * @param site the site name, must not be {@code null} or empty.
   */
  public abstract void setSite(String site);

  /**
   * Gets the default moderation state for the site.
   *
   * @return the default approval state, never {@code null}.
   */
  public abstract String getDefaultState();

  /**
   * Sets the default moderation state for the site.
   *
   * @param defaultState the default approval state, must not be {@code null}.
   */
  public abstract void setDefaultState(String defaultState);
}

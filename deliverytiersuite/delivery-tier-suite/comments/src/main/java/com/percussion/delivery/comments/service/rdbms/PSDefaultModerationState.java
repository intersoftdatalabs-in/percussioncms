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
package com.percussion.delivery.comments.service.rdbms;

import com.percussion.delivery.comments.data.IPSDefaultModerationState;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Simple entity to store default moderation state for comments service.
 *
 * @author erikserating
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments2")
@Table(name = "PERC_DEFAULT_MODERATION_STATE")
public class PSDefaultModerationState implements IPSDefaultModerationState {

  @Id private String site;

  @Basic private String defaultState;

  /** Default no-arg constructor required by Hibernate. */
  public PSDefaultModerationState() {}

  /**
   * Creates a new default moderation state for the given site.
   *
   * @param site the site name, must not be blank.
   * @param defaultState the default approval state, must not be blank.
   */
  public PSDefaultModerationState(String site, String defaultState) {
    if (StringUtils.isBlank(site))
      throw new IllegalArgumentException("site cannot be null or empty.");
    if (StringUtils.isBlank(defaultState))
      throw new IllegalArgumentException("defaultState cannot be null or empty.");
    this.site = site;
    this.defaultState = defaultState;
  }

  /**
   * Gets the site name this default moderation state is associated with.
   *
   * @return the site name.
   */
  public String getSite() {
    return site;
  }

  /**
   * Sets the site name this default moderation state is associated with.
   *
   * @param site the site name.
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Gets the default approval state for the site.
   *
   * @return the default approval state.
   */
  public String getDefaultState() {
    return defaultState;
  }

  /**
   * Sets the default approval state for the site.
   *
   * @param defaultState the default approval state.
   */
  public void setDefaultState(String defaultState) {
    this.defaultState = defaultState;
  }
}

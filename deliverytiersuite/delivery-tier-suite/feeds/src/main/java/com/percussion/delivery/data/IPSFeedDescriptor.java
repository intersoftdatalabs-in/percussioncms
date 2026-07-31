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
package com.percussion.delivery.data;

import tools.jackson.databind.annotation.JsonDeserialize;
import com.percussion.delivery.feeds.data.PSFeedDescriptor;

/** A feed descriptor contains meta data needed to create a feed. */
@JsonDeserialize(as = PSFeedDescriptor.class)
public interface IPSFeedDescriptor {
  /**
   * Gets the name of the feed.
   *
   * @return the name of the feed.
   */
  public String getName();

  /**
   * Gets the name of the site the feed belongs to.
   *
   * @return the name of the site the feed belongs to.
   */
  public String getSite();

  /**
   * Gets the feed title.
   *
   * @return the feed title.
   */
  public String getTitle();

  /**
   * Gets the feed description.
   *
   * @return the feed description.
   */
  public String getDescription();

  /**
   * Gets the link to the page the feed represents.
   *
   * @return the link to the page the feed represents.
   */
  public String getLink();

  /**
   * Gets the query to get the feed data from the meta-data service.
   *
   * @return the query to get the feed data from the meta-data service.
   */
  public String getQuery();

  /**
   * Gets the feed output type.
   *
   * @return the feed output type. Never <code>null</code>.
   */
  public String getType();
}

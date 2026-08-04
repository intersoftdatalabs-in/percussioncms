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

package com.percussion.delivery.metadata.data;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Holds the time period / section path / limit / sort order parameters supplied by the "top visited
 * blog posts" REST request. Marshalled to / from JSON via the {@code @XmlRootElement} binding.
 */
@XmlRootElement(name = "visitQuery")
public class PSVisitQuery {
  private String sectionPath;
  private String promotedPagePaths;
  private String limit;
  private String timePeriod;
  private String sortOrder;

  /** No-arg constructor required by the JAX-RS / Jackson binding layer. */
  public PSVisitQuery() {}

  /**
   * Returns the time period for the query (one of {@code TODAY}, {@code WEEK}, {@code MONTH},
   * {@code YEAR}, {@code ALLTIME}).
   *
   * @return the timePeriod, may be <code>null</code>.
   */
  public String getTimePeriod() {
    return timePeriod;
  }

  /**
   * Sets the time period for the query.
   *
   * @param timePeriod the timePeriod to set; may be <code>null</code>.
   */
  public void setTimePeriod(String timePeriod) {
    this.timePeriod = timePeriod;
  }

  /**
   * Returns the section path to filter on.
   *
   * @return the sectionPath, may be <code>null</code>.
   */
  public String getSectionPath() {
    return sectionPath;
  }

  /**
   * Sets the section path to filter on.
   *
   * @param pagePath the section path to set; may be <code>null</code>.
   */
  public void setSectionPath(String pagePath) {
    this.sectionPath = pagePath;
  }

  /**
   * Returns the semicolon-separated list of promoted page paths surfaced before the regular
   * ranking.
   *
   * @return the promotedPagePaths, may be <code>null</code> or empty.
   */
  public String getPromotedPagePaths() {
    return promotedPagePaths;
  }

  /**
   * Sets the semicolon-separated list of promoted page paths.
   *
   * @param promotedPagePaths the promoted page paths to set; may be <code>null</code> or empty.
   */
  public void setPromotedPagePaths(String promotedPagePaths) {
    this.promotedPagePaths = promotedPagePaths;
  }

  /**
   * Returns the textual limit (optionally prefixed with {@code R-}) requested by the caller.
   *
   * @return the limit, may be <code>null</code>.
   */
  public String getLimit() {
    return limit;
  }

  /**
   * Sets the textual limit requested by the caller.
   *
   * @param limit the limit to set; may be <code>null</code>.
   */
  public void setLimit(String limit) {
    this.limit = limit;
  }

  /**
   * Returns the requested sort order ({@code asc} / {@code desc}).
   *
   * @return the sortOrder, may be <code>null</code>.
   */
  public String getSortOrder() {
    return sortOrder;
  }

  /**
   * Sets the sort order for the result.
   *
   * @param sortOrder the sortOrder to set; may be <code>null</code>.
   */
  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }
}

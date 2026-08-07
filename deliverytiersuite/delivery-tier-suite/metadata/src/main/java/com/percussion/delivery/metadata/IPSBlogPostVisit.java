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

package com.percussion.delivery.metadata;

import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Represents a single recorded visit to a blog post on a published site, capturing the page path,
 * the visit count and the date of the last hit. Implementations are typically persisted by {@link
 * IPSBlogPostVisitDao} and aggregated by {@link IPSBlogPostVisitService}.
 */
public interface IPSBlogPostVisit {
  /**
   * Returns the total number of recorded hits for the blog post.
   *
   * @return the cumulative hit count, never {@code null}.
   */
  public BigInteger getHitCount();

  /**
   * Sets the total number of recorded hits for the blog post.
   *
   * @param count the cumulative hit count to store; may be {@code null}.
   */
  public void setHitCount(BigInteger count);

  /**
   * Returns the calendar date of the most recent hit on the blog post.
   *
   * @return the hit date, may be {@code null} if never hit.
   */
  public LocalDate getHitDate();

  /**
   * Sets the calendar date of the most recent hit on the blog post.
   *
   * @param date the hit date to store; may be {@code null}.
   */
  public void setHitDate(LocalDate date);

  /**
   * Returns the published site-relative page path of the blog post that was visited.
   *
   * @return the page path, never {@code null} nor empty.
   */
  public String getPagepath();

  /**
   * Sets the published site-relative page path of the blog post that was visited.
   *
   * @param path the page path to store; may be {@code null}.
   */
  public void setPagepath(String path);
}

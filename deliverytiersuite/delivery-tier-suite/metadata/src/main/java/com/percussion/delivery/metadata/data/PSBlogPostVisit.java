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

import com.percussion.delivery.metadata.IPSBlogPostVisit;
import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Lightweight transport model for blog-post visit data captured by the DTS blog visit tracking
 * service. Mirrors {@link IPSBlogPostVisit} but lives in the {@code data} package so it can be
 * marshalled to JSON without dragging in the Hibernate entity that backs persistence.
 */
public class PSBlogPostVisit implements IPSBlogPostVisit {

  /** Published site-relative page path of the visited blog post. */
  private String pagePath;

  /** Calendar date the visit occurred. {@link LocalDate} is immutable. */
  private LocalDate date;

  /** Cumulative hit count for this page path. */
  private BigInteger hitCount;

  /** No-arg constructor required by the JSON / JAXB binding layer. */
  public PSBlogPostVisit() {}

  /**
   * Construct a fully-populated visit record.
   *
   * @param pagePath the page path of the visited blog post; may be {@code null}.
   * @param date the date the visit occurred; may be {@code null}.
   * @param hitCount the cumulative hit count; may be {@code null}.
   */
  public PSBlogPostVisit(String pagePath, LocalDate date, BigInteger hitCount) {
    this.pagePath = pagePath;
    this.date = date;
    this.hitCount = hitCount;
  }

  @Override
  public BigInteger getHitCount() {
    return hitCount;
  }

  @Override
  public void setHitCount(BigInteger count) {
    hitCount = count;
  }

  @Override
  public LocalDate getHitDate() {
    return date;
  }

  @Override
  public void setHitDate(LocalDate date) {
    this.date = date;
  }

  @Override
  public String getPagepath() {
    return this.pagePath;
  }

  @Override
  public void setPagepath(String pagePath) {
    this.pagePath = pagePath;
  }
}

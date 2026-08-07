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
package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSBlogPostVisit;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigInteger;
import java.time.LocalDate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Hibernate-backed entity that represents a single recorded visit to a published blog post.
 *
 * <p>Page visit object. {@code hitDate} is a {@link LocalDate} (JPA DATE) so Hibernate 7 maps it
 * without deprecated {@code @Temporal}.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSBlogPostVisit")
@Table(name = "BLOG_POST_VISIT")
public final class PSDbBlogPostVisit implements IPSBlogPostVisit {

  /** Surrogate primary key for this visit row. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "VISIT_ID")
  private long visitId;

  /** Published site-relative page path of the visited blog post. */
  @Column(length = 2000)
  private String pagepath;

  /** Calendar date of the visit. */
  @Basic private LocalDate hitDate;

  /** Cumulative hit count for the page path on {@link #hitDate}. */
  @Basic private BigInteger hitCount;

  /** No-arg constructor required by Hibernate. */
  public PSDbBlogPostVisit() {}

  /**
   * Constructs a fully-populated visit entity.
   *
   * @param pagepath the page path of the visited blog post; may not be {@code null} or empty.
   * @param hitDate the date the visit occurred; may not be {@code null}.
   * @param hitCount the cumulative hit count for this page path; may not be {@code null}.
   */
  public PSDbBlogPostVisit(String pagepath, LocalDate hitDate, BigInteger hitCount) {
    if (pagepath == null || pagepath.length() == 0)
      throw new IllegalArgumentException("pagepath cannot be null or empty");
    if (hitDate == null) throw new IllegalArgumentException("hitDate cannot be null");
    if (hitCount == null) throw new IllegalArgumentException("hitCount cannot be null");

    // Direct field assignment; class is final (no this-escape via overridable setters).
    this.hitCount = hitCount;
    this.hitDate = hitDate;
    this.pagepath = pagepath;
  }

  /**
   * @return the page path
   */
  public String getPagepath() {
    return pagepath;
  }

  /**
   * @param path the pagepath to set
   */
  public void setPagepath(String path) {
    this.pagepath = path;
  }

  /**
   * Returns the date the visit occurred.
   *
   * @return the hit date, may be {@code null}.
   */
  public LocalDate getHitDate() {
    return hitDate;
  }

  /**
   * Sets the date the visit occurred.
   *
   * @param hitDate the hit date to set; may be {@code null}.
   */
  public void setHitDate(LocalDate hitDate) {
    this.hitDate = hitDate;
  }

  /**
   * Returns the cumulative hit count for this page path.
   *
   * @return the hit count, may be {@code null}.
   */
  public BigInteger getHitCount() {
    return hitCount;
  }

  /**
   * Sets the cumulative hit count for this page path.
   *
   * @param hitCount the hit count to set; may not be {@code null}.
   */
  public void setHitCount(BigInteger hitCount) {
    this.hitCount = hitCount;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !getClass().getName().equals(obj.getClass().getName())) return false;
    PSDbBlogPostVisit visits = (PSDbBlogPostVisit) obj;
    return new EqualsBuilder()
        .append(hitDate, visits.hitDate)
        .append(hitCount, visits.hitCount)
        .append(pagepath, visits.pagepath)
        .isEquals();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(hitDate).append(hitCount).append(pagepath).toHashCode();
  }
}

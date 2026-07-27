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
package com.percussion.delivery.likes.service.rdbms;

import com.percussion.delivery.likes.data.IPSLikes;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * RDBMS-backed entity representing a like count for a (site, likeId, type) tuple. Stored in the
 * {@code PERC_PAGE_LIKES} table with a uniqueness constraint on those three columns.
 *
 * @author davidpardini
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSLikes1")
@Table(
    name = "PERC_PAGE_LIKES",
    uniqueConstraints = @UniqueConstraint(columnNames = {"site", "likeId", "type"}))
public class PSLikes implements IPSLikes, Serializable {
  private static final long serialVersionUID = 1L;

  /** Unique identifier for the like row. Assigned by the persistence layer. */
  @TableGenerator(
      name = "likesId",
      table = "PERC_ID_GEN",
      pkColumnName = "GEN_KEY",
      valueColumnName = "GEN_VALUE",
      pkColumnValue = "likesId",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "likesId")
  private long id;

  /** Site the like belongs to. */
  @Basic private String site;

  /** Identifier of the liked entity. */
  @Basic private String likeId;

  /** Type of liked entity (page, comment, image). */
  @Basic private String type;

  /** Current total number of likes. */
  @Basic private int total;

  /** Default no-arg constructor required by Hibernate. */
  public PSLikes() {}

  /**
   * Creates a new likes with the same values as the given one, except for the id.
   *
   * @param likes A Likes to create a copy from.
   */
  public PSLikes(IPSLikes likes) {
    this.type = likes.getType();
    this.site = likes.getSite();
    this.likeId = likes.getLikeId();
    this.total = likes.getTotal();
  }

  /**
   * Creates a new likes row for the given site, like id and type.
   *
   * @param site the site name, must not be {@code null}.
   * @param likeId the like id, must not be {@code null}.
   * @param type the like type, must not be {@code null}.
   */
  public PSLikes(String site, String likeId, String type) {
    super();
    this.site = site;
    this.likeId = likeId;
    this.type = type;
  }

  /**
   * Sets the unique identifier for this like.
   *
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id == null ? 0 : Long.valueOf(id);
  }

  /**
   * Gets the like identifier.
   *
   * @return the likeId
   */
  public String getLikeId() {
    return likeId;
  }

  /**
   * Sets the like identifier.
   *
   * @param likeId the likeId to set
   */
  public void setLikeId(String likeId) {
    this.likeId = likeId;
  }

  /**
   * Gets the type of liked entity.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of liked entity.
   *
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the site this like belongs to.
   *
   * @return the site
   */
  public String getSite() {
    return site;
  }

  /**
   * Sets the site this like belongs to.
   *
   * @param site the site to set
   */
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Gets the unique identifier for this like.
   *
   * @return the id
   */
  public String getId() {
    return String.valueOf(id);
  }

  /**
   * Gets the current total number of likes.
   *
   * @return the total
   */
  public int getTotal() {
    return total;
  }

  /**
   * Sets the current total number of likes.
   *
   * @param total the total to set
   */
  public void setTotal(int total) {
    this.total = total;
  }
}

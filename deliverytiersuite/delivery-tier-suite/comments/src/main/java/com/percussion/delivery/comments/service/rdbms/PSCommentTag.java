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

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import java.io.Serializable;

/**
 * Represents a tag attached to a comment. Tags are stored in their own table and linked back to a
 * {@link PSComment} via a many-to-one relationship.
 *
 * @author miltonpividori
 */
@Entity
@Table(name = "PERC_COMMENT_TAGS")
public class PSCommentTag implements Serializable {
  private static final long serialVersionUID = 1L;

  @TableGenerator(
      name = "commentTagId",
      table = "PERC_ID_GEN",
      pkColumnName = "GEN_KEY",
      valueColumnName = "GEN_VALUE",
      pkColumnValue = "commentTagId",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "commentTagId")
  private long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "COMMENT_ID")
  PSComment comment;

  @Basic private String name;

  /** Default no-arg constructor required by Hibernate. */
  public PSCommentTag() {}

  /**
   * Creates a new tag with the given name.
   *
   * @param name the tag name, must not be {@code null}.
   */
  public PSCommentTag(String name) {
    this.name = name;
  }

  /**
   * Gets the unique id assigned to this tag.
   *
   * @return the id.
   */
  public long getId() {
    return id;
  }

  /**
   * Gets the comment this tag is attached to.
   *
   * @return the parent {@link PSComment}.
   */
  public PSComment getComment() {
    return comment;
  }

  /**
   * Sets the comment this tag is attached to.
   *
   * @param comment the parent {@link PSComment}.
   */
  public final void setComment(PSComment comment) {
    this.comment = comment;
  }

  /**
   * Gets the tag name.
   *
   * @return the tag name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the tag name.
   *
   * @param name the tag name.
   */
  public void setName(String name) {
    this.name = name;
  }
}

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
package com.percussion.delivery.likes.data;

/**
 * Represents a REST like entity in the system.
 *
 * @author davidpardini
 */
public class PSRestLikes implements IPSLikes {

  /** Default no-arg constructor required by Jackson and JAXB. */
  public PSRestLikes() {}

  /** The unique identifier for this like. */
  private String id;
  private String likeId;
  /** The type of liked entity (page, comment, image). */
  private String type;
  /** The site this like belongs to. */
  private String site;
  /** The current total number of likes. */
  private int total;

  /**
   * Gets the unique identifier for this like.
   *
   * @return the ID.
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Sets the unique identifier for this like.
   *
   * @param id the ID to set.
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the like identifier.
   *
   * @return the like ID.
   */
  @Override
  public String getLikeId() {
    return likeId;
  }

  /**
   * Sets the like identifier.
   *
   * @param likeId the like ID to set.
   */
  @Override
  public void setLikeId(String likeId) {
    this.likeId = likeId;
  }

  /**
   * Gets the type of like.
   *
   * @return the type.
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Sets the type of like.
   *
   * @param type the type to set.
   */
  @Override
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the site associated with this like.
   *
   * @return the site name.
   */
  @Override
  public String getSite() {
    return site;
  }

  /**
   * Sets the site for this like.
   *
   * @param site the site name to set.
   */
  @Override
  public void setSite(String site) {
    this.site = site;
  }

  /**
   * Gets the total number of likes.
   *
   * @return the total count.
   */
  @Override
  public int getTotal() {
    return total;
  }

  /**
   * Sets the total number of likes.
   *
   * @param total the total count to set.
   */
  @Override
  public void setTotal(int total) {
    this.total = total;
  }
}

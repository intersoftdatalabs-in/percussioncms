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
 * Represents a like entity in the system. Provides accessors and mutators for like properties.
 *
 * @author Administrator
 */
public interface IPSLikes {

  /**
   * Gets the site associated with this like.
   *
   * @return the site name, never null or empty.
   */
  String getSite();

  /**
   * Gets the type of like (e.g., page, comment, image).
   *
   * @return the type as a string.
   */
  String getType();

  /**
   * Gets the like identifier.
   *
   * @return the like ID.
   */
  String getLikeId();

  /**
   * Gets the total number of likes.
   *
   * @return the total count.
   */
  int getTotal();

  /**
   * Gets the unique identifier for this like.
   *
   * @return the ID.
   */
  String getId();

  /**
   * Sets the unique identifier for this like.
   *
   * @param id the ID to set.
   */
  void setId(String id);

  /**
   * Sets the site for this like.
   *
   * @param site the site name.
   */
  void setSite(String site);

  /**
   * Sets the type of like.
   *
   * @param type the type to set.
   */
  void setType(String type);

  /**
   * Sets the like identifier.
   *
   * @param id the like ID.
   */
  void setLikeId(String id);

  /**
   * Sets the total number of likes.
   *
   * @param total the total count.
   */
  void setTotal(int total);

  /** Like types. */
  enum Type {
    /** Like applied to a page. */
    page,
    /** Like applied to a comment. */
    comment,
    /** Like applied to an image. */
    image
  }
}

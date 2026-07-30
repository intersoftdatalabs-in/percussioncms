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

package com.percussion.delivery.likes.services;

import com.percussion.delivery.likes.data.IPSLikes;
import java.util.Collection;
import java.util.List;

/** Data access interface for likes. */
public interface IPSLikesDao {
  /**
   * Finds likes for the specified site, like id and type.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @param likeId the like id, must not be {@code null} or empty.
   * @param type the like type, must not be {@code null}.
   * @return list of matching likes, never {@code null}, may be empty.
   * @throws Exception if an error occurs during the lookup.
   */
  public List<IPSLikes> find(String site, String likeId, String type) throws Exception;

  /**
   * Finds all likes associated with the specified site.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @return list of likes for the site, never {@code null}, may be empty.
   * @throws Exception if an error occurs during the lookup.
   */
  public List<IPSLikes> findLikesForSite(String site) throws Exception;

  /**
   * Deletes the likes with the specified ids.
   *
   * @param ids the collection of like ids to delete, must not be {@code null}.
   * @throws Exception if an error occurs during deletion.
   */
  public void delete(Collection<String> ids) throws Exception;

  /**
   * Persists a single like.
   *
   * @param like the like to save, must not be {@code null}.
   * @throws Exception if an error occurs during the save.
   */
  public void save(IPSLikes like) throws Exception;

  /**
   * Persists a list of likes.
   *
   * @param likes the likes to save, must not be {@code null}.
   * @throws Exception if an error occurs during the save.
   */
  public void save(List<IPSLikes> likes) throws Exception;

  /**
   * Creates a new like entry for the specified site, like id and type.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @param likeId the like id, must not be {@code null} or empty.
   * @param type the like type, must not be {@code null}.
   * @return the newly created like, never {@code null}.
   * @throws Exception if an error occurs during creation.
   */
  public IPSLikes create(String site, String likeId, String type) throws Exception;

  /**
   * Increments the total number of likes for the specified entity.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @param likeId the like id, must not be {@code null} or empty.
   * @param type the like type, must not be {@code null}.
   * @return the updated total count of likes.
   * @throws Exception if an error occurs during the increment.
   */
  public int incrementTotal(String site, String likeId, String type) throws Exception;

  /**
   * Decrements the total number of likes for the specified entity.
   *
   * @param site the site name, must not be {@code null} or empty.
   * @param likeId the like id, must not be {@code null} or empty.
   * @param type the like type, must not be {@code null}.
   * @return the updated total count of likes.
   * @throws Exception if an error occurs during the decrement.
   */
  public int decrementTotal(String site, String likeId, String type) throws Exception;
}

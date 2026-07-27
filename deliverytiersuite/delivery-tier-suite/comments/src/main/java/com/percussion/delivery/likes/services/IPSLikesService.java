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

/**
 * The likes service is used to store, total and show total rating - "likes" only. It will run in
 * the delivery tier.
 *
 * @author davidpardini
 */
public interface IPSLikesService {

  /**
   * Returns the total number of likes for the given URL, site and type
   *
   * @param site the site the like is associated with. Must not be {@code null} or empty.
   * @param likeId the id of the liked entity. Must not be {@code null} or empty.
   * @param type the type of the liked entity (page, comment, image). Must not be {@code null}.
   * @return int total of likes for the specified entity.
   */
  public int getTotalLikes(String site, String likeId, String type);

  /**
   * Increments the total number of likes for the given URL and returns the number of total likes
   *
   * @param site the site the like is associated with. Must not be {@code null} or empty.
   * @param likeId the id of the liked entity. Must not be {@code null} or empty.
   * @param type the type of the liked entity (page, comment, image). Must not be {@code null}.
   * @return int total of likes after the increment.
   */
  public int like(String site, String likeId, String type);

  /**
   * Decrements the total number of likes for the given URL and returns the number of total likes
   *
   * @param site the site the like is associated with. Must not be {@code null} or empty.
   * @param likeId the id of the liked entity. Must not be {@code null} or empty.
   * @param type the type of the liked entity (page, comment, image). Must not be {@code null}.
   * @return int total of likes after the decrement.
   */
  public int unlike(String site, String likeId, String type);

  /**
   * After a site rename in CM1, this method needs to be called to update the current likes for a
   * page so that they are pointed to the new site.
   *
   * @param prevSiteName the old site name.
   * @param newSiteName the new site name.
   */
  public void updateLikesForSiteAfterRename(String prevSiteName, String newSiteName);
}

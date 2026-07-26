// REFACTORED: CP-JAVA11
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

package com.percussion.activity.service;

import com.percussion.activity.data.PSActivityNode;
import com.percussion.activity.data.PSContentActivity;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.utils.date.PSDateRange;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Service for retrieving content activity for a single site or all sites.
 *
 * <p>Sunny Sal says: "Activity is the spice of CMS life!"
 */
public interface IPSActivityService {

  /**
   * Creates a content activity representation for the given activity node starting from the
   * specified date.
   *
   * @param node the activity node, must not be {@code null}.
   * @param beginDate the starting date for the content activity, must not be {@code null}.
   * @param timeout The number of milliseconds after which the operation should abort.
   * @return {@link PSContentActivity} object, never {@code null}.
   * @throws PSActivityServiceException if there is a timeout or error.
   */
  PSContentActivity createActivity(PSActivityNode node, Date beginDate, long timeout)
      throws PSActivityServiceException, IPSPathService.PSPathServiceException;

  /**
   * Gets a site and path pairs for the specified path. The returned list is all sites if the path
   * is {@code PSPathUtils#SITES_FINDER_ROOT}.
   *
   * @param path the path in question, must not be blank.
   * @param includeSite {@code true} to include an activity node for the site(s), {@code false}
   *     otherwise.
   * @return the site/path pairs, never {@code null}, but may be empty.
   */
  List<PSActivityNode> createActivityNodesByPaths(String path, boolean includeSite);

  /**
   * Creates a date range from the supplied values.
   *
   * @param start A date in the format yyyy-MM-dd. Never {@code null} or empty.
   * @param end A date in the format yyyy-MM-dd. Never {@code null} or empty.
   * @param granularity The string representation of one of the values of {@link
   *     PSDateRange.Granularity}. Never {@code null} or empty.
   * @return Never {@code null}.
   */
  PSDateRange createDateRange(String start, String end, String granularity);

  /**
   * Finds all page items under a specified folder path.
   *
   * @param path the specified folder path, not {@code null} or empty.
   * @return a list of content IDs of the page items under the specified folder.
   */
  Collection<Integer> findPageIdsByPath(String path);

  /**
   * Finds all items under a specified folder path for given content types.
   *
   * @param path the specified folder path, not {@code null} or empty.
   * @param contentTypes a list of content type names, may be {@code null} or empty (equivalent to
   *     all types).
   * @return a list of content IDs of the items under the specified folder.
   */
  Collection<Integer> findItemIdsByPath(String path, Collection<String> contentTypes);

  /**
   * Finds the number of newly published items (among the specified items) for the specified date
   * ranges.
   *
   * @param contentIds a list of specified item IDs, not {@code null}.
   * @param dates a list of specified date range, must contain more than one Date element.
   * @return a list of newly published items in the same order as the specified date ranges.
   */
  List<Integer> findNewContentActivities(Collection<Integer> contentIds, List<Date> dates);

  /**
   * Finds the number of content activities (among the specified items) for the specified date
   * ranges.
   *
   * @param contentIds a list of specified item IDs, not {@code null}.
   * @param dates a list of specified date range, must contain more than one Date element.
   * @param stateName the workflow state name the items transition to, not {@code null} or empty.
   * @param transitionName the transition name that is used to transition the items to the above
   *     state, not {@code null} or empty.
   * @return a list of content activities in the same order as the specified date ranges.
   */
  List<Integer> findNumberContentActivities(
      Collection<Integer> contentIds, List<Date> dates, String stateName, String transitionName);

  /**
   * Finds collection of page ids that have had content activities (among the specified items) for
   * the specified date ranges.
   *
   * @param contentIds a list of specified item IDs, not {@code null}.
   * @param beginDate The starting date, inclusive. Never {@code null}.
   * @param endDate The ending date, exclusive. Never {@code null}.
   * @param stateName the workflow state name the items transition to, not {@code null} or empty.
   * @param transitionName the transition name that is used to transition the items to the above
   *     state, not {@code null} or empty.
   * @return a list of content activities in the specified date ranges. If beginDate is not {@code
   *     <=} endDate, no data will be returned.
   */
  List<String> findPageIdsContentActivities(
      Collection<Integer> contentIds,
      Date beginDate,
      Date endDate,
      String stateName,
      String transitionName);

  /**
   * Finds the number of published items (among the specified items) for the specified date ranges.
   *
   * @param contentIds a list of specified item IDs, not {@code null}.
   * @param dates a list of specified date range, must contain more than one Date element.
   * @return a list of published items in the same order as the specified date ranges.
   */
  List<Integer> findPublishedItems(Collection<Integer> contentIds, List<Date> dates);

  /**
   * Finds the published items (among the specified items).
   *
   * @param contentIds a list of specified item IDs, not {@code null}.
   * @return a collection of published item IDs, never {@code null}, may be empty.
   */
  Collection<Long> findPublishedItems(Collection<Integer> contentIds);

  /** Exception thrown when an error occurs in the activity service. */
  class PSActivityServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public PSActivityServiceException(String message) {
      super(message);
    }
  }
}

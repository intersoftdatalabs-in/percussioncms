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

package com.percussion.delivery.comments.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentSort;

/**
 * Utility class for converting between different comment-related object types. Used only for
 * testing purposes.
 */
public final class CommentConverter {

  private CommentConverter() {
    // Private constructor to prevent instantiation
  }

  /**
   * Converts data package criteria to bean package criteria.
   *
   * @param dataCriteria The data package criteria to convert
   * @return A new bean package criteria object with copied values
   */
  public static PSCommentCriteria convertToBeanCriteria(
      final com.percussion.delivery.comments.data.PSCommentCriteria dataCriteria) {
    if (dataCriteria == null) {
      return null;
    }

    final PSCommentCriteria beanCriteria =
        new PSCommentCriteria();
    beanCriteria.setPagepath(dataCriteria.getPagepath());
    beanCriteria.setSite(dataCriteria.getSite());
    beanCriteria.setUsername(dataCriteria.getUsername());
    beanCriteria.setTag(dataCriteria.getTag());
    beanCriteria.setViewed(dataCriteria.getViewed());
    beanCriteria.setModerated(dataCriteria.getModerated());
    beanCriteria.setState(CommentConverter.convertToModelState(dataCriteria.getState()));
    beanCriteria.setMaxResults(dataCriteria.getMaxResults());
    beanCriteria.setStartIndex(dataCriteria.getStartIndex());
    beanCriteria.setLastCommentId(dataCriteria.getLastCommentId());

    if (dataCriteria.getSort() != null) {
      beanCriteria.setSort(CommentConverter.convertToBeanSort(dataCriteria.getSort()));
    }

    return beanCriteria;
  }

  /**
   * Converts a Collection of Strings to a List of Strings.
   *
   * @param collection The collection to convert
   * @return A new ArrayList containing the elements from the collection
   */
  public static List<String> convertToList(final Collection<String> collection) {
    if (collection == null) {
      return null;
    }
    return new ArrayList<>(collection);
  }

  /**
   * Converts data package sort to bean package sort.
   *
   * @param dataSort The data package sort to convert
   * @return A new bean package sort object with copied values
   */
  private static PSCommentSort convertToBeanSort(
      final com.percussion.delivery.comments.data.PSCommentSort dataSort) {
    if (dataSort == null) {
      return null;
    }

    PSCommentSort.SORTBY sortBy = null;
    if (dataSort.getSortBy() != null) {
      switch (dataSort.getSortBy()) {
        case CREATEDDATE:
          sortBy = PSCommentSort.SORTBY.CREATEDDATE;
          break;
        case USERNAME:
          sortBy = PSCommentSort.SORTBY.USERNAME;
          break;
      }
    }

    return new PSCommentSort(sortBy, dataSort.isAscending());
  }

  /**
   * Converts data package approval state to model approval state.
   *
   * @param dataState The data package approval state to convert
   * @return The corresponding model approval state
   */
  private static IPSComment.APPROVAL_STATE convertToModelState(
      final com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE dataState) {
    if (dataState == null) {
      return null;
    }

    return switch (dataState) {
        case APPROVED -> IPSComment.APPROVAL_STATE.APPROVED;
        case REJECTED -> IPSComment.APPROVAL_STATE.REJECTED;
        default -> null;
    };
  }
}

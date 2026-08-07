/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Package-private typed list helpers for workflow role/assignment code.
 *
 * <p>These mirror the semantics of the corresponding raw-type methods on {@link PSWorkFlowUtils}
 * ({@code intersectLists}, {@code filterList}, {@code lowerCaseList}) so call sites in this module
 * can use {@code List<T>} without unchecked conversions from the legacy raw APIs.
 */
final class PSTypedWorkflowLists {

  private PSTypedWorkflowLists() {}

  /**
   * Intersect two lists, returning elements of the first that are contained in the second.
   *
   * @param list1 first list, may be {@code null}
   * @param list2 second list, may be {@code null}
   * @param <T> element type
   * @return intersection list (possibly empty), or {@code null} if either input is {@code null}
   */
  static <T> List<T> intersectLists(List<T> list1, List<T> list2) {
    if (null == list1 || null == list2) {
      return null;
    }
    List<T> newList = new ArrayList<>();
    if (list1.isEmpty() || list2.isEmpty()) {
      return newList;
    }
    for (T item : list1) {
      if (list2.contains(item)) {
        newList.add(item);
      }
    }
    return newList;
  }

  /**
   * Retain list members whose map entry is {@link Boolean#TRUE}.
   *
   * @param inputList list of keys, may be {@code null}
   * @param map boolean map keyed by list element type, may be {@code null}
   * @param <K> key type
   * @return filtered list (possibly empty), or {@code null} if input is {@code null} or map is
   *     {@code null}/empty
   */
  static <K> List<K> filterList(List<K> inputList, Map<K, Boolean> map) {
    if (null == inputList || null == map || map.isEmpty()) {
      return null;
    }
    List<K> filteredList = new ArrayList<>();
    if (inputList.isEmpty()) {
      return filteredList;
    }
    for (K key : inputList) {
      if (null != key) {
        Boolean val = map.get(key);
        if (null != val && val) {
          filteredList.add(key);
        }
      }
    }
    return filteredList;
  }

  /**
   * Create a lower-case version of a list of strings.
   *
   * @param inputList list of strings, may be {@code null}
   * @return list with lower-cased items, or {@code null} if input was {@code null}
   */
  static List<String> lowerCaseList(List<String> inputList) {
    if (null == inputList) {
      return null;
    }
    List<String> newList = new ArrayList<>(inputList.size());
    for (String item : inputList) {
      if (null != item) {
        newList.add(item.toLowerCase());
      } else {
        newList.add(null);
      }
    }
    return newList;
  }
}

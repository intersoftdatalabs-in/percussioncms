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

package com.percussion.share.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections.Predicate;

/**
 * Abstract filter for filtering collections using a predicate.
 *
 * @param <T> the type of resource to filter
 */
public abstract class PSAbstractFilter<T> implements Predicate {

  /**
   * Filters the given collection using this predicate.
   *
   * @param resources the collection to filter
   * @return a new list containing only the elements that should be kept
   */
  public List<T> filter(Collection<T> resources) {
    // Prefer typed loop over CollectionUtils.filter (raw Predicate API).
    var rvalue = new ArrayList<T>(resources.size());
    for (T resource : resources) {
      if (shouldKeep(resource)) {
        rvalue.add(resource);
      }
    }
    return rvalue;
  }

  @Override
  @SuppressWarnings("unchecked") // commons-collections Predicate is raw; domain type is T
  public boolean evaluate(Object obj) {
    return shouldKeep((T) obj);
  }

  /**
   * Determines if the given resource should be kept.
   *
   * @param resource the resource to check
   * @return true if the resource should be kept, false otherwise
   */
  public abstract boolean shouldKeep(T resource);
}

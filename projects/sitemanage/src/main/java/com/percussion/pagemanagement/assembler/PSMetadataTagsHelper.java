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
package com.percussion.pagemanagement.assembler;

import jakarta.servlet.ServletException;
import java.util.*;
import java.util.stream.Collectors;
import net.sf.json.JSONException;

/** Helper for processing metadata tags and their occurrences. */
public class PSMetadataTagsHelper {

  public static final String REFERENCES = "perc:tags";
  public static final String TAG_NAME = "tagName";
  public static final String TAG_COUNT = "tagCount";
  public static final String PROPERTIES = "properties";
  public static final String COUNT_SORT = "count";

  /**
   * Returns a map of tags and their occurrences, sorted as requested.
   *
   * @param results assumed not null
   * @param sortOrder sort order ("count" or alpha)
   * @return map of tag to count
   * @throws ServletException on error
   */
  public Map<String, Integer> processTags(List<PSMetadataEntry> results, String sortOrder)
      throws ServletException {
    var arrayPages = initializeArray(results);
    Map<String, Integer> tagsMap = new TreeMap<>();
    try {
      int i = 0;
      for (var entryPage : results) {
        for (var prop : entryPage.getProperties()) {
          if (REFERENCES.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
            countTags(tagsMap, prop.getStringvalue().trim(), arrayPages.get(i));
          }
        }
        i++;
      }
      if (COUNT_SORT.equals(sortOrder)) {
        tagsMap = sortByCountOrder(tagsMap);
      } else {
        tagsMap = sortByAlphaOrder(tagsMap);
      }
      return tagsMap;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Counts tags and updates the tags map.
   *
   * @param tagsMap not null
   * @param stringvalue not null
   * @param arrayList not null
   */
  private void countTags(Map<String, Integer> tagsMap, String stringvalue, List<String> arrayList) {
    var tag = stringvalue.trim().toLowerCase();
    try {
      if (tagsMap.containsKey(tag)) {
        if (!arrayList.contains(tag)) {
          tagsMap.put(tag, tagsMap.get(tag) + 1);
        }
      } else {
        tagsMap.put(tag, 1);
        arrayList.add(tag);
      }
    } catch (Exception e) {
      // Swallow exception, but log if needed
    }
  }

  private Map<String, Integer> sortByAlphaOrder(Map<String, Integer> tagsMap) throws JSONException {
    return tagsMap.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
  }

  private Map<String, Integer> sortByCountOrder(Map<String, Integer> tagObjects)
      throws JSONException {
    return tagObjects.entrySet().stream()
        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
  }

  /**
   * Initializes a list of arrays for each page to avoid duplicate tags.
   *
   * @param results not null
   * @return list of lists for each page
   */
  private List<List<String>> initializeArray(List<PSMetadataEntry> results) {
    return results.stream().map(r -> new ArrayList<String>()).collect(Collectors.toList());
  }
}

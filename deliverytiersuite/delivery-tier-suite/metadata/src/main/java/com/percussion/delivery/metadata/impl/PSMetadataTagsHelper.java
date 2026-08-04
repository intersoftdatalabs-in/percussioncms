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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.impl.utils.PSPair;
import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is responsible for process the tags list metadata and return return the JSONObject
 * with the list properties with tags and their occurrences.
 *
 * @author davidpardini
 */
public class PSMetadataTagsHelper {

  /** Metadata property name used to look up tag references on a metadata entry. */
  public static final String REFERENCES = "perc:tags";

  /** JSON / map key carrying the tag name. */
  public static final String TAG_NAME = "tagName";

  /** JSON / map key carrying the tag occurrence count. */
  public static final String TAG_COUNT = "tagCount";

  /** JSON / map key carrying the list of property entries. */
  public static final String PROPERTIES = "properties";

  /** Sort-mode value used to indicate descending-by-count ordering. */
  public static final String COUNT_SORT = "count";

  /** No-arg constructor; the helper is stateless aside from its comparator instances. */
  public PSMetadataTagsHelper() {}

  /**
   * Returns the list of (tag, occurrence) pairs for the supplied metadata entries. First iterates
   * by page and later by property page.
   *
   * @param results the metadata entries to process; assumed not <code>null</code>.
   * @param sortOrder the sort mode - {@code "count"} to sort by descending occurrence, anything
   *     else sorts alphabetically; may be <code>null</code>.
   * @return a list of {@link PSPair} entries where {@code first} is the tag name and {@code second}
   *     is the occurrence count; never <code>null</code>, may be empty.
   * @throws ServletException if the iteration fails.
   */
  public List<PSPair<String, Integer>> processTags(List<IPSMetadataEntry> results, String sortOrder)
      throws ServletException {
    // Initialize array used for unduplicated tags
    List<ArrayList<String>> arrayPages = inicializeArray(results);

    Map<String, Integer> tagsMap = new HashMap<>();
    try {
      int i = 0;
      for (IPSMetadataEntry entryPage : results) {
        for (IPSMetadataProperty prop : entryPage.getProperties()) {
          if (REFERENCES.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
            countTags(tagsMap, prop.getStringvalue().trim(), arrayPages.get(i));
          }
        }
        i++;
      }

      List<PSPair<String, Integer>> tagResultList = new ArrayList<>();
      for (Entry<String, Integer> tagEntry : tagsMap.entrySet()) {
        tagResultList.add(new PSPair<>(tagEntry.getKey(), tagEntry.getValue()));
      }

      // SORT BY ..
      Comparator<PSPair<String, Integer>> comp = new AlphaOrderTagComparator();
      if (COUNT_SORT.equalsIgnoreCase(sortOrder)) {
        comp = new CountOrderTagComparator();
      }
      Collections.sort(tagResultList, comp);

      return tagResultList;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  class AlphaOrderTagComparator implements Comparator<PSPair<String, Integer>> {
    public int compare(PSPair<String, Integer> o1, PSPair<String, Integer> o2) {
      return o1.getFirst().compareTo(o2.getFirst());
    }
  }

  class CountOrderTagComparator implements Comparator<PSPair<String, Integer>> {
    public int compare(PSPair<String, Integer> o1, PSPair<String, Integer> o2) {
      return o1.getSecond().equals(o2.getSecond())
          ? o1.getFirst().compareTo(o2.getFirst())
          : o2.getSecond().compareTo(o1.getSecond());
    }
  }

  /**
   * This method is responsible for return the maps with the tags and their occurrences. First split
   * the stringValue parameter with the tags and add the maps, if the tags already adds 1 to its
   * respective value.
   *
   * @param tagsMap assumed not <code>null</code>.
   * @param stringvalue assumed not <code>null</code>.
   * @param arrayList assumed not <code>null</code>.
   */
  private void countTags(
      Map<String, Integer> tagsMap, String stringvalue, ArrayList<String> arrayList) {
    String tag = stringvalue.trim().toLowerCase();
    try {
      if (tagsMap.containsKey(tag)) {
        if (!arrayList.contains(tag)) {
          int count = tagsMap.get(tag).intValue();
          count++;
          tagsMap.put(tag, Integer.valueOf(count));
        }
      } else {
        tagsMap.put(tag, Integer.valueOf(1));
        arrayList.add(tag);
      }
    } catch (Exception e) {
    }
  }

  /**
   * This method is responsible for return the List with the list of quantity of pages. Returns a
   * list of arrays that will be used not to have duplicate tags.
   *
   * @param results assumed not <code>null</code>.
   * @return List with the list of quantity of pages
   */
  private List<ArrayList<String>> inicializeArray(List<IPSMetadataEntry> results) {
    List<ArrayList<String>> arrayPages = new ArrayList<>();

    for (int j = 0; j < results.size(); j++) {
      ArrayList<String> array = new ArrayList<>();
      arrayPages.add(array);
    }

    return arrayPages;
  }
}

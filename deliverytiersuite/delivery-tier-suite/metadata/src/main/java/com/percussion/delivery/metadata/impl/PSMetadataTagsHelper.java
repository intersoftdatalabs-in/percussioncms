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
package com.percussion.delivery.metadata.impl;

import java.util.*;
import javax.servlet.ServletException;
import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.impl.utils.PSPair;

/**
 * Processes tags list metadata and returns a list of tag/count pairs.
 * @author davidpardini
 */
public class PSMetadataTagsHelper {

    public static final String REFERENCES = "perc:tags";
    public static final String TAG_NAME = "tagName";
    public static final String TAG_COUNT = "tagCount";
    public static final String PROPERTIES = "properties";
    public static final String COUNT_SORT = "count";

    /**
     * Returns the list of tags and their occurrences.
     * Iterates by page and then by property.
     * @param results assumed not <code>null</code>.
     * @param sortOrder sort order ("count" for count descending, otherwise alpha).
     * @return List<PSPair<String, Integer>>
     * @throws ServletException
     */
    public List<PSPair<String, Integer>> processTags(List<IPSMetadataEntry> results, String sortOrder)
            throws ServletException {
        var arrayPages = initializeArray(results);
        var tagsMap = new HashMap<String, Integer>();
        try {
            for (int i = 0; i < results.size(); i++) {
                var entryPage = results.get(i);
                for (var prop : entryPage.getProperties()) {
                    if (REFERENCES.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
                        countTags(tagsMap, prop.getStringvalue().trim(), arrayPages.get(i));
                    }
                }
            }
            var tagResultList = new ArrayList<PSPair<String, Integer>>();
            tagsMap.forEach((key, value) -> tagResultList.add(new PSPair<>(key, value)));
            Comparator<PSPair<String, Integer>> comp = Comparator.comparing(PSPair::getFirst);
            if (COUNT_SORT.equalsIgnoreCase(sortOrder)) {
                comp = Comparator.<PSPair<String, Integer>>comparingInt(PSPair::getSecond)
                        .reversed()
                        .thenComparing(PSPair::getFirst);
            }
            tagResultList.sort(comp);
            return tagResultList;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Counts tags and updates the map.
     */
    private void countTags(Map<String, Integer> tagsMap, String stringvalue, ArrayList<String> arrayList) {
        var tag = stringvalue.trim().toLowerCase();
        if (tagsMap.containsKey(tag)) {
            if (!arrayList.contains(tag)) {
                tagsMap.put(tag, tagsMap.get(tag) + 1);
            }
        } else {
            tagsMap.put(tag, 1);
            arrayList.add(tag);
        }
    }

    /**
     * Initializes the array used for unduplicated tags.
     */
    private List<ArrayList<String>> initializeArray(List<IPSMetadataEntry> results) {
        var arrayPages = new ArrayList<ArrayList<String>>();
        for (int j = 0; j < results.size(); j++) {
            arrayPages.add(new ArrayList<>());
        }
        return arrayPages;
    }
}

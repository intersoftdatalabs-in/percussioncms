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

// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.utils;

import java.util.HashSet;
import java.util.Set;

/** Utility for filtering out bad keywords from link text. */
public class PSLinkBadKeywords {
  private static final Set<String> FILTER_SET =
      new HashSet<>() {
        {
          add("more");
          add("this");
          add("that");
          add("click here");
          add("there");
          add("here");
          add("over there");
        }
      };

  /** Checks if the given string is in the filter list. */
  public static boolean isStringInFilterList(final String stringToFind) {
    return FILTER_SET.contains(stringToFind.toLowerCase());
  }

  /** Filters out common bad keywords from link text. */
  public static String filterLinkTextString(String stringForFilter) {
    var returnString =
        stringForFilter
            .replace("Link to ", "")
            .replace("link to ", "")
            .replace("Browse to ", "")
            .replace("browse to ", "")
            .replace("Navigate to ", "")
            .replace("navigate to ", "")
            .replace("Click here for ", "")
            .replace("click here for ", "");
    return returnString;
  }
}

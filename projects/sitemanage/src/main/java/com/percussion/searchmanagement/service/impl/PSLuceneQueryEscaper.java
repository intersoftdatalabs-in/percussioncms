/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.searchmanagement.service.impl;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Package-visible Lucene free-text query escaping used by {@link PSSearchService}. Extracted so
 * unit tests can exercise mid-query slash escaping without Spring-wiring {@code PSSearchService}.
 */
final class PSLuceneQueryEscaper {

  private static final List<String> LUCENE_SPECIAL_CHARACTERS =
      Arrays.asList(
          "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "'", "~", "*", "?", ":");

  private PSLuceneQueryEscaper() {}

  /**
   * Escapes Lucene special characters for free-text queries, including mid-query {@code /} (paths).
   *
   * @param query raw query text; blank values returned unchanged
   * @return escaped query safe for classic QueryParser
   */
  static String escape(String query) {
    if (StringUtils.isBlank(query)) {
      return query;
    }
    var escapedQuery = query;
    for (var specialCharacter : LUCENE_SPECIAL_CHARACTERS) {
      if (escapedQuery.startsWith(specialCharacter)) {
        var replacement = "\\" + specialCharacter;
        escapedQuery = replacement + escapedQuery.substring(1);
        break;
      }
    }
    escapedQuery = escapedQuery.replaceAll("(?<!\\\\)/", "\\\\/");
    return escapedQuery;
  }
}

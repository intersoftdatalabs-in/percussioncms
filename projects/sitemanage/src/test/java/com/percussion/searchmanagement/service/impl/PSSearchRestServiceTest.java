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
package com.percussion.searchmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.searchmanagement.data.PSSearchCriteria;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-878 / v8.1.7 PRs #889 and #914: path-based search terms with slashes and dashes
 * must be Lucene-escaped so classic QueryParser does not throw ParseException.
 */
class PSSearchRestServiceTest {

  @Test
  void sanitizeCriteriaEscapesSlashesAndDashes() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("people/donna-williams");
    restService.sanitizeCriteria(criteria);
    assertNotNull(criteria.getQuery());
    // Lucene classic QueryParser.escape escapes / and -
    assertEquals("people\\/donna\\-williams", criteria.getQuery());
  }

  /**
   * getSearchFields() returns an unmodifiable view; sanitize must not call replaceAll on that view
   * (UnsupportedOperationException → Home Search 500).
   */
  @Test
  void sanitizeCriteriaDoesNotMutateUnmodifiableSearchFields() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("hello");
    criteria.setSearchFields(java.util.Map.of("sys_title", "<b>x</b>"));
    restService.sanitizeCriteria(criteria);
    assertEquals("hello", criteria.getQuery()); // also escaped if special chars
    assertEquals("&lt;b&gt;x&lt;/b&gt;", criteria.getSearchFields().get("sys_title"));
  }

  @Test
  void sanitizeCriteriaHandlesNullAndEmptySearchFields() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("q");
    restService.sanitizeCriteria(criteria); // null fields
    criteria.setSearchFields(java.util.Map.of());
    restService.sanitizeCriteria(criteria); // empty fields
    assertEquals("q", criteria.getQuery());
  }

  /**
   * GH-2950: Explorer free-text search posts minimal criteria (query + paging). Server must accept
   * that shape by defaulting {@code formatId} / {@code startIndex} so {@code searchForIds} does not
   * throw IllegalArgumentException → HTTP 400.
   */
  @Test
  void sanitizeCriteriaDefaultsFormatIdAndStartIndexForMinimalQuery() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("a");
    criteria.setMaxResults(25);
    restService.sanitizeCriteria(criteria);
    assertEquals(Integer.valueOf(PSSearchRestService.DEFAULT_SEARCH_FORMAT_ID), criteria.getFormatId());
    assertEquals(Integer.valueOf(1), criteria.getStartIndex());
    assertEquals("a", criteria.getQuery());
  }

  @Test
  void sanitizeCriteriaPreservesExplicitFormatIdAndStartIndex() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("hello");
    criteria.setFormatId(12);
    criteria.setStartIndex(10);
    restService.sanitizeCriteria(criteria);
    assertEquals(Integer.valueOf(12), criteria.getFormatId());
    assertEquals(Integer.valueOf(10), criteria.getStartIndex());
  }

  @Test
  void sanitizeCriteriaClearsRootFolderPathAsUnscoped() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("a");
    criteria.setFolderPath("//");
    restService.sanitizeCriteria(criteria);
    assertNull(criteria.getFolderPath());
    criteria.setFolderPath("/");
    restService.sanitizeCriteria(criteria);
    assertNull(criteria.getFolderPath());
    criteria.setFolderPath("//Sites");
    restService.sanitizeCriteria(criteria);
    assertEquals("//Sites", criteria.getFolderPath());
  }

  @Test
  void sanitizeCriteriaCoercesNonPositiveStartIndexToOne() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);
    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("x");
    criteria.setStartIndex(0);
    restService.sanitizeCriteria(criteria);
    assertEquals(Integer.valueOf(1), criteria.getStartIndex());
    criteria.setStartIndex(-3);
    restService.sanitizeCriteria(criteria);
    assertEquals(Integer.valueOf(1), criteria.getStartIndex());
  }

  /**
   * Exercises {@link PSLuceneQueryEscaper#escape(String)} mid-query slash escaping used on the
   * urlDecodedQuery search path in {@code PSSearchService} (GH-878 / #889 / #914).
   */
  @Test
  void escapeLuceneQueryEscapesMidQuerySlash() {
    assertEquals("a\\/b", PSLuceneQueryEscaper.escape("a/b"));
    assertEquals("people\\/donna-williams", PSLuceneQueryEscaper.escape("people/donna-williams"));
  }

  @Test
  void escapeLuceneQueryLeavesAlreadyEscapedSlashUntouched() {
    assertEquals("a\\/b", PSLuceneQueryEscaper.escape("a\\/b"));
    assertEquals("x\\/y\\/z", PSLuceneQueryEscaper.escape("x\\/y\\/z"));
  }

  @Test
  void escapeLuceneQueryHandlesBlankAndLeadingSpecial() {
    assertNull(PSLuceneQueryEscaper.escape(null));
    assertEquals("", PSLuceneQueryEscaper.escape(""));
    assertEquals("   ", PSLuceneQueryEscaper.escape("   "));
    // leading special char is escaped once, then mid-query slashes
    assertEquals("\\-a\\/b", PSLuceneQueryEscaper.escape("-a/b"));
  }
}

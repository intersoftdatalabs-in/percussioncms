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
package com.percussion.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSContentType;
import com.percussion.cms.objectstore.PSKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSSearchQuery} surfaces (#2998 / epic #2022 residual of
 * #2873). Uses a stub implementation so contract behavior is verified without a live Lucene index.
 */
public class PSSearchQueryTypedTest {

  @Test
  public void conveniencePerformSearchDelegatesWithNullControlProps() throws PSSearchException {
    RecordingSearchQuery query = new RecordingSearchQuery();
    Collection<PSKey> ctypes = Collections.singletonList(PSContentType.createKey(42));
    Map<String, String> fieldQueries = new HashMap<>();
    fieldQueries.put("sys_title", "alpha");

    List<PSSearchResult> results = query.performSearch(ctypes, "global", fieldQueries);

    assertTrue(results.isEmpty());
    assertSame(ctypes, query.lastCtypeIds);
    assertEquals("global", query.lastGlobalQuery);
    assertSame(fieldQueries, query.lastFieldQueries);
    assertEquals(null, query.lastControlProps);
  }

  @Test
  public void performSearchAcceptsTypedMapsAndListReturn() throws PSSearchException {
    RecordingSearchQuery query = new RecordingSearchQuery();
    query.resultsToReturn.add(
        new PSSearchResult(new com.percussion.design.objectstore.PSLocator("7"), 90));

    Map<String, String> props = new HashMap<>();
    props.put(PSSearchQuery.QUERYPROP_MAXRESULTS, "10");
    props.put(PSSearchQuery.QUERYPROP_LANGUAGE, "en-us");

    List<PSSearchResult> results =
        query.performSearch(new ArrayList<>(), null, Collections.emptyMap(), props);

    assertEquals(1, results.size());
    assertEquals(7, results.get(0).getKey().getId());
    assertEquals(90, results.get(0).getRelevancy());
    assertSame(props, query.lastControlProps);
  }

  /** Minimal stub that records last args and returns a configurable result list. */
  private static final class RecordingSearchQuery extends PSSearchQuery {
    Collection<? extends PSKey> lastCtypeIds;
    String lastGlobalQuery;
    Map<String, String> lastFieldQueries;
    Map<String, String> lastControlProps;
    final List<PSSearchResult> resultsToReturn = new ArrayList<>();

    @Override
    public List<PSSearchResult> performSearch(
        Collection<? extends PSKey> ctypeIds,
        String globalQuery,
        Map<String, String> fieldQueries,
        Map<String, String> controlProps) {
      lastCtypeIds = ctypeIds;
      lastGlobalQuery = globalQuery;
      lastFieldQueries = fieldQueries;
      lastControlProps = controlProps;
      return resultsToReturn;
    }
  }
}

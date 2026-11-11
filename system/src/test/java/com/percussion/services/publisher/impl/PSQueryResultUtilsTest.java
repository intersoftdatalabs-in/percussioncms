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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.services.publisher.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 987 T034: the JCR 1.0 → 2.0 migration added the {@code getSelectorNames()}
 * stub to the split-result wrapper in {@link PSQueryResultUtils}. Pin the
 * 2.0 API surface and the split behavior it rides on.
 */
@Tag("UnitTest")
class PSQueryResultUtilsTest {

  private QueryResult singleRowResult(String... columns) throws Exception {
    QueryResult qr = mock(QueryResult.class);
    when(qr.getColumnNames()).thenReturn(columns);
    Row row = mock(Row.class);
    RowIterator rows = mock(RowIterator.class);
    when(rows.hasNext()).thenReturn(true, false);
    when(rows.nextRow()).thenReturn(row);
    when(qr.getRows()).thenReturn(rows);
    return qr;
  }

  @Test
  void splitResultsExposeEmptySelectorNames() throws Exception {
    Iterator<QueryResult> split =
        PSQueryResultUtils.splitQueryResults(singleRowResult("c1"), 1);

    QueryResult chunk = split.next();
    assertEquals(List.of("c1"), List.of(chunk.getColumnNames()));
    assertEquals(0, chunk.getSelectorNames().length);
  }
}

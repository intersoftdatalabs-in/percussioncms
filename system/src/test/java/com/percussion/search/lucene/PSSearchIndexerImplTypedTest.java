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
package com.percussion.search.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSContentType;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.search.PSSearchException;
import com.percussion.search.PSSearchKey;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed Lucene indexer surfaces (#2998 / epic #2022 residual of #2873).
 */
public class PSSearchIndexerImplTypedTest {

  @Test
  public void stringFieldValuesKeepsOnlyStringEntries() {
    Map<String, Object> fragment = new HashMap<>();
    fragment.put("sys_title", "Hello");
    fragment.put("sys_lang", "en-us");
    fragment.put("body", new byte[] {1, 2, 3});
    fragment.put("stream", new ByteArrayInputStream(new byte[] {9}));
    fragment.put("missing", null);

    Map<String, String> strings = PSSearchIndexerImpl.stringFieldValues(fragment);

    assertEquals(2, strings.size());
    assertEquals("Hello", strings.get("sys_title"));
    assertEquals("en-us", strings.get("sys_lang"));
    assertFalse(strings.containsKey("body"));
    assertFalse(strings.containsKey("stream"));
    assertFalse(strings.containsKey("missing"));
  }

  @Test
  public void stringFieldValuesEmptyWhenNoStrings() {
    Map<String, Object> fragment = new HashMap<>();
    fragment.put("bin", new byte[0]);
    assertTrue(PSSearchIndexerImpl.stringFieldValues(fragment).isEmpty());
  }

  @Test
  public void updateRejectsNullUnitId() {
    PSSearchIndexerImpl indexer = new PSSearchIndexerImpl();
    Map<String, Object> fragment = new HashMap<>();
    fragment.put("sys_title", "x");
    assertThrows(
        IllegalArgumentException.class, () -> indexer.update(null, fragment, false));
  }

  @Test
  public void updateRejectsNullFragment() {
    PSSearchIndexerImpl indexer = new PSSearchIndexerImpl();
    PSSearchKey unitId =
        new PSSearchKey(PSContentType.createKey(1), new PSLocator(10, 1), null);
    assertThrows(IllegalArgumentException.class, () -> indexer.update(unitId, null, false));
  }

  @Test
  public void deleteRejectsNullCollection() {
    PSSearchIndexerImpl indexer = new PSSearchIndexerImpl();
    assertThrows(IllegalArgumentException.class, () -> indexer.delete(null));
  }

  @Test
  public void deleteSkipsNullEntriesWithoutIndexing() throws PSSearchException {
    PSSearchIndexerImpl indexer = new PSSearchIndexerImpl();
    List<PSSearchKey> unitIds = new ArrayList<>();
    unitIds.add(null);
    // No non-null keys → no index writers touched; must not throw.
    indexer.delete(unitIds);
  }
}

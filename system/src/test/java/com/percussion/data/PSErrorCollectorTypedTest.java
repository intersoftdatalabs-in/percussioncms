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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed {@link PSErrorCollector} maps and item error lists. */
@Tag("UnitTest")
class PSErrorCollectorTypedTest {

  @Test
  void fieldAndItemErrorsTrackCountsAndMaxExceeded() {
    PSErrorCollector collector = new PSErrorCollector(PSErrorCollector.TYPE_FIELD, 2);
    assertFalse(collector.hasErrors());
    assertFalse(collector.maxErrorsExceeded());

    collector.add(Integer.valueOf(0), "http://example/error?page=0");
    assertTrue(collector.hasErrors());
    assertEquals(1, collector.getErrorCount());
    assertFalse(collector.maxErrorsExceeded());

    List<String> submit = Arrays.asList("title");
    List<String> display = Arrays.asList("Title");
    collector.add(Integer.valueOf(0), submit, display, "bad value {0}", Arrays.asList("x"));
    assertEquals(2, collector.getErrorCount());
    assertTrue(collector.maxErrorsExceeded());
  }

  @Test
  void genericMessageSetAndItemDocumentsIncrementCount() {
    PSErrorCollector collector = new PSErrorCollector(PSErrorCollector.TYPE_ITEM, 10);
    collector.set("generic failure");
    org.w3c.dom.Document empty =
        com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    collector.add(empty);
    assertTrue(collector.hasErrors());
    assertEquals(1, collector.getErrorCount());
  }
}

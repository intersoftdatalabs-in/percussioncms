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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.server.PSRequest;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

    // Assert MessageFormat result and stored submit/display names via public document API.
    PSRequest request = new PSRequest(null, null, null, null);
    Document errorDoc = collector.getErrorDocument(request);
    assertNotNull(errorDoc);
    NodeList fields = errorDoc.getElementsByTagName(PSErrorCollector.ERROR_FIELD_ELEM);
    assertEquals(1, fields.getLength());
    Element field = (Element) fields.item(0);
    assertEquals("title", field.getAttribute(PSErrorCollector.SUBMIT_NAME_ATTR));
    assertEquals("Title", field.getAttribute(PSErrorCollector.DISPLAY_NAME_ATTR));
    NodeList messages = errorDoc.getElementsByTagName(PSErrorCollector.ERROR_MESSAGE_ELEM);
    assertEquals(1, messages.getLength());
    String messageText = messages.item(0).getTextContent();
    assertTrue(
        messageText.startsWith("bad value x"),
        "expected formatted message prefix, got: " + messageText);
  }

  @Test
  void genericMessageSetAndItemDocumentsIncrementCount() throws Exception {
    PSErrorCollector collector = new PSErrorCollector(PSErrorCollector.TYPE_ITEM, 10);
    collector.set("generic failure");
    // set() does not increment error count; assert stored generic message is retained.
    Field generic = PSErrorCollector.class.getDeclaredField("m_genericError");
    generic.setAccessible(true);
    assertEquals("generic failure", generic.get(collector));

    org.w3c.dom.Document empty = com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    collector.add(empty);
    assertTrue(collector.hasErrors());
    assertEquals(1, collector.getErrorCount());
  }
}

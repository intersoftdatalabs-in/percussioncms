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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.server.PSRelationshipDbProcessor;
import com.percussion.cms.objectstore.server.util.PSFieldFinderUtilTest;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for residual cms.objectstore this-escape real fixes (#2652 / parent #2022):
 * CoreItem definition extract without this-escape, leaf value/processor construction, multi-value
 * Element restore. Complements {@link PSObjectStoreThisEscapeResidualTest} (#2613).
 */
public class PSObjectStoreThisEscapeCoreItemTest {

  @Test
  public void coreItemExtractsFieldsAndChildrenFromDefinition() throws Exception {
    PSItemDefinition def = PSFieldFinderUtilTest.loadItemDefinition("PSFieldFinderUtilTest1.xml");
    assertNotNull(def);
    assertEquals("Press Release", def.getName());

    PSCoreItem item = new PSCoreItem(def);
    assertNotNull(item.getItemDefinition());
    assertEquals(def, item.getItemDefinition());

    // Private base-load extract must populate parent fields from the content editor definition.
    Iterator<String> fieldNames = item.getAllFieldNames();
    assertTrue(fieldNames.hasNext(), "expected at least one extracted field");

    int fieldCount = 0;
    while (fieldNames.hasNext()) {
      String name = fieldNames.next();
      assertNotNull(item.getFieldByName(name));
      fieldCount++;
    }
    assertTrue(fieldCount > 0);

    // Re-apply via public populate path after construction still works.
    PSItemDefExtractor.populateItemDefinition(item);
    assertTrue(item.getAllFieldNames().hasNext());
  }

  @Test
  public void itemDefinitionElementRoundTrip() throws Exception {
    PSItemDefinition original =
        PSFieldFinderUtilTest.loadItemDefinition("PSFieldFinderUtilTest1.xml");
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);

    PSItemDefinition restored = new PSItemDefinition(xml);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getTypeId(), restored.getTypeId());
    assertEquals(original.getAppName(), restored.getAppName());
  }

  @Test
  public void textDateXmlBinaryValueCtors() throws Exception {
    PSTextValue text = new PSTextValue("hello");
    assertEquals("hello", text.getValueAsString());
    PSTextValue empty = new PSTextValue(null);
    assertEquals("", empty.getValueAsString());

    Date now = new Date(1_700_000_000_000L);
    PSDateValue dateVal = new PSDateValue(now);
    assertNotNull(dateVal.getValue());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement("payload");
    el.appendChild(doc.createTextNode("x"));
    PSXmlValue xmlVal = new PSXmlValue(el);
    assertEquals("payload", ((Element) xmlVal.getValue()).getNodeName());

    byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
    PSBinaryValue bin = new PSBinaryValue(bytes);
    assertArrayEquals(bytes, (byte[]) bin.getValue());
    assertTrue(bin.isDataLoaded());

    PSBinaryValue fromStream =
        new PSBinaryValue(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)));
    assertArrayEquals("xyz".getBytes(StandardCharsets.UTF_8), (byte[]) fromStream.getValue());
  }

  @Test
  public void searchMultiPropertyElementRoundTrip() throws Exception {
    PSSearchMultiProperty original = new PSSearchMultiProperty("sys_community");
    original.add("10");
    original.add("20");
    assertTrue(original.contains("10"));
    assertTrue(original.contains("20"));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);
    PSSearchMultiProperty restored = new PSSearchMultiProperty(xml);
    assertEquals("sys_community", restored.getName());
    assertTrue(restored.contains("10"));
    assertTrue(restored.contains("20"));

    // Public fromXml after construction still works
    restored.fromXml(xml);
    assertTrue(restored.contains("10"));
  }

  @Test
  public void relationshipDbProcessorCtorsAcceptNullAndThreadContext() throws Exception {
    PSRelationshipDbProcessor threadProc = new PSRelationshipDbProcessor(true);
    assertNotNull(threadProc);

    // null request → internal rhythmyx user path; must not throw
    PSRelationshipDbProcessor nullReq = new PSRelationshipDbProcessor((com.percussion.server.PSRequest) null);
    assertNotNull(nullReq);
    nullReq.setRequest(null);
  }

  @Test
  public void processingStatisticsNamedCtor() {
    PSProcessingStatistics stats = new PSProcessingStatistics(1, 2, 3, 4, 5);
    assertEquals(1, stats.getInsertedCount());
    assertEquals(2, stats.getUpdatedCount());
    assertEquals(3, stats.getDeletedCount());
    assertEquals(4, stats.getSkippedCount());
    assertEquals(5, stats.getErroredCount());
    assertFalse(stats.getInsertedCount() < 0);
  }
}

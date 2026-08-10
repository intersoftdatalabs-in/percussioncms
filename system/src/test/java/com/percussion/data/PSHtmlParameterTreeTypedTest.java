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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.server.PSRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Behavioral tests for typed {@link PSHtmlParameterTree} parameter maps after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSHtmlParameterTreeTypedTest {

  @Test
  void generateTreeEmitsScalarAndListParameters() {
    PSRequest request = new PSRequest(null, null, null, null);
    request.setParameter("title", "Hello");
    List<String> multi = new ArrayList<>();
    multi.add("a");
    multi.add("b");
    request.setParameter("tags", multi);

    Document doc = PSHtmlParameterTree.generateHtmlParameterTree(request);
    assertNotNull(doc);
    Element root = doc.getDocumentElement();
    assertEquals("PSXParams", root.getNodeName());

    NodeList paramGroups = root.getElementsByTagName("PSXParam");
    assertEquals(2, paramGroups.getLength()); // two levels for multi-value tags

    Element first = (Element) paramGroups.item(0);
    assertEquals("Hello", textOf(first, "title"));
    assertEquals("a", textOf(first, "tags"));

    Element second = (Element) paramGroups.item(1);
    assertEquals("b", textOf(second, "tags"));
  }

  private static String textOf(Element parent, String childName) {
    NodeList kids = parent.getElementsByTagName(childName);
    if (kids.getLength() == 0) return null;
    return kids.item(0).getTextContent();
  }
}

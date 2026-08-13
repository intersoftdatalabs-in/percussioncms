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
package com.percussion.server.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.design.objectstore.PSTraceOption;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Typed option iteration helper for {@link PSConsoleCommandTrace} (#3272). */
@Tag("UnitTest")
@DisplayName("PSConsoleCommandTrace typed options")
class PSConsoleCommandTraceTypedTest {

  @Test
  @DisplayName("addTraceOptionElements writes flag and name attributes in order")
  void addTraceOptionElementsWritesAttributes() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXConsoleCommandResults");

    List<PSTraceOption> options = new ArrayList<>();
    options.add(new PSTraceOption(0x10, "Basic Request", "desc", "basicRequest"));
    options.add(new PSTraceOption(0x20, "App Handler", null, "appHandler"));

    PSConsoleCommandTrace.addTraceOptionElements(doc, root, options);

    NodeList nodes = root.getElementsByTagName("PSXTraceOption");
    assertEquals(2, nodes.getLength());

    Element first = (Element) nodes.item(0);
    assertEquals("0x10", first.getAttribute("flag"));
    assertEquals("basicRequest", first.getAttribute("name"));

    Element second = (Element) nodes.item(1);
    assertEquals("0x20", second.getAttribute("flag"));
    assertEquals("appHandler", second.getAttribute("name"));
  }

  @Test
  @DisplayName("help constructor is accepted without extra args")
  void helpConstructorAccepted() throws Exception {
    PSConsoleCommandTrace cmd = new PSConsoleCommandTrace("help");
    assertNotNull(cmd);
  }
}

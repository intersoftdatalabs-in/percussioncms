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
package com.percussion.webui.filter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guards the {@code PSWebUiSpaFallbackFilter} servlet mappings in {@code WEB-INF/web.xml}.
 *
 * <p>#6578: pretty SPA routes ({@code /cm/app/<entry>/<section>}) 404'd on direct load in
 * root-context cells because the SPA mappings only matched the {@code REQUEST} dispatch while
 * {@code PSDispatcherFilter} strips the {@code /Rhythmyx} prefix via an internal {@code FORWARD}.
 * Both SPA mappings must match {@code REQUEST} and {@code FORWARD}.
 */
public class PSWebUiSpaFallbackWebXmlTest {

  private static final String FILTER_NAME = "PSWebUiSpaFallbackFilter";

  @Test
  public void spaMappingsCoverRequestAndForwardDispatch() throws Exception {
    Path webXml =
        Paths.get(
            System.getProperty("user.dir"), "src", "main", "webapp", "WEB-INF", "web.xml");
    if (!Files.isRegularFile(webXml)) {
      fail("web.xml not found at " + webXml);
    }
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    Element root =
        factory.newDocumentBuilder().parse(webXml.toFile()).getDocumentElement();

    Map<String, Set<String>> dispatchersByPattern = new HashMap<>();
    NodeList mappings = root.getElementsByTagName("filter-mapping");
    for (int i = 0; i < mappings.getLength(); i++) {
      Element mapping = (Element) mappings.item(i);
      if (!FILTER_NAME.equals(textOf(mapping, "filter-name"))) {
        continue;
      }
      String pattern = textOf(mapping, "url-pattern");
      NodeList dispatchers = mapping.getElementsByTagName("dispatcher");
      Set<String> dispatchersForPattern =
          dispatchersByPattern.computeIfAbsent(pattern, key -> new HashSet<>());
      if (dispatchers.getLength() == 0) {
        // Servlet default when no <dispatcher> is declared.
        dispatchersForPattern.add("REQUEST");
      }
      for (int j = 0; j < dispatchers.getLength(); j++) {
        dispatchersForPattern.add(dispatchers.item(j).getTextContent().trim());
      }
    }

    for (String pattern : new String[] {"/cm/app/*", "/cm/pages/app/*"}) {
      Set<String> dispatchers = dispatchersByPattern.get(pattern);
      assertTrue(
          FILTER_NAME + " must map " + pattern + " (found mappings: " + dispatchersByPattern + ")",
          dispatchers != null);
      assertTrue(
          FILTER_NAME + " mapping " + pattern + " must match REQUEST (found: " + dispatchers + ")",
          dispatchers.contains("REQUEST"));
      assertTrue(
          FILTER_NAME + " mapping " + pattern + " must match FORWARD for #6578 (found: "
              + dispatchers + ")",
          dispatchers.contains("FORWARD"));
    }
  }

  private static String textOf(Element parent, String tag) {
    NodeList nodes = parent.getElementsByTagName(tag);
    if (nodes.getLength() == 0) {
      return null;
    }
    return nodes.item(0).getTextContent().trim();
  }
}

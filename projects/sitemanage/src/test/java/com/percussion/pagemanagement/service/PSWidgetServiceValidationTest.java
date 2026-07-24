/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Validates the WidgetRegistry.xml contents. Ensures removed widgets (e.g. Evergage Beacon per
 * GH#709, Share This per GH#690) are no longer listed in any group. This test is activated as part
 * of the widget removal work (previously placeholder during migration). Updated for final package
 * dir removal in perc-packages (perc.evergageBeacon).
 */
public class PSWidgetServiceValidationTest {

  @Test
  public void testWidgetRegistryExcludesRemovedWidgets() throws Exception {
    // Load the same registry resource used by PSWidgetService.loadWidgetTypeMap()
    try (InputStream in =
        this.getClass()
            .getClassLoader()
            .getResourceAsStream("com/percussion/pagemanagement/service/impl/WidgetRegistry.xml")) {
      if (in == null) {
        fail("WidgetRegistry.xml not found on test classpath");
      }
      Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
      NodeList groupElems = doc.getElementsByTagName("group");
      Set<String> allWidgetNames = new HashSet<>();
      Set<String> communityWidgets = new HashSet<>();
      Set<String> deprecatedWidgets = new HashSet<>();

      for (int i = 0; i < groupElems.getLength(); i++) {
        Element groupElem = (Element) groupElems.item(i);
        String groupName = groupElem.getAttribute("name");
        NodeList widgetElems = groupElem.getElementsByTagName("widget");
        for (int j = 0; j < widgetElems.getLength(); j++) {
          Element widgetElem = (Element) widgetElems.item(j);
          String wdgName = widgetElem.getAttribute("name");
          allWidgetNames.add(wdgName);
          if ("Community".equals(groupName)) {
            communityWidgets.add(wdgName);
          }
          if ("Deprecated".equals(groupName)) {
            deprecatedWidgets.add(wdgName);
          }
        }
      }

      // Evergage Beacon removed completely (GH#709) - should not be present in any group
      assertFalse(
          allWidgetNames.contains("Evergage Beacon"),
          "Evergage Beacon should have been removed from WidgetRegistry.xml");

      // Some expected widgets still present to ensure file is valid and not accidentally truncated
      assertTrue(allWidgetNames.contains("EMS Event List"), "Community widget EMS Event List should remain");
      assertTrue(allWidgetNames.contains("Archives"), "Percussion group widget Archives should remain");
      // Flash removed completely - should not be present in any group
      assertFalse(
          allWidgetNames.contains("Flash"),
          "Flash should have been removed from WidgetRegistry.xml");

      // Share This removed completely (GH#690) - retired sharethis.com widget
      assertFalse(
          allWidgetNames.contains("Share This"),
          "Share This should have been removed from WidgetRegistry.xml");
      assertFalse(
          deprecatedWidgets.contains("Share This"),
          "Share This must not remain in the Deprecated group");

      // Community group should no longer contain Evergage
      assertFalse(
          communityWidgets.contains("Evergage Beacon"),
          "Evergage Beacon must not be in Community group");
    }
  }
}

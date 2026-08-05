/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.webui.gadget.servlets;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Loads {@code GadgetRegistry.xml} from the classpath. Restored for v8.1.7 PR #722 / #885 grouping
 * (Percussion vs Deprecated). The registry was removed with Shindig cleanup but remains the
 * canonical grouping for dashboard gadget types.
 */
public final class GadgetRegistry {

  private static final Logger log = LogManager.getLogger(GadgetRegistry.class);

  /** Classpath location of the registry resource. */
  public static final String REGISTRY_RESOURCE =
      "com/percussion/webui/gadget/servlets/GadgetRegistry.xml";

  private GadgetRegistry() {}

  /**
   * Loads gadget name → group name map from the classpath registry.
   *
   * @return unmodifiable map; empty if the resource is missing or unreadable
   */
  public static Map<String, String> loadGadgetTypeMap() {
    Map<String, String> gadTypeMap = new LinkedHashMap<>();
    try (InputStream in =
        GadgetRegistry.class.getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
      if (in == null) {
        log.error("Gadget registry file is missing from classpath: {}", REGISTRY_RESOURCE);
        return Collections.emptyMap();
      }

      Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
      NodeList groupElems = doc.getElementsByTagName("group");
      for (int i = 0; i < groupElems.getLength(); i++) {
        Element groupElem = (Element) groupElems.item(i);
        String groupName = groupElem.getAttribute("name");
        NodeList gadgetElems = groupElem.getElementsByTagName("gadget");
        for (int j = 0; j < gadgetElems.getLength(); j++) {
          Element gadgetElem = (Element) gadgetElems.item(j);
          String gdgName = gadgetElem.getAttribute("name");
          if (gdgName != null && !gdgName.isBlank()) {
            gadTypeMap.put(gdgName, groupName);
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to load {}: {}", REGISTRY_RESOURCE, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(gadTypeMap);
  }

  /**
   * Maps a gadget display name to its logical group.
   *
   * @param gadgetName display name from registry (e.g. "Activity")
   * @return group name, or {@code "Custom"} if unknown
   */
  public static String getGadgetType(String gadgetName) {
    if (gadgetName == null || gadgetName.isBlank()) {
      return "Custom";
    }
    String type = loadGadgetTypeMap().get(gadgetName);
    return type != null ? type : "Custom";
  }
}

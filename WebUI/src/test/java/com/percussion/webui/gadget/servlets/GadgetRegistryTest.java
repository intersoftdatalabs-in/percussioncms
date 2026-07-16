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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;

/**
 * Regression for v8.1.7 PR #722 / #885: GadgetRegistry.xml on classpath with Percussion +
 * Deprecated groups.
 */
public class GadgetRegistryTest {

  @Test
  public void registryLoadsWithPercussionAndDeprecatedGroups() {
    Map<String, String> map = GadgetRegistry.loadGadgetTypeMap();
    assertFalse("GadgetRegistry.xml must be on the classpath", map.isEmpty());

    assertEquals("Deprecated", map.get("Activity"));
    assertEquals("Deprecated", map.get("Siteimprove"));
    assertEquals("Deprecated", map.get("Membership"));
    assertEquals("Deprecated", map.get("Redirect Management"));
    assertEquals("Deprecated", map.get("Widget Configuration"));

    // #885 undeprecated these back to Percussion
    assertEquals("Percussion", map.get("Google Setup"));
    assertEquals("Percussion", map.get("Traffic"));
    assertEquals("Percussion", map.get("What's Working"));

    assertEquals("Percussion", map.get("Welcome"));
    assertEquals("Percussion", map.get("Bulk Upload"));
  }

  @Test
  public void unknownGadgetIsCustom() {
    assertEquals("Custom", GadgetRegistry.getGadgetType("Not A Real Gadget"));
  }

  @Test
  public void deprecatedTypeLookup() {
    assertTrue("Deprecated".equals(GadgetRegistry.getGadgetType("Activity")));
  }
}

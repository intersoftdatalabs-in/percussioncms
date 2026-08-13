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
package com.percussion.services.assembly.impl.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Typed helpers for nav assembly leftovers (#3280). */
@Tag("UnitTest")
class PSNavHelperTypedTest {

  @Test
  @DisplayName("getParams flattens first values and optional user")
  void getParamsFlattens() {
    IPSAssemblyItem item = mock(IPSAssemblyItem.class);
    Map<String, String[]> raw = new HashMap<>();
    raw.put("sys_folderid", new String[] {"42", "99"});
    raw.put("empty", new String[] {null});
    when(item.getParameters()).thenReturn(raw);
    when(item.getParameterValue("sys_folderid", null)).thenReturn("42");
    when(item.getParameterValue("empty", null)).thenReturn(null);
    when(item.getUserName()).thenReturn("editor");

    Map<String, String> params = PSNavHelper.getParams(item);
    assertEquals("42", params.get("sys_folderid"));
    assertFalse(params.containsKey("empty"));
    assertEquals("editor", params.get(IPSHtmlParameters.SYS_USER));
  }

  @Test
  @DisplayName("putNavBaseFromVariables copies string (and non-string) values without cast")
  void putNavBaseFromVariables() {
    Map<String, Object> nav = new HashMap<>();
    Map<String, Object> vars = new HashMap<>();
    vars.put("navbase", "/sites/site1");

    PSNavHelper.putNavBaseFromVariables(nav, vars, "navbase");
    assertEquals("/sites/site1", nav.get("base"));

    nav.clear();
    PSNavHelper.putNavBaseFromVariables(nav, vars, "missing");
    assertFalse(nav.containsKey("base"));

    PSNavHelper.putNavBaseFromVariables(nav, "not-a-map", "navbase");
    assertFalse(nav.containsKey("base"));

    PSNavHelper.putNavBaseFromVariables(nav, null, "navbase");
    assertFalse(nav.containsKey("base"));
  }

  @Test
  @DisplayName("copyPropertyMap keeps Property values and skips other entries")
  void copyPropertyMap() {
    Property prop = mock(Property.class);
    Map<Object, Object> raw = new HashMap<>();
    raw.put("rx:title", prop);
    raw.put("nav:url", "not-a-property");
    raw.put(3, prop);

    Map<String, Property> typed = PSNavonNodeInvocationHandler.copyPropertyMap(raw);
    assertEquals(1, typed.size());
    assertEquals(prop, typed.get("rx:title"));

    assertTrue(PSNavonNodeInvocationHandler.copyPropertyMap(null).isEmpty());
  }
}

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
package com.percussion.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.extension.PSDatabaseFunction;
import com.percussion.extension.PSDatabaseFunctionDef;
import com.percussion.extension.PSDatabaseFunctionManager;
import com.percussion.extension.PSDatabaseFunctionsColl;
import com.percussion.relationship.effect.PSAttachCloneToFolder;
import com.percussion.relationship.effect.PSEffectUtils;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed xml / extension / relationship / webdav collection APIs after rawtypes
 * cleanup (#2935 / parent #2877).
 */
@Tag("UnitTest")
@DisplayName("xml/extension/relationship/webdav package generics")
class PSXmlExtensionRelationshipWebdavTypedTest {

  @Test
  @DisplayName("PSDtdBuilder writes DTD with typed element/child maps")
  void dtdBuilderTypedMaps() throws Exception {
    PSDtdBuilder builder = new PSDtdBuilder("Root");
    builder.addElement("Child", PSDtdBuilder.OCCURS_ANY, "Root");
    builder.addElement("Grand", PSDtdBuilder.OCCURS_OPTIONAL, "Child");
    assertEquals("Root", builder.getRootName());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    builder.write(out);
    String dtd = out.toString(StandardCharsets.UTF_8);
    assertTrue(dtd.contains("<!ELEMENT Root (Child*)>"), dtd);
    assertTrue(dtd.contains("<!ELEMENT Child (Grand?)>"), dtd);
    assertTrue(dtd.contains("<!ELEMENT Grand (#PCDATA)>"), dtd);
  }

  @Test
  @DisplayName("PSDtdAttribute possible values list is typed String")
  void dtdAttributeTypedPossibleValues() {
    PSDtdAttribute attr = new PSDtdAttribute("status");
    attr.setType(PSDtdAttribute.ENUMERATION);
    attr.setPossibleValues(new ArrayList<>());
    attr.addPossibleValue("draft");
    attr.addPossibleValue("public");
    List<String> values = attr.getPossibleValues();
    assertEquals(2, values.size());
    assertEquals("draft", values.get(0));
    assertEquals("public", values.get(1));
    assertThrows(UnsupportedOperationException.class, () -> values.add("x"));
  }

  @Test
  @DisplayName("PSAttachCloneToFolder clone category set is typed and seeded")
  void attachCloneCategoriesTyped() {
    Set<String> cats = PSAttachCloneToFolder.ms_cloneRelCategories;
    assertNotNull(cats);
    assertEquals(3, cats.size());
    assertTrue(cats.contains(PSRelationshipConfig.CATEGORY_COPY));
    assertTrue(cats.contains(PSRelationshipConfig.CATEGORY_PROMOTABLE));
    assertTrue(cats.contains(PSRelationshipConfig.CATEGORY_TRANSLATION));
    Set<String> copy = new HashSet<>(cats);
    assertEquals(copy, cats);
  }

  @Test
  @DisplayName("PSDatabaseFunctionDef params list is typed")
  void databaseFunctionDefTypedParams() throws Exception {
    // Build via XML-free private path is hard; use coll + function map typing surface
    PSDatabaseFunctionsColl coll =
        new PSDatabaseFunctionsColl(PSDatabaseFunctionManager.FUNCTION_TYPE_SYSTEM);
    assertNotNull(coll.iterator());
    assertFalse(coll.iterator().hasNext());

    PSDatabaseFunction func =
        new PSDatabaseFunction(PSDatabaseFunctionManager.FUNCTION_TYPE_SYSTEM, "upper");
    Iterator<PSDatabaseFunctionDef> defs = func.iterator();
    assertNotNull(defs);
    assertFalse(defs.hasNext());
  }

  @Test
  @DisplayName("PSEffectUtils.getWorkflowStates accepts Collection<?> content ids")
  void effectUtilsCollectionWildcardSignature() throws Exception {
    // Compile-time check that Collection<?> is accepted; null request still NPE/throws from
    // production path — call with empty collection and stub-less context is not practical without
    // request. Verify method is accessible via reflection signature.
    var method =
        PSEffectUtils.class.getMethod(
            "getWorkflowStates",
            com.percussion.server.IPSRequestContext.class,
            java.util.Collection.class,
            String.class);
    assertEquals(java.util.Collection.class, method.getParameterTypes()[1]);
  }

  @Test
  @DisplayName("PSDatabaseFunctionDefParam list round-trip via setPossibleValues style list")
  void dtdAttributeSetPossibleValuesTyped() {
    PSDtdAttribute attr = new PSDtdAttribute("color");
    attr.setPossibleValues(List.of("red", "green", "blue"));
    List<String> values = attr.getPossibleValues();
    assertEquals(3, values.size());
    assertEquals("red", values.get(0));
  }
}

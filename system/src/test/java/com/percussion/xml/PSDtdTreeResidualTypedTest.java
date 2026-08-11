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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Residual DTD merge/catalog typing tests for #2944.
 */
@Tag("UnitTest")
@DisplayName("DTD tree residual generics")
class PSDtdTreeResidualTypedTest {

  @Test
  @DisplayName("elementKeyIterator is Iterator of String")
  void elementKeyIteratorTyped() throws Exception {
    var method = PSDtdTree.class.getMethod("elementKeyIterator");
    assertTrue(method.getGenericReturnType().getTypeName().contains("String"));
  }

  @Test
  @DisplayName("getCatalog returns List of String")
  void catalogTyped() throws Exception {
    var method = PSDtdTree.class.getMethod("getCatalog", String.class, String.class);
    assertEquals(List.class, method.getReturnType());
    assertTrue(method.getGenericReturnType().getTypeName().contains("String"));
  }

  @Test
  @DisplayName("PSDtdTreeMergeManager updateTreeForUserMod accepts typed catalog paths")
  void mergeManagerUpdateSignature() throws Exception {
    var method =
        PSDtdTreeMergeManager.class.getMethod(
            "updateTreeForUserMod", PSDtdTree.class, PSDtdTree.class);
    assertNotNull(method);
    assertEquals(PSDtdTree.class, method.getReturnType());
  }
}

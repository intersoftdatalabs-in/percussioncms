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
package com.percussion.fastforward.sfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link PSSqlInList} generics cleanup (issue #2323 batch 2). */
class PSSqlInListTest {

  @Test
  void emptyLiteralInClause() {
    PSSqlInList list = new PSSqlInList();
    assertEquals("('')", list.toString());
  }

  @Test
  void emptyNumericInClause() {
    PSSqlInList list = new PSSqlInList();
    list.setType(PSSqlInList.TYPE_NUMERIC);
    assertEquals("()", list.toString());
  }

  @Test
  void literalValuesQuotedAndCommaSeparated() {
    PSSqlInList list = new PSSqlInList(Arrays.asList("a", "b", "c"));
    assertEquals("('a','b','c')", list.toString());
  }

  @Test
  void numericValuesUnquoted() {
    PSSqlInList list = new PSSqlInList();
    list.setType(PSSqlInList.TYPE_NUMERIC);
    list.add(1);
    list.add(2);
    list.add(Integer.valueOf(3));
    assertEquals("(1,2,3)", list.toString());
  }

  @Test
  void collectionConstructorPreservesMembers() {
    List<Object> source = Arrays.asList(10, 20);
    PSSqlInList list = new PSSqlInList(source);
    list.setType(PSSqlInList.TYPE_NUMERIC);
    assertEquals(2, list.size());
    assertEquals("(10,20)", list.toString());
  }
}

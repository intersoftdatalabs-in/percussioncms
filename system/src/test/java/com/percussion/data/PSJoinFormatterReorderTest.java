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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndJoin;
import com.percussion.design.objectstore.PSBackEndTable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed {@link PSJoinFormatter#getReorderedJoins(List)}. */
@Tag("UnitTest")
class PSJoinFormatterReorderTest {

  @Test
  void outerJoinsMoveAheadOfInnerJoins() {
    PSBackEndTable t1 = new PSBackEndTable("t1");
    PSBackEndTable t2 = new PSBackEndTable("t2");
    PSBackEndTable t3 = new PSBackEndTable("t3");

    PSBackEndJoin inner =
        new PSBackEndJoin(new PSBackEndColumn(t1, "id"), new PSBackEndColumn(t2, "id"));
    inner.setInnerJoin();

    PSBackEndJoin outer =
        new PSBackEndJoin(new PSBackEndColumn(t2, "id"), new PSBackEndColumn(t3, "id"));
    outer.setLeftOuterJoin();

    List<PSBackEndJoin> joins = new ArrayList<>();
    joins.add(inner);
    joins.add(outer);

    List<PSBackEndJoin> reordered = PSJoinFormatter.getReorderedJoins(joins);
    assertEquals(2, reordered.size());
    assertTrue(reordered.get(0).isLeftOuterJoin());
    assertTrue(reordered.get(1).isInnerJoin());
  }
}

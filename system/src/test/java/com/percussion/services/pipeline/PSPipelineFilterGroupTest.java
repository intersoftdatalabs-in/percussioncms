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

package com.percussion.services.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.pipeline.model.FilterGroupIr;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Nested pipeline filter groups")
class PSPipelineFilterGroupTest {

  @Test
  void validate_rejectsEmptyGroupAndMissingColumn() {
    FilterGroupIr empty = new FilterGroupIr();
    empty.setType(FilterGroupIr.TYPE_GROUP);
    empty.setOp(FilterGroupIr.OP_AND);
    PSPipelineIrException noChild =
        assertThrows(PSPipelineIrException.class, () -> PSPipelineFilterGroup.validate(empty));
    assertTrue(noChild.getMessage().toLowerCase().contains("child"), noChild.getMessage());

    FilterGroupIr pred = predicate("sku", "=", "SKU-1");
    pred.setLeft("");
    FilterGroupIr root = group(FilterGroupIr.OP_AND, pred);
    PSPipelineIrException noCol =
        assertThrows(PSPipelineIrException.class, () -> PSPipelineFilterGroup.validate(root));
    assertTrue(noCol.getMessage().toLowerCase().contains("column"), noCol.getMessage());
  }

  @Test
  void filterRows_nestedAndOrMatchesFixtureShape() throws Exception {
    FilterGroupIr nested =
        group(FilterGroupIr.OP_OR, predicate("qty", "=", "3"), predicate("qty", "=", "99"));
    FilterGroupIr root = group(FilterGroupIr.OP_AND, predicate("sku", "=", "SKU-1"), nested);
    List<Map<String, Object>> rows =
        List.of(
            Map.of("sku", "SKU-1", "name", "Loopback Widget", "qty", 3),
            Map.of("sku", "SKU-2", "name", "Local Gadget", "qty", 7));
    List<Map<String, Object>> out = PSPipelineFilterGroup.filterRows(root, rows, Map.of());
    assertEquals(1, out.size());
    assertEquals("SKU-1", out.get(0).get("sku"));
  }

  private static FilterGroupIr group(String op, FilterGroupIr... children) {
    FilterGroupIr g = new FilterGroupIr();
    g.setType(FilterGroupIr.TYPE_GROUP);
    g.setOp(op);
    g.setChildren(List.of(children));
    return g;
  }

  private static FilterGroupIr predicate(String left, String operator, String right) {
    FilterGroupIr p = new FilterGroupIr();
    p.setType(FilterGroupIr.TYPE_PREDICATE);
    p.setLeftKind("COLUMN");
    p.setLeft(left);
    p.setOperator(operator);
    p.setRightKind("LITERAL");
    p.setRight(right);
    return p;
  }
}

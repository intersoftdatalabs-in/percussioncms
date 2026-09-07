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

import type { PipelineFilterGroup } from "../api/developer/types";

export function predicateNode(
  left: string,
  operator: string,
  right: string,
): PipelineFilterGroup {
  return {
    type: "PREDICATE",
    leftKind: "COLUMN",
    left,
    operator,
    rightKind: "LITERAL",
    right,
    omitWhenNull: false,
  };
}

/** Default nested group matching the bundled HTTP fixture: SKU-1 AND (qty=3 OR qty=99). */
export function defaultNestedFilterGroup(): PipelineFilterGroup {
  return {
    type: "GROUP",
    op: "AND",
    children: [
      predicateNode("sku", "=", "SKU-1"),
      {
        type: "GROUP",
        op: "OR",
        children: [predicateNode("qty", "=", "3"), predicateNode("qty", "=", "99")],
      },
    ],
  };
}

export function clientFilterGroupError(group: PipelineFilterGroup): string | null {
  if ((group.type || "GROUP").toUpperCase() !== "GROUP") {
    return "Filter group root must be type GROUP";
  }
  const op = (group.op || "").toUpperCase();
  if (op !== "AND" && op !== "OR") {
    return "Filter group op must be AND or OR";
  }
  const children = group.children || [];
  if (children.length === 0) {
    return "Filter group must have at least one child";
  }
  for (const child of children) {
    const err = clientNodeError(child);
    if (err) return err;
  }
  return null;
}

function clientNodeError(node: PipelineFilterGroup | undefined): string | null {
  if (!node) {
    return "Filter group contains a null node";
  }
  const type = (node.type || "").toUpperCase();
  if (type === "GROUP") {
    return clientFilterGroupError(node);
  }
  if (type !== "PREDICATE") {
    return "Filter node type must be GROUP or PREDICATE";
  }
  if (!(node.left || "").trim()) {
    return "Filter predicate column is required";
  }
  if (!(node.operator || "").trim()) {
    return "Filter predicate operator is required";
  }
  if (!(node.right || "").trim()) {
    return "Filter predicate value is required";
  }
  return null;
}

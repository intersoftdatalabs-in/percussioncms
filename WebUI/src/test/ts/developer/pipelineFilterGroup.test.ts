/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  clientFilterGroupError,
  defaultNestedFilterGroup,
} from "../../../main/ts/developer/pipelineFilterGroup";

describe("pipelineFilterGroup", () => {
  it("default nested group is valid", () => {
    expect(clientFilterGroupError(defaultNestedFilterGroup())).toBeNull();
  });

  it("rejects missing predicate column", () => {
    const g = defaultNestedFilterGroup();
    if (g.children?.[0]) g.children[0].left = "  ";
    expect(clientFilterGroupError(g)).toMatch(/column/i);
  });
});

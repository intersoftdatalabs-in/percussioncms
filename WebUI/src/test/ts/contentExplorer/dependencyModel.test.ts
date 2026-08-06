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
 */
import { describe, expect, it } from "vitest";
import {
  DEPENDENCY_DIMENSIONS,
  composeFromServerSummary,
  labelFor,
  synthesiseRelationshipSummary,
  totalKnownEdges,
} from "../../../main/ts/contentExplorer/views/dependencyModel";
import type { PSItemProperties } from "../../../main/ts/api/contentExplorer/types";
import type { PSNodeRelationshipSummary } from "../../../main/ts/api/contentExplorer/relationship";

function item(id = "i-1"): PSItemProperties {
  return { id, folderPath: "/Sites/Foo", type: "page" };
}

describe("DEPENDENCY_DIMENSIONS", () => {
  it("contains the 6 capability-matrix P-Adv rows", () => {
    expect([...DEPENDENCY_DIMENSIONS]).toEqual([
      "outgoing",
      "incoming",
      "aa",
      "taxonomy",
      "local",
      "reverse",
    ]);
  });
});

describe("labelFor", () => {
  it("returns the human-readable label per dimension", () => {
    expect(labelFor("outgoing")).toBe("Outgoing relationships");
    expect(labelFor("incoming")).toBe("Incoming relationships");
    expect(labelFor("aa")).toBe("Active Assembly links");
    expect(labelFor("taxonomy")).toBe("Site / taxonomy edges");
    expect(labelFor("local")).toBe("Local dependencies");
    expect(labelFor("reverse")).toBe("Reverse dependencies");
  });
});

describe("synthesiseRelationshipSummary", () => {
  it("marks the AA dimension as known with the supplied count", () => {
    const summary = synthesiseRelationshipSummary(item(), 3);
    const aa = summary.dimensions.find((d) => d.dimension === "aa");
    expect(aa?.count).toBe(3);
    expect(aa?.unknown).toBeUndefined();
    expect(aa?.label).toBe("3 AA links");
  });

  it("marks singular AA link with the singular label", () => {
    const summary = synthesiseRelationshipSummary(item(), 1);
    const aa = summary.dimensions.find((d) => d.dimension === "aa");
    expect(aa?.label).toBe("1 AA link");
  });

  it("marks non-AA dimensions unknown=true (per the rest gap)", () => {
    const summary = synthesiseRelationshipSummary(item(), 0);
    for (const d of summary.dimensions) {
      if (d.dimension === "aa") continue;
      expect(d.unknown).toBe(true);
      expect(d.count).toBe(0);
    }
  });

  it("sets clientSideOnly=true to drive the UI banner", () => {
    expect(synthesiseRelationshipSummary(item(), 0).clientSideOnly).toBe(true);
  });

  it("propagates the source item id and folderPath into the summary", () => {
    const summary = synthesiseRelationshipSummary(item("xyz"), 0);
    expect(summary.nodeId).toBe("xyz");
    expect(summary.nodePath).toBe("/Sites/Foo");
  });
});

describe("totalKnownEdges", () => {
  it("sums the known (non-unknown) dimensions", () => {
    const summary = synthesiseRelationshipSummary(item(), 3);
    expect(totalKnownEdges(summary)).toBe(3);
  });
  it("returns 0 when all dimensions are unknown", () => {
    const summary = synthesiseRelationshipSummary(item(), 0);
    expect(totalKnownEdges(summary)).toBe(0);
  });
});

describe("composeFromServerSummary (US8 / T102)", () => {
  const server: PSNodeRelationshipSummary = {
    outgoing: { count: 3, byType: [{ type: "translation", count: 3 }] },
    incoming: { count: 1, byType: [{ type: "translation", count: 1 }] },
    taxonomy: { count: 2, nodes: ["a", "b"] },
    local: { count: 4, links: [{ type: "local", targetId: "x" }] },
    reverse: { count: 5, byType: [{ type: "linkback", count: 5 }] },
  };

  it("marks the summary as server-authoritative (clientSideOnly=false)", () => {
    const summary = composeFromServerSummary(item(), server, 2);
    expect(summary.clientSideOnly).toBe(false);
  });

  it("uses the supplied AA-link count for the AA row", () => {
    const summary = composeFromServerSummary(item(), server, 7);
    const aa = summary.dimensions.find((d) => d.dimension === "aa");
    expect(aa?.count).toBe(7);
    expect(aa?.label).toBe("7 AA links");
  });

  it("promotes the per-type breakdown into the row label", () => {
    const summary = composeFromServerSummary(item(), server, 0);
    const out = summary.dimensions.find((d) => d.dimension === "outgoing");
    expect(out?.label).toContain("3");
    expect(out?.label).toContain("translation");
  });

  it("renders the taxonomy path list as a count label", () => {
    const summary = composeFromServerSummary(item(), server, 0);
    const tax = summary.dimensions.find((d) => d.dimension === "taxonomy");
    expect(tax?.count).toBe(2);
    expect(tax?.label).toMatch(/2 nodes/);
  });

  it("uses the local-link count for the local row", () => {
    const summary = composeFromServerSummary(item(), server, 0);
    const local = summary.dimensions.find((d) => d.dimension === "local");
    expect(local?.count).toBe(4);
    expect(local?.label).toBe("4 local links");
  });
});

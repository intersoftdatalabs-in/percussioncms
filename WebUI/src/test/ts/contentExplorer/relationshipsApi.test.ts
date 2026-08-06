/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import { describe, expect, it, vi, afterEach } from "vitest";
import {
  fetchNodeSummary,
  fetchOutgoing,
  RelationshipSummaryAuthError,
} from "../../../main/ts/api/contentExplorer/relationshipsApi";
import type { PSRelationshipSummary } from "../../../main/ts/api/contentExplorer/relationship";

const SAMPLE_OUTGOING: PSRelationshipSummary = {
  count: 3,
  byType: [{ type: "translation", count: 3 }],
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("relationshipsApi", () => {
  it("returns the typed summary on 200", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify(SAMPLE_OUTGOING), { status: 200 }),
      ),
    );
    const result = await fetchOutgoing("123");
    expect(result).toEqual(SAMPLE_OUTGOING);
  });

  it("throws RelationshipSummaryAuthError on 403", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("denied", { status: 403 })),
    );
    await expect(fetchOutgoing("private")).rejects.toBeInstanceOf(
      RelationshipSummaryAuthError,
    );
  });

  it("fetchNodeSummary preserves the consolidated shape", async () => {
    const consolidated = {
      outgoing: SAMPLE_OUTGOING,
      incoming: { count: 0, byType: [] },
      taxonomy: { count: 0, nodes: [] },
      local: { count: 0, links: [] },
      reverse: { count: 0, byType: [] },
    };
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () => new Response(JSON.stringify(consolidated), { status: 200 }),
      ),
    );
    const result = await fetchNodeSummary("node-1");
    expect(result).toEqual(consolidated);
  });
});

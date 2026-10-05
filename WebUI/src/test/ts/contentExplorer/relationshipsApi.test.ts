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
  fetchRelationshipEdges,
  isActiveAssemblyRelationship,
  relatedItemOpenTarget,
  relationshipMoveEnds,
  removeAllOwnedRelationshipEdges,
  removeRelationshipEdge,
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

  it("throws RelationshipSummaryAuthError on 404", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("missing", { status: 404 })),
    );
    await expect(fetchOutgoing("999")).rejects.toMatchObject({ status: 404 });
  });

  it("attaches HTTP status on a 404 so the viewer can name it", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("missing", { status: 404 })),
    );
    try {
      await fetchNodeSummary("999999");
      throw new Error("expected 404");
    } catch (err) {
      expect(err).toBeInstanceOf(Error);
      expect((err as { status?: number }).status).toBe(404);
    }
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

  it("lists removable edges and deletes one by id", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "DELETE") {
        return new Response(null, { status: 204 });
      }
      return new Response(
        JSON.stringify({
          items: [
            {
              relationshipId: 7,
              configName: "Translation",
              category: "rs_translation",
              dependentId: 9,
              label: "Translation -> 9",
            },
          ],
        }),
        { status: 200 },
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    const edges = await fetchRelationshipEdges("42");
    expect(edges[0].relationshipId).toBe(7);
    await removeRelationshipEdge("42", 7);
    const deleteCall = fetchMock.mock.calls.find(
      (call) => call[1] && (call[1] as RequestInit).method === "DELETE",
    );
    expect(String(deleteCall?.[0])).toContain("/relationships/42/edges/7");
  });

  it("reads the snippet template on an explorer relationship row (#5219)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              items: [
                {
                  relationshipId: 71,
                  configName: "ActiveAssembly",
                  category: "rs_activeassembly",
                  dependentId: 4,
                  label: "AA first",
                  slotId: 5,
                  templateId: 4,
                  templateName: "Brief",
                },
              ],
            }),
            { status: 200 },
          ),
      ),
    );
    const edges = await fetchRelationshipEdges("42");
    expect(edges[0]).toMatchObject({
      relationshipId: 71,
      slotId: 5,
      templateId: 4,
      templateName: "Brief",
    });
  });

  it("deletes every owned edge and skips folder rows (#4988)", async () => {
    const remove = vi.fn().mockResolvedValue(undefined);
    await removeAllOwnedRelationshipEdges(
      "42",
      [
        {
          relationshipId: 7,
          configName: "Translation",
          category: "rs_translation",
          dependentId: 9,
          label: "Translation -> 9",
        },
        {
          relationshipId: 8,
          configName: "Folder",
          category: "rs_folder",
          dependentId: 3,
          label: "Folder",
        },
        {
          relationshipId: 11,
          configName: "Active Assembly",
          category: "rs_aa",
          dependentId: 4,
          label: "AA -> 4",
        },
      ],
      remove,
    );
    expect(remove.mock.calls).toEqual([
      ["42", 7],
      ["42", 11],
    ]);
  });

  it("does not claim a full delete when a later edge returns 409 (#4988)", async () => {
    const remove = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce({ status: 409 });
    await expect(
      removeAllOwnedRelationshipEdges(
        "42",
        [
          {
            relationshipId: 7,
            configName: "Translation",
            category: "rs_translation",
            dependentId: 9,
            label: "Translation -> 9",
          },
          {
            relationshipId: 11,
            configName: "Active Assembly",
            category: "rs_aa",
            dependentId: 4,
            label: "AA -> 4",
          },
        ],
        remove,
      ),
    ).rejects.toMatchObject({ status: 409 });
    expect(remove).toHaveBeenCalledTimes(2);
  });

  it("rejects a non-positive relationship id before DELETE", async () => {
    await expect(removeRelationshipEdge("42", 0)).rejects.toMatchObject({
      status: 400,
    });
  });

  it("moves only Active Assembly edges and stops at the ends (#5201)", () => {
    const translation = {
      relationshipId: 7,
      configName: "Translation",
      category: "rs_translation",
      dependentId: 9,
      label: "Translation -> 9",
    };
    const first = {
      relationshipId: 11,
      configName: "Active Assembly",
      category: "rs_aa",
      dependentId: 4,
      label: "AA first",
    };
    const last = {
      relationshipId: 12,
      configName: "Widget-Content",
      category: "rs_activeassembly",
      dependentId: 5,
      label: "AA last",
    };
    const otherSlot = {
      relationshipId: 14,
      configName: "ActiveAssembly",
      category: "rs_activeassembly",
      dependentId: 8,
      label: "AA other slot",
      slotId: 9,
    };
    const folder = {
      relationshipId: 8,
      configName: "Active Assembly",
      category: "rs_folder",
      dependentId: 3,
      label: "Folder",
    };
    const alone = {
      relationshipId: 13,
      configName: "Active Assembly",
      category: "rs_aa",
      dependentId: 6,
      label: "AA only",
    };
    expect(isActiveAssemblyRelationship(first)).toBe(true);
    expect(isActiveAssemblyRelationship(last)).toBe(true);
    expect(isActiveAssemblyRelationship(translation)).toBe(false);
    expect(isActiveAssemblyRelationship(folder)).toBe(false);
    const rows = [translation, first, folder, last];
    expect(relationshipMoveEnds(rows, first)).toEqual({ up: false, down: true });
    expect(relationshipMoveEnds(rows, last)).toEqual({ up: true, down: false });
    expect(relationshipMoveEnds(rows, translation)).toEqual({
      up: false,
      down: false,
    });
    expect(relationshipMoveEnds(rows, folder)).toEqual({ up: false, down: false });
    expect(relationshipMoveEnds([alone], alone)).toEqual({ up: false, down: false });
    const slotted = [
      { ...first, slotId: 3 },
      { ...otherSlot },
      { ...last, slotId: 3 },
    ];
    expect(relationshipMoveEnds(slotted, slotted[0])).toEqual({
      up: false,
      down: true,
    });
    expect(relationshipMoveEnds(slotted, otherSlot)).toEqual({
      up: false,
      down: false,
    });
    expect(relationshipMoveEnds(slotted, slotted[2])).toEqual({
      up: true,
      down: false,
    });
  });

  it("opens only a related row that has a content id (#5218)", () => {
    expect(
      relatedItemOpenTarget({
        category: "rs_translation",
        configName: "Translation",
        dependentId: 9,
      }),
    ).toEqual({ kind: "content", contentId: 9 });
    expect(
      relatedItemOpenTarget({
        category: "rs_folder",
        configName: "Folder",
        dependentId: 3,
      }),
    ).toEqual({ kind: "folder" });
    expect(
      relatedItemOpenTarget({
        category: "rs_translation",
        configName: "Folder",
        dependentId: 4,
      }),
    ).toEqual({ kind: "folder" });
    expect(
      relatedItemOpenTarget({
        category: "rs_translation",
        configName: "Translation",
        dependentId: 0,
      }),
    ).toEqual({ kind: "no_content" });
  });
});

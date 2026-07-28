/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  fetchContentActivity,
  fetchItemsByWorkflowState,
  groupItemsByStatus,
  normalizeContentActivityRow,
  unwrapNamedList,
} from "@/api/dashboard/gadgetApi";
import * as client from "@/api/client";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    post: vi.fn(),
  };
});

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchSites: vi.fn().mockResolvedValue([{ name: "Demo" }]),
  };
});

describe("gadgetApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("unwrapNamedList handles array and root envelope", () => {
    expect(unwrapNamedList([{ a: 1 }], "X")).toEqual([{ a: 1 }]);
    expect(
      unwrapNamedList({ ContentActivity: [{ name: "n" }] }, "ContentActivity"),
    ).toEqual([{ name: "n" }]);
    expect(
      unwrapNamedList({ ContentActivity: { name: "solo" } }, "ContentActivity"),
    ).toEqual([{ name: "solo" }]);
  });

  it("normalizeContentActivityRow maps counts", () => {
    const row = normalizeContentActivityRow({
      name: "Demo",
      siteName: "Demo",
      publishedItems: 10,
      pendingItems: "2",
      newItems: 1,
      updatedItems: 3,
      archivedItems: 0,
    });
    expect(row).toMatchObject({
      name: "Demo",
      publishedItems: 10,
      pendingItems: 2,
      newItems: 1,
      updatedItems: 3,
      archivedItems: 0,
    });
  });

  it("fetchContentActivity POSTs ContentActivityRequest envelope", async () => {
    vi.mocked(client.post).mockResolvedValue({
      ContentActivity: [
        {
          name: "Demo",
          publishedItems: 1,
          pendingItems: 0,
          newItems: 0,
          updatedItems: 0,
          archivedItems: 0,
        },
      ],
    });
    const rows = await fetchContentActivity("/Sites/Demo", "days", 30);
    expect(client.post).toHaveBeenCalledWith(
      PATHS.ACTIVITY_CONTENT,
      expect.objectContaining({
        ContentActivityRequest: {
          path: "/Sites/Demo",
          durationType: "days",
          duration: "30",
        },
      }),
    );
    expect(rows).toHaveLength(1);
    expect(rows[0].name).toBe("Demo");
  });

  it("fetchItemsByWorkflowState POSTs ItemByWfStateRequest", async () => {
    vi.mocked(client.post).mockResolvedValue({
      ItemProperties: [
        { id: "1", name: "Home", status: "Draft", path: "/Sites/Demo/Home" },
        { id: "2", name: "About", status: "Draft", path: "/Sites/Demo/About" },
        { id: "3", name: "Live", status: "Live", path: "/Sites/Demo/Live" },
      ],
    });
    const items = await fetchItemsByWorkflowState(
      "/Sites/Demo",
      "Default Workflow",
      "",
    );
    expect(client.post).toHaveBeenCalledWith(
      PATHS.PATH_ITEM_BY_WF_STATE,
      expect.objectContaining({
        ItemByWfStateRequest: {
          path: "/Sites/Demo",
          workflow: "Default Workflow",
          state: "",
        },
      }),
    );
    expect(items).toHaveLength(3);
  });

  it("groupItemsByStatus aggregates counts", () => {
    const buckets = groupItemsByStatus([
      { name: "a", status: "Draft" },
      { name: "b", status: "Draft" },
      { name: "c", status: "Live" },
    ]);
    expect(buckets.find((b) => b.state === "Draft")?.count).toBe(2);
    expect(buckets.find((b) => b.state === "Live")?.count).toBe(1);
  });
});

/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  fetchContentActivity,
  fetchItemsByWorkflowState,
  fetchProcessMonitors,
  fetchGlobalVariables,
  fetchFormsForSite,
  fetchContentTraffic,
  fetchEffectiveness,
  formatTrafficDate,
  groupItemsByStatus,
  normalizeContentActivityRow,
  normalizeContentTraffic,
  normalizeEffectivenessRows,
  normalizeProcessMonitors,
  parseGlobalVariablesData,
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

  it("normalizeProcessMonitors reads stats.entries", () => {
    const rows = normalizeProcessMonitors({
      monitor: [
        {
          stats: {
            entries: {
              name: "Publish",
              designator: "pub",
              status: "idle",
              message: "ok",
            },
          },
        },
      ],
    });
    expect(rows).toEqual([
      {
        designator: "pub",
        name: "Publish",
        status: "idle",
        message: "ok",
      },
    ]);
  });

  it("fetchProcessMonitors GETs sitemanage/monitor/all", async () => {
    vi.mocked(client.get).mockResolvedValue({ monitor: [] });
    await fetchProcessMonitors();
    expect(client.get).toHaveBeenCalledWith(PATHS.MONITOR_ALL);
  });

  it("parseGlobalVariablesData handles map and variables array", () => {
    expect(
      parseGlobalVariablesData({ variables: [{ name: "a", value: "1" }] }),
    ).toEqual([{ name: "a", value: "1" }]);
    expect(parseGlobalVariablesData('{"siteName":"Demo"}')).toEqual([
      { name: "siteName", value: "Demo" },
    ]);
  });

  it("fetchGlobalVariables loads percglobalvariables metadata", async () => {
    vi.mocked(client.get).mockResolvedValue({
      metaData: {
        key: "percglobalvariables",
        data: JSON.stringify({ foo: "bar" }),
      },
    });
    const vars = await fetchGlobalVariables();
    expect(client.get).toHaveBeenCalledWith(
      `${PATHS.METADATA_FIND}/percglobalvariables`,
    );
    expect(vars).toEqual([{ name: "foo", value: "bar" }]);
  });

  it("fetchFormsForSite GETs asset forms", async () => {
    vi.mocked(client.get).mockResolvedValue({
      FormSummary: [
        {
          name: "contact",
          title: "Contact",
          totalSubmissions: 3,
          newSubmissions: 1,
        },
      ],
    });
    const forms = await fetchFormsForSite("Demo");
    expect(client.get).toHaveBeenCalledWith(`${PATHS.ASSET_FORMS}/Demo`);
    expect(forms[0]).toMatchObject({
      name: "contact",
      totalSubmissions: 3,
      newSubmissions: 1,
    });
  });

  it("formatTrafficDate uses MM/dd/yyyy", () => {
    expect(formatTrafficDate(new Date(2026, 0, 5))).toBe("01/05/2026");
  });

  it("normalizeContentTraffic builds points", () => {
    const r = normalizeContentTraffic(
      {
        ContentTraffic: {
          site: "Demo",
          dates: ["01/01/2026", "01/02/2026"],
          visits: [1, 2],
          livePages: [3, 4],
          newPages: [0, 1],
          pageUpdates: [0, 0],
          takeDowns: [0, 0],
        },
      },
      "/Sites/Demo",
      "01/01/2026",
      "01/02/2026",
    );
    expect(r.points).toHaveLength(2);
    expect(r.totalVisits).toBe(3);
    expect(r.points[1].livePages).toBe(4);
  });

  it("fetchContentTraffic POSTs ContentTrafficRequest", async () => {
    vi.mocked(client.post).mockResolvedValue({
      ContentTraffic: {
        dates: ["01/01/2026"],
        visits: [5],
        livePages: [1],
        newPages: [0],
        pageUpdates: [0],
        takeDowns: [0],
      },
    });
    await fetchContentTraffic({
      path: "/Sites/Demo",
      startDate: "01/01/2026",
      endDate: "01/31/2026",
      granularity: "DAY",
    });
    expect(client.post).toHaveBeenCalledWith(
      PATHS.ACTIVITY_TRAFFIC,
      expect.objectContaining({
        ContentTrafficRequest: expect.objectContaining({
          path: "/Sites/Demo",
          granularity: "DAY",
        }),
      }),
    );
  });

  it("normalizeEffectivenessRows and fetchEffectiveness", async () => {
    expect(
      normalizeEffectivenessRows({
        Effectiveness: [{ name: "A", effectiveness: 9 }],
      }),
    ).toEqual([{ name: "A", effectiveness: 9 }]);

    vi.mocked(client.post).mockResolvedValue({
      Effectiveness: [{ name: "A", effectiveness: 9 }],
    });
    const rows = await fetchEffectiveness({ path: "/Sites/Demo" });
    expect(client.post).toHaveBeenCalledWith(
      PATHS.ACTIVITY_EFFECTIVENESS,
      expect.objectContaining({
        EffectivenessRequest: expect.objectContaining({
          path: "/Sites/Demo",
          durationType: "days",
        }),
      }),
    );
    expect(rows[0].name).toBe("A");
  });
});


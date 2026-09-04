/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  getApplicationDetail,
  startApplication,
  stopApplication,
  unwrapApplicationDetail,
  withoutStalePipelineLifecycleGap,
} from "../../../../main/ts/api/developer/pipelinesApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("pipelinesApi Slice B lifecycle", () => {
  it("withoutStalePipelineLifecycleGap drops start/stop unsupported strings", () => {
    expect(
      withoutStalePipelineLifecycleGap([
        "Start / stop / enable application not supported via this API",
        "Pipe IR / SQL mapper / resource tanks not exposed (Pipelines Slice A+)",
        "Enable / disable application not supported via this API",
      ]),
    ).toEqual([
      "Pipe IR / SQL mapper / resource tanks not exposed (Pipelines Slice A+)",
      "Enable / disable application not supported via this API",
    ]);
  });

  it("unwrapApplicationDetail flattens ApplicationDetail root and strips stale gaps", () => {
    const detail = unwrapApplicationDetail({
      ApplicationDetail: {
        name: "sys_cmpDocuments",
        active: true,
        designGaps: ["Start / stop / enable application not supported via this API"],
      },
    });
    expect(detail.name).toBe("sys_cmpDocuments");
    expect(detail.active).toBe(true);
    expect(detail.designGaps).toEqual([]);
  });

  it("getApplicationDetail GETs encoded path and unwraps", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      name: "sys_cmpDocuments",
      active: false,
      designGaps: ["Pipe IR not exposed"],
    });
    const detail = await getApplicationDetail("sys_cmpDocuments");
    expect(String(spy.mock.calls[0][0])).toContain("/pipelines/sys_cmpDocuments");
    expect(detail.name).toBe("sys_cmpDocuments");
    expect(detail.active).toBe(false);
    expect(detail.designGaps).toEqual(["Pipe IR not exposed"]);
  });

  it("startApplication POSTs /start and returns refreshed detail", async () => {
    const spy = vi.spyOn(client, "post").mockResolvedValue({
      name: "sys_cmpDocuments",
      active: true,
    });
    const detail = await startApplication("sys_cmpDocuments");
    expect(String(spy.mock.calls[0][0])).toMatch(/\/pipelines\/sys_cmpDocuments\/start$/);
    expect(detail.active).toBe(true);
  });

  it("stopApplication POSTs /stop and returns refreshed detail", async () => {
    const spy = vi.spyOn(client, "post").mockResolvedValue({
      name: "sys_cmpDocuments",
      active: false,
    });
    const detail = await stopApplication("app with spaces");
    expect(String(spy.mock.calls[0][0])).toContain(
      `/pipelines/${encodeURIComponent("app with spaces")}/stop`,
    );
    expect(detail.active).toBe(false);
  });
});

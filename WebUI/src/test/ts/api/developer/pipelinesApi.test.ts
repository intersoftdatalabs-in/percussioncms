/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  getApplicationDetail,
  getPipelineIr,
  listApplications,
} from "../../../../main/ts/api/developer/pipelinesApi";
import { PATHS } from "../../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("pipelinesApi", () => {
  it("listApplications GETs catalog and unwraps Application array", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      Application: [{ name: "sys_cmpDocuments", id: 1 }],
    });
    const rows = await listApplications({ name: "sys", limit: 10, offset: 0 });
    expect(spy).toHaveBeenCalled();
    const url = String(spy.mock.calls[0][0]);
    expect(url.startsWith(PATHS.PIPELINES)).toBe(true);
    expect(url).toContain("name=sys");
    expect(url).toContain("limit=10");
    expect(url).toContain("offset=0");
    expect(rows).toEqual([{ name: "sys_cmpDocuments", id: 1 }]);
  });

  it("getApplicationDetail encodes idOrName", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({ name: "sys_cmpDocuments" });
    await getApplicationDetail("sys_cmpDocuments");
    expect(spy).toHaveBeenCalledWith(`${PATHS.PIPELINES}/sys_cmpDocuments`);
  });

  it("getPipelineIr GETs /ir with encoded idOrName", async () => {
    const doc = {
      irVersion: "1.0",
      source: "NATIVE",
      resources: [{ name: "Dataset34", kind: "QUERY" }],
    };
    const spy = vi.spyOn(client, "get").mockResolvedValue(doc);
    const result = await getPipelineIr("app/with space");
    expect(spy).toHaveBeenCalledWith(
      `${PATHS.PIPELINES}/${encodeURIComponent("app/with space")}/ir`,
    );
    expect(result).toEqual(doc);
  });
});

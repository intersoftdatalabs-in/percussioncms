/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  executeResource,
  getApplicationDetail,
  getApplicationValidation,
  startApplication,
  stopApplication,
  getPipelineIr,
  unwrapApplicationDetail,
  unwrapApplicationValidationResult,
  unwrapPipelineExecuteResult,
  withoutStalePipelineLifecycleGap,
  wrapPipelineExecuteRequestForWire,
  wrapPipelineHttpBackendTankForWire,
  unwrapPipelineHttpBackendTank,
  putHttpBackendTank,
} from "../../../../main/ts/api/developer/pipelinesApi";
import { PATHS } from "../../../../main/ts/api/paths";

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

describe("pipelinesApi wave 3 execute + validation", () => {
  it("unwrapPipelineExecuteResult flattens root wrap", () => {
    const result = unwrapPipelineExecuteResult({
      PipelineExecuteResult: {
        appName: "lookupApp",
        resourceName: "DatasetQ",
        rowCount: 2,
      },
    });
    expect(result.appName).toBe("lookupApp");
    expect(result.resourceName).toBe("DatasetQ");
    expect(result.rowCount).toBe(2);
  });

  it("wrapPipelineExecuteRequestForWire nests under PipelineExecuteRequest", () => {
    expect(wrapPipelineExecuteRequestForWire({ params: { TYPE: "workflow" } })).toEqual({
      PipelineExecuteRequest: { params: { TYPE: "workflow" } },
    });
  });

  it("executeResource POSTs encoded path with WRAP_ROOT body", async () => {
    const spy = vi.spyOn(client, "post").mockResolvedValue({
      appName: "lookupApp",
      resourceName: "DatasetQ",
      rows: [{ TYPE: "workflow" }],
      rowCount: 1,
    });
    const out = await executeResource("app with spaces", "res/one", {
      params: { TYPE: "workflow" },
    });
    expect(String(spy.mock.calls[0][0])).toContain(
      `/pipelines/${encodeURIComponent("app with spaces")}/resources/${encodeURIComponent("res/one")}/execute`,
    );
    expect(spy.mock.calls[0][1]).toEqual({
      PipelineExecuteRequest: { params: { TYPE: "workflow" } },
    });
    expect(out.rowCount).toBe(1);
    expect(out.rows?.[0]).toEqual({ TYPE: "workflow" });
  });

  it("unwrapApplicationValidationResult normalizes problems array", () => {
    const result = unwrapApplicationValidationResult({
      ApplicationValidationResult: {
        name: "sys_cmpDocuments",
        valid: true,
        problems: { severity: "WARNING", code: "1", message: "one" },
      },
    });
    expect(result.name).toBe("sys_cmpDocuments");
    expect(result.problems).toHaveLength(1);
    expect(result.problems?.[0].code).toBe("1");
  });

  it("getApplicationValidation GETs /validation", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      name: "sys_cmpDocuments",
      valid: true,
      problems: [],
    });
    const result = await getApplicationValidation("sys_cmpDocuments");
    expect(String(spy.mock.calls[0][0])).toMatch(
      /\/pipelines\/sys_cmpDocuments\/validation$/,
    );
    expect(result.valid).toBe(true);
    expect(result.problems).toEqual([]);
  });
});

describe("pipelinesApi Slice C HTTP backend tank", () => {
  it("wraps and unwraps PipelineHttpBackendTank", () => {
    expect(
      wrapPipelineHttpBackendTankForWire({
        adapterType: "HTTP",
        url: "http://127.0.0.1/pipeline-http-fixture",
      }),
    ).toEqual({
      PipelineHttpBackendTank: {
        adapterType: "HTTP",
        url: "http://127.0.0.1/pipeline-http-fixture",
      },
    });
    expect(
      unwrapPipelineHttpBackendTank({
        PipelineHttpBackendTank: { adapterType: "HTTP", url: "http://127.0.0.1/x" },
      }).url,
    ).toBe("http://127.0.0.1/x");
  });

  it("putHttpBackendTank PUTs encoded path with WRAP_ROOT body", async () => {
    const spy = vi.spyOn(client, "put").mockResolvedValue({
      adapterType: "HTTP",
      url: "http://127.0.0.1/pipeline-http-fixture",
    });
    const out = await putHttpBackendTank("app with spaces", "res/one", {
      adapterType: "HTTP",
      url: "http://127.0.0.1/pipeline-http-fixture",
    });
    expect(String(spy.mock.calls[0][0])).toContain(
      `/pipelines/${encodeURIComponent("app with spaces")}/resources/${encodeURIComponent("res/one")}/backendTank`,
    );
    expect(spy.mock.calls[0][1]).toEqual({
      PipelineHttpBackendTank: {
        adapterType: "HTTP",
        url: "http://127.0.0.1/pipeline-http-fixture",
      },
    });
    expect(out.adapterType).toBe("HTTP");
  });
});

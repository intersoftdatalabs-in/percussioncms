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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createWorkflow,
  getWorkflowAllowedContentTypes,
  isValidWorkflowName,
  isWorkflowCreateReady,
  normalizeWorkflowName,
  parseWorkflowDetail,
  parseWorkflowList,
  parseWorkflowSummary,
  setWorkflowAllowedContentTypes,
  wrapWorkflowContentTypesForWire,
  wrapWorkflowCreateForWire,
} from "../../../../main/ts/api/developer/workflowsApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("parseWorkflowList", () => {
  it("returns bare arrays", () => {
    const rows = [{ workflowName: "Default Workflow" }];
    expect(parseWorkflowList(rows)).toEqual(rows);
  });

  it("returns empty for null", () => {
    expect(parseWorkflowList(null)).toEqual([]);
    expect(parseWorkflowList(undefined)).toEqual([]);
  });

  it("unwraps Workflow root wrapper (PSUiWorkflowList @JsonRootName)", () => {
    expect(
      parseWorkflowList({
        Workflow: [
          { workflowName: "Default Workflow", defaultWorkflow: true },
          { workflowName: "Simple Workflow", defaultWorkflow: false },
        ],
      }),
    ).toEqual([
      { workflowName: "Default Workflow", defaultWorkflow: true },
      { workflowName: "Simple Workflow", defaultWorkflow: false },
    ]);
  });

  it("unwraps single Workflow object", () => {
    expect(
      parseWorkflowList({
        Workflow: { workflowName: "Only One", defaultWorkflow: true },
      }),
    ).toEqual([{ workflowName: "Only One", defaultWorkflow: true }]);
  });

  it("unwraps nested Workflow WRAP_ROOT (#3202)", () => {
    expect(
      parseWorkflowList({
        Workflow: {
          Workflow: [
            { workflowName: "Default Workflow", defaultWorkflow: true },
          ],
        },
      }),
    ).toEqual([{ workflowName: "Default Workflow", defaultWorkflow: true }]);
  });

  it("unwraps PSUiWorkflowList envelope with inner Workflow array", () => {
    expect(
      parseWorkflowList({
        PSUiWorkflowList: {
          Workflow: [{ workflowName: "Simple Workflow" }],
        },
      }),
    ).toEqual([{ workflowName: "Simple Workflow" }]);
  });

  it("unwraps WorkflowList / entries aliases", () => {
    expect(
      parseWorkflowList({ WorkflowList: [{ workflowName: "A" }] }),
    ).toEqual([{ workflowName: "A" }]);
    expect(parseWorkflowList({ entries: [{ workflowName: "B" }] })).toEqual([
      { workflowName: "B" },
    ]);
  });

  it("throws on unknown object shape", () => {
    expect(() => parseWorkflowList({ unexpected: true })).toThrow(
      /Unexpected workflow list payload/,
    );
  });

  it("throws on non-object non-array types", () => {
    expect(() => parseWorkflowList("not-json-list")).toThrow(
      /Unexpected workflow list payload type/,
    );
  });
});

describe("parseWorkflowDetail (#3562)", () => {
  it("returns a flat body with workflowName", () => {
    expect(
      parseWorkflowDetail({
        workflowName: "Default Workflow",
        defaultWorkflow: true,
      }),
    ).toEqual({
      workflowName: "Default Workflow",
      defaultWorkflow: true,
    });
  });

  it("unwraps Jackson Workflow WRAP_ROOT without top-level workflowName", () => {
    expect(
      parseWorkflowDetail({
        Workflow: {
          workflowName: "Default Workflow",
          workflowDescription: "Stock default",
          defaultWorkflow: true,
          workflowSteps: [{ stepName: "Draft" }],
        },
      }),
    ).toEqual({
      workflowName: "Default Workflow",
      workflowDescription: "Stock default",
      defaultWorkflow: true,
      workflowSteps: [{ stepName: "Draft" }],
    });
  });

  it("unwraps nested Workflow envelopes", () => {
    expect(
      parseWorkflowDetail({
        Workflow: {
          Workflow: { workflowName: "Simple Workflow", defaultWorkflow: false },
        },
      }),
    ).toEqual({
      workflowName: "Simple Workflow",
      defaultWorkflow: false,
    });
  });

  it("accepts name alias when workflowName is absent", () => {
    expect(
      parseWorkflowDetail({
        Workflow: { name: "Simple Workflow", defaultWorkflow: false },
      }),
    ).toEqual({
      name: "Simple Workflow",
      workflowName: "Simple Workflow",
      defaultWorkflow: false,
    });
  });

  it("unwraps one-item Workflow array envelope", () => {
    expect(
      parseWorkflowDetail({
        Workflow: [{ workflowName: "Default Workflow" }],
      }),
    ).toEqual({ workflowName: "Default Workflow" });
  });

  it("maps steps alias onto workflowSteps", () => {
    expect(
      parseWorkflowDetail({
        workflowName: "Default Workflow",
        steps: [{ stepName: "Review" }],
      }),
    ).toEqual({
      workflowName: "Default Workflow",
      steps: [{ stepName: "Review" }],
      workflowSteps: [{ stepName: "Review" }],
    });
  });

  it("throws when wrapped payload has no name", () => {
    expect(() => parseWorkflowDetail({ Workflow: { unexpected: true } })).toThrow(
      /missing workflowName/,
    );
  });

  it("throws on empty or non-object payloads", () => {
    expect(() => parseWorkflowDetail(null)).toThrow(/not found or empty/);
    expect(() => parseWorkflowDetail([])).toThrow(/not found or empty/);
    expect(() => parseWorkflowDetail("nope")).toThrow(/not found or empty/);
  });
});

describe("wrapWorkflowContentTypesForWire (SY-06)", () => {
  it("wraps allowedContentTypes under WorkflowContentTypes", () => {
    expect(
      wrapWorkflowContentTypesForWire({
        allowedContentTypes: [{ name: "percPage" }],
      }),
    ).toEqual({
      WorkflowContentTypes: {
        allowedContentTypes: [{ name: "percPage" }],
      },
    });
  });
});

describe("workflow allowed content types API (SY-06)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("GETs /workflows/{id}/allowedContentTypes and unwraps NamedObjectRefList", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        NamedObjectRefList: [{ name: "percPage", label: "Page" }],
      }),
    );
    const list = await getWorkflowAllowedContentTypes("Simple Workflow");
    expect(list).toEqual([{ name: "percPage", label: "Page" }]);
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Simple Workflow")}/allowedContentTypes`,
    );
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("GET");
  });

  it("PUTs WorkflowContentTypes wrap without a client-held lock", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        NamedObjectRefList: [{ name: "percImage" }],
      }),
    );
    const saved = await setWorkflowAllowedContentTypes("Simple Workflow", {
      allowedContentTypes: [{ name: "percImage" }],
    });
    expect(saved).toEqual([{ name: "percImage" }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Simple Workflow")}/allowedContentTypes`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      WorkflowContentTypes: {
        allowedContentTypes: [{ name: "percImage" }],
      },
    });
  });
});

describe("workflow name validation (slice 21 create)", () => {
  it("trims and accepts workflow-admin characters", () => {
    expect(normalizeWorkflowName("  Nightly QA-1_2 ")).toBe("Nightly QA-1_2");
    expect(isValidWorkflowName("Nightly QA-1_2")).toBe(true);
    expect(isWorkflowCreateReady({ name: "Nightly QA" })).toBe(true);
  });

  it("rejects blank, wildcard, too-long, and illegal names", () => {
    expect(isValidWorkflowName("  ")).toBe(false);
    expect(isValidWorkflowName(null)).toBe(false);
    expect(isValidWorkflowName("Bad*Name")).toBe(false);
    expect(isValidWorkflowName("Bad%Name")).toBe(false);
    expect(isValidWorkflowName("N".repeat(51))).toBe(false);
    expect(isValidWorkflowName("Bad/Name!")).toBe(false);
    expect(isWorkflowCreateReady({ name: "bad name?" })).toBe(false);
  });
});

describe("parseWorkflowSummary (slice 21 create)", () => {
  it("parses a flat summary body", () => {
    expect(
      parseWorkflowSummary({ workflowName: "Nightly QA", defaultWorkflow: false }),
    ).toEqual({
      workflowName: "Nightly QA",
      workflowDescription: undefined,
      defaultWorkflow: false,
    });
  });

  it("unwraps Jackson WorkflowSummary root", () => {
    expect(
      parseWorkflowSummary({
        WorkflowSummary: {
          workflowName: "Nightly QA",
          workflowDescription: "QA",
          defaultWorkflow: false,
        },
      }),
    ).toEqual({
      workflowName: "Nightly QA",
      workflowDescription: "QA",
      defaultWorkflow: false,
    });
  });

  it("throws when the name is missing", () => {
    expect(() => parseWorkflowSummary({})).toThrow(/workflowName/);
    expect(() => parseWorkflowSummary(null)).toThrow(/empty response/);
  });
});

describe("workflow create API (slice 21)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("POSTs WorkflowCreate wrap and parses the summary", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ workflowName: "Nightly QA", defaultWorkflow: false }),
    );
    const created = await createWorkflow({ name: "Nightly QA" });
    expect(created.workflowName).toBe("Nightly QA");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.WORKFLOWS_ASSOC);
    expect(JSON.parse(String(init.body))).toEqual(
      wrapWorkflowCreateForWire({ name: "Nightly QA" }),
    );
  });
});

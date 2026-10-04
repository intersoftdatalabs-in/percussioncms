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
  createWorkflowAgingTransition,
  deleteWorkflowAgingTransition,
  updateWorkflowAgingInterval,
  deleteWorkflow,
  deleteWorkflowStep,
  createWorkflowTransition,
  deleteWorkflowTransition,
  updateWorkflowTransition,
  workflowStepDeletePath,
  workflowTransitionDeletePath,
  getWorkflowAllowedContentTypes,
  isPositiveMinuteInterval,
  isValidWorkflowName,
  isWorkflowCreateReady,
  normalizeWorkflowName,
  parseWorkflowDetail,
  parseWorkflowGraph,
  parseWorkflowList,
  parseWorkflowSummary,
  setWorkflowAllowedContentTypes,
  addStepRole,
  removeStepRole,
  listStepRoleAssignments,
  parseStepRoleAssignments,
  renameWorkflow,
  setDefaultWorkflow,
  setStepRoleAssignment,
  setStepRoleInbox,
  setStepRoleNotify,
  wrapWorkflowStepRoleAddForWire,
  wrapWorkflowStepRoleAssignmentForWire,
  wrapWorkflowStepRoleInboxForWire,
  wrapWorkflowStepRoleNotifyForWire,
  updateWorkflow,
  wrapWorkflowContentTypesForWire,
  wrapWorkflowCreateForWire,
  wrapWorkflowRenameForWire,
  wrapWorkflowUpdateForWire,
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

describe("workflow update API (slice 21 update)", () => {
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

  it("wraps under WorkflowUpdate and includes description when provided", () => {
    expect(
      wrapWorkflowUpdateForWire({
        name: "Nightly QA",
        description: "Edited by surface spec",
      }),
    ).toEqual({
      WorkflowUpdate: {
        name: "Nightly QA",
        description: "Edited by surface spec",
      },
    });
  });

  it("wraps under WorkflowUpdate with no description key", () => {
    expect(wrapWorkflowUpdateForWire({ name: "Nightly QA" })).toEqual({
      WorkflowUpdate: { name: "Nightly QA" },
    });
  });

  it("PUTs WorkflowUpdate wrap to /workflows/{name} and parses the summary", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        workflowName: "Nightly QA",
        workflowDescription: "Edited",
        defaultWorkflow: false,
      }),
    );
    const updated = await updateWorkflow("Nightly QA", {
      name: "Nightly QA",
      description: "Edited",
    });
    expect(updated.workflowName).toBe("Nightly QA");
    expect(updated.workflowDescription).toBe("Edited");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      WorkflowUpdate: { name: "Nightly QA", description: "Edited" },
    });
  });

  it("URL-encodes the workflow idOrName", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ workflowName: "My WF", defaultWorkflow: false }),
    );
    await updateWorkflow("My WF", { name: "My WF" });
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain(encodeURIComponent("My WF"));
  });
});

describe("workflow rename API (slice 60)", () => {
  const fetchMock = vi.fn();

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("wraps under WorkflowRename and does not send a description", () => {
    expect(wrapWorkflowRenameForWire({ name: "Nightly QA 2" })).toEqual({
      WorkflowRename: { name: "Nightly QA 2" },
    });
  });

  it("POSTs WorkflowRename to /workflows/{name}/rename", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        workflowName: "Nightly QA 2",
        workflowDescription: "Keep me",
        defaultWorkflow: false,
      }),
    );
    const renamed = await renameWorkflow("Nightly QA", { name: "Nightly QA 2" });
    expect(renamed.workflowName).toBe("Nightly QA 2");
    expect(renamed.workflowDescription).toBe("Keep me");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/rename`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      WorkflowRename: { name: "Nightly QA 2" },
    });
  });
});

describe("workflow step role assignment API (slice 61)", () => {
  const fetchMock = vi.fn();

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("unwraps the assignment list and keeps Reader", () => {
    expect(
      parseStepRoleAssignments({
        WorkflowStepRoleAssignmentList: {
          assignments: [
            { stepName: "Draft", roleName: "Author", assignmentType: "READER" },
          ],
        },
      }),
    ).toEqual([{ stepName: "Draft", roleName: "Author", assignmentType: "READER" }]);
  });

  it("PUTs one role assignment and does not send the step name in the body", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        WorkflowStepRoleAssignmentList: {
          assignments: [
            { stepName: "Draft", roleName: "Author", assignmentType: "READER" },
          ],
        },
      }),
    );
    const rows = await setStepRoleAssignment("Nightly QA", "Draft", {
      roleName: "Author",
      assignmentType: "READER",
    });
    expect(rows[0].assignmentType).toBe("READER");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/steps/${encodeURIComponent("Draft")}/role-assignment`,
    );
    expect(JSON.parse(String(init.body))).toEqual(
      wrapWorkflowStepRoleAssignmentForWire({
        roleName: "Author",
        assignmentType: "READER",
      }),
    );
    expect(listStepRoleAssignments).toBeTypeOf("function");
  });
});

describe("workflow step role notify API (slice 65)", () => {
  const fetchMock = vi.fn();

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("PUTs notify for one role and does not send inbox or assignment type", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        WorkflowStepRoleAssignmentList: {
          assignments: [
            {
              stepName: "Draft",
              roleName: "Author",
              assignmentType: "ASSIGNEE",
              notify: false,
            },
          ],
        },
      }),
    );
    const rows = await setStepRoleNotify("Nightly QA", "Draft", {
      roleName: "Author",
      notify: false,
    });
    expect(rows[0].notify).toBe(false);
    expect(rows[0].assignmentType).toBe("ASSIGNEE");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/steps/${encodeURIComponent("Draft")}/role-notify`,
    );
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("role-assignment");
    const body = JSON.parse(String(init.body)) as {
      WorkflowStepRoleNotifyWrite: { roleName: string; notify: boolean };
    };
    expect(body).toEqual(
      wrapWorkflowStepRoleNotifyForWire({
        roleName: "Author",
        notify: false,
      }),
    );
    expect(body.WorkflowStepRoleNotifyWrite).not.toHaveProperty("inbox");
    expect(body.WorkflowStepRoleNotifyWrite).not.toHaveProperty("assignmentType");
  });
});

describe("workflow step role inbox API (slice 66)", () => {
  const fetchMock = vi.fn();

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("PUTs inbox for one role and does not send notify or assignment type", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        WorkflowStepRoleAssignmentList: {
          assignments: [
            {
              stepName: "Draft",
              roleName: "Author",
              assignmentType: "ASSIGNEE",
              notify: true,
              inbox: false,
            },
          ],
        },
      }),
    );
    const rows = await setStepRoleInbox("Nightly QA", "Draft", {
      roleName: "Author",
      inbox: false,
    });
    expect(rows[0].inbox).toBe(false);
    expect(rows[0].notify).toBe(true);
    expect(rows[0].assignmentType).toBe("ASSIGNEE");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/steps/${encodeURIComponent("Draft")}/role-inbox`,
    );
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("role-notify");
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("role-assignment");
    const body = JSON.parse(String(init.body)) as {
      WorkflowStepRoleInboxWrite: { roleName: string; inbox: boolean };
    };
    expect(body).toEqual(
      wrapWorkflowStepRoleInboxForWire({
        roleName: "Author",
        inbox: false,
      }),
    );
    expect(body.WorkflowStepRoleInboxWrite).not.toHaveProperty("notify");
    expect(body.WorkflowStepRoleInboxWrite).not.toHaveProperty("assignmentType");
  });
});

describe("workflow step role add API (slice 63)", () => {
  const fetchMock = vi.fn();

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("POSTs one role onto a step and does not send notify or inbox", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        WorkflowStepRoleAssignmentList: {
          assignments: [
            { stepName: "Draft", roleName: "System", assignmentType: "READER" },
          ],
        },
      }),
    );
    const rows = await addStepRole("Nightly QA", "Draft", {
      roleName: "System",
      assignmentType: "READER",
    });
    expect(rows[0].roleName).toBe("System");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/steps/${encodeURIComponent("Draft")}/roles`,
    );
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("role-assignment");
    const body = JSON.parse(String(init.body)) as {
      WorkflowStepRoleAdd: { roleName: string; assignmentType: string };
    };
    expect(body).toEqual(
      wrapWorkflowStepRoleAddForWire({
        roleName: "System",
        assignmentType: "READER",
      }),
    );
    expect(body.WorkflowStepRoleAdd).not.toHaveProperty("notify");
    expect(body.WorkflowStepRoleAdd).not.toHaveProperty("inbox");
  });

  it("DELETEs one role from a step and does not use role-assignment", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        WorkflowStepRoleAssignmentList: {
          assignments: [
            { stepName: "Draft", roleName: "Editor", assignmentType: "READER" },
          ],
        },
      }),
    );
    const rows = await removeStepRole("Nightly QA", "Draft", "Author");
    expect(rows[0].roleName).toBe("Editor");
    expect(rows.some((row) => row.roleName === "Author")).toBe(false);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/steps/${encodeURIComponent("Draft")}/roles/${encodeURIComponent("Author")}`,
    );
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("role-assignment");
    expect(init.body).toBeUndefined();
  });
});

describe("set default workflow API (slice 37)", () => {
  const fetchMock = vi.fn();

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("POSTs /workflows/{name}/default and parses the summary", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        workflowName: "Beta Flow",
        defaultWorkflow: true,
      }),
    );
    const updated = await setDefaultWorkflow("Beta Flow");
    expect(updated.workflowName).toBe("Beta Flow");
    expect(updated.defaultWorkflow).toBe(true);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Beta Flow")}/default`,
    );
  });
});

describe("workflow delete API (slice 21 delete)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("DELETEs /workflows/{name} and returns void on 204", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(null, { status: 204 }),
    );
    await expect(deleteWorkflow("Nightly QA")).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}`,
    );
  });

  it("URL-encodes the workflow idOrName", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(null, { status: 204 }),
    );
    await deleteWorkflow("My WF");
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain(encodeURIComponent("My WF"));
  });

  it("propagates the 409 conflict when the workflow still owns content items", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ error: "workflow still has associated items" }),
        { status: 409, headers: { "Content-Type": "application/json" } },
      ),
    );
    await expect(deleteWorkflow("In Use")).rejects.toMatchObject({ status: 409 });
  });
});

describe("workflow transition delete API (slice 33)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("builds a DELETE path with from, label, and to", () => {
    const path = workflowTransitionDeletePath("Nightly QA", "Draft", "Submit", "Review");
    expect(path.startsWith(`${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/transitions?`)).toBe(
      true,
    );
    const q = new URLSearchParams(path.split("?")[1]);
    expect(q.get("from")).toBe("Draft");
    expect(q.get("label")).toBe("Submit");
    expect(q.get("to")).toBe("Review");
  });

  it("DELETEs one transition and parses the returned graph", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [{ name: "Draft" }, { name: "Review" }],
          edges: [],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await deleteWorkflowTransition("Nightly QA", "Draft", "Submit", "Review");
    expect(graph.nodes).toHaveLength(2);
    expect(graph.edges).toEqual([]);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/transitions?");
  });

  it("propagates 404 when the transition is missing", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "not found" }), {
        status: 404,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      deleteWorkflowTransition("Nightly QA", "Draft", "Missing", "Review"),
    ).rejects.toMatchObject({ status: 404 });
  });
});

describe("workflow transition write API (slice 31)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("POSTs a wrapped transition and parses the graph", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [{ name: "Draft" }, { name: "Review" }],
          edges: [{ from: "Draft", to: "Review", label: "Send" }],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await createWorkflowTransition("Nightly QA", {
      from: "Draft",
      to: "Review",
      label: "Send",
    });
    expect(graph.edges?.[0]?.label).toBe("Send");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/transitions");
    expect(String(init.body)).toContain("WorkflowTransitionWrite");
    expect(String(init.body)).toContain("Send");
  });

  it("PUTs the new label and destination on the transition query", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [],
          edges: [{ from: "Draft", to: "Pending", label: "Send" }],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await updateWorkflowTransition("Nightly QA", "Draft", "Submit", "Review", {
      to: "Pending",
      label: "Send",
    });
    expect(graph.edges?.[0]?.to).toBe("Pending");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/transitions?");
    const q = new URLSearchParams(url.split("?")[1]);
    expect(q.get("from")).toBe("Draft");
    expect(q.get("label")).toBe("Submit");
    expect(q.get("to")).toBe("Review");
  });

  it("propagates 409 when the edge already exists", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "exists" }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      createWorkflowTransition("Nightly QA", { from: "Draft", to: "Review", label: "Submit" }),
    ).rejects.toMatchObject({ status: 409 });
  });

  it("POSTs a wrapped absolute aging transition and parses the aging edge", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [{ name: "Draft" }, { name: "Review" }],
          edges: [
            { from: "Draft", to: "Review", label: "Aging 15", aging: true, intervalMinutes: 15 },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await createWorkflowAgingTransition("Nightly QA", {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
    });
    expect(graph.edges?.[0]?.aging).toBe(true);
    expect(graph.edges?.[0]?.intervalMinutes).toBe(15);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/aging-transitions");
    expect(String(init.body)).toContain("WorkflowAgingTransitionWrite");
    expect(String(init.body)).toContain("15");
    expect(isPositiveMinuteInterval("15")).toBe(true);
    expect(isPositiveMinuteInterval("0")).toBe(false);
    expect(isPositiveMinuteInterval("-3")).toBe(false);
    expect(isPositiveMinuteInterval("")).toBe(false);
    expect(isPositiveMinuteInterval("1.5")).toBe(false);
  });

  it("propagates 409 when the aging transition already exists", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "exists" }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      createWorkflowAgingTransition("Nightly QA", {
        from: "Draft",
        to: "Review",
        intervalMinutes: 15,
      }),
    ).rejects.toMatchObject({ status: 409 });
  });

  it("PUTs a wrapped aging interval change and parses the new minutes", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [{ name: "Draft" }, { name: "Review" }],
          edges: [
            { from: "Draft", to: "Review", label: "Aging 30", aging: true, intervalMinutes: 30 },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await updateWorkflowAgingInterval("Nightly QA", {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
      newIntervalMinutes: 30,
    });
    expect(graph.edges?.[0]?.intervalMinutes).toBe(30);
    expect(graph.edges?.[0]?.label).toBe("Aging 30");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/aging-transitions/interval");
    expect(String(init.body)).toContain("WorkflowAgingIntervalWrite");
    expect(String(init.body)).toContain("newIntervalMinutes");
    expect(String(init.body)).toContain("30");
  });

  it("propagates 400 and 409 when an aging interval change is rejected", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "bad" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      updateWorkflowAgingInterval("Nightly QA", {
        from: "Draft",
        to: "Review",
        intervalMinutes: 15,
        newIntervalMinutes: 0,
      }),
    ).rejects.toMatchObject({ status: 400 });
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "exists" }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      updateWorkflowAgingInterval("Nightly QA", {
        from: "Draft",
        to: "Review",
        intervalMinutes: 15,
        newIntervalMinutes: 45,
      }),
    ).rejects.toMatchObject({ status: 409 });
  });

  it("DELETEs one absolute aging transition and parses the graph without that edge", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [{ name: "Draft" }, { name: "Review" }],
          edges: [{ from: "Draft", to: "Review", label: "Submit", aging: false }],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await deleteWorkflowAgingTransition("Nightly QA", "Draft", "Review", 15);
    expect(graph.edges?.[0]?.label).toBe("Submit");
    expect(graph.edges?.[0]?.aging).toBe(false);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/aging-transitions?");
    expect(url).toContain("from=Draft");
    expect(url).toContain("to=Review");
    expect(url).toContain("intervalMinutes=15");
    expect(url).not.toContain("/transitions?");
  });

  it("propagates 400 and 409 when an aging delete is rejected", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "bad" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      deleteWorkflowAgingTransition("Nightly QA", "Draft", "Review", 0),
    ).rejects.toMatchObject({ status: 400 });
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "repeated" }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      deleteWorkflowAgingTransition("Nightly QA", "Draft", "Review", 15),
    ).rejects.toMatchObject({ status: 409 });
  });

  it("propagates 400 when the label is invalid", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "invalid" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      updateWorkflowTransition("Nightly QA", "Draft", "Submit", "Review", {
        to: "Review",
        label: "Bad!",
      }),
    ).rejects.toMatchObject({ status: 400 });
  });
});

describe("workflow step delete API (slice 34)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("builds a DELETE path for one step", () => {
    const path = workflowStepDeletePath("Nightly QA", "Orphan Step");
    expect(path).toBe(
      `${PATHS.WORKFLOWS_ASSOC}/${encodeURIComponent("Nightly QA")}/steps/${encodeURIComponent("Orphan Step")}`,
    );
  });

  it("DELETEs an unreferenced step and parses the graph", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          workflowName: "Nightly QA",
          packaged: false,
          nodes: [{ name: "Draft" }],
          edges: [],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const graph = await deleteWorkflowStep("Nightly QA", "Orphan");
    expect(graph.nodes).toEqual([{ name: "Draft" }]);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/steps/Orphan");
  });

  it("propagates 409 when the step is still referenced", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ error: "still referenced" }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(deleteWorkflowStep("Nightly QA", "Draft")).rejects.toMatchObject({ status: 409 });
  });
});

describe("parseWorkflowGraph", () => {
  it("unwraps WorkflowGraph and defaults missing lists", () => {
    expect(
      parseWorkflowGraph({
        WorkflowGraph: {
          workflowName: "Simple Workflow",
          packaged: true,
          nodes: [{ name: "Draft" }],
        },
      }),
    ).toEqual({
      workflowName: "Simple Workflow",
      packaged: true,
      defaultWorkflow: false,
      nodes: [{ name: "Draft" }],
      edges: [],
    });
  });

  it("reads a bare graph object", () => {
    const graph = parseWorkflowGraph({
      workflowName: "Nightly QA",
      packaged: false,
      defaultWorkflow: false,
      nodes: [],
      edges: [{ from: "A", to: "B", label: "Go" }],
    });
    expect(graph.edges?.[0]?.label).toBe("Go");
    expect(graph.packaged).toBe(false);
  });
});

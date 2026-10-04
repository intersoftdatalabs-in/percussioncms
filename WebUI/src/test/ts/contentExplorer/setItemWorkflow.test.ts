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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it, vi } from "vitest";
import type { ApiError } from "../../../main/ts/api/client";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

function httpError(status: number): ApiError {
  return { status, statusText: String(status), body: null };
}
import {
  classifySetWorkflowSelection,
  describeSetWorkflowMultiSave,
  gateExplorerWorkflowChange,
  loadSetWorkflowCatalog,
  loadSetWorkflowMultiCatalog,
  planSetWorkflowMulti,
  saveSetWorkflow,
  saveSetWorkflowOnSelection,
} from "../../../main/ts/contentExplorer/setItemWorkflow";

const page: PSPathItem = {
  id: "16777215-101-551",
  name: "Home",
  path: "/Sites/CI/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

const folder: PSPathItem = {
  id: "1",
  name: "CI",
  path: "/Sites/CI/",
  type: "folder",
  category: "folder",
  leaf: false,
};

describe("set item workflow (#5076)", () => {
  it("blocks empty, folder, and multi-select before any save", () => {
    expect(classifySetWorkflowSelection({ item: null, selectedCount: 0 }).status).toBe(
      "blocked",
    );
    expect(classifySetWorkflowSelection({ item: folder, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "folder",
      name: "CI",
    });
    expect(classifySetWorkflowSelection({ item: page, selectedCount: 2 })).toMatchObject({
      reason: "multi",
    });
  });

  it("refuses the current workflow and an id that is not listed", () => {
    expect(
      gateExplorerWorkflowChange({
        selectedId: "4",
        currentId: "4",
        allowedIds: ["4", "7"],
      }),
    ).toEqual({ ok: false, reason: "unchanged" });
    expect(
      gateExplorerWorkflowChange({
        selectedId: "9",
        currentId: "4",
        allowedIds: ["4", "7"],
      }),
    ).toEqual({ ok: false, reason: "forbidden" });
    expect(
      gateExplorerWorkflowChange({
        selectedId: "7",
        currentId: "4",
        allowedIds: ["4", "7"],
      }),
    ).toEqual({ ok: true, workflowId: "7" });
  });

  it("does not call change when the catalog is forbidden", async () => {
    const change = vi.fn();
    const loaded = await loadSetWorkflowCatalog({
      item: page,
      selectedCount: 1,
      loadChoices: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(change).not.toHaveBeenCalled();
  });

  it("saves only a different allowed workflow", async () => {
    const change = vi.fn().mockResolvedValue({ workflowId: "7" });
    const saved = await saveSetWorkflow({
      itemId: page.id as string,
      selectedId: "7",
      currentId: "4",
      allowedIds: ["4", "7"],
      change,
    });
    expect(saved).toEqual({ status: "saved", workflowId: "7" });
    expect(change).toHaveBeenCalledWith(page.id, "7");
  });

  it("maps HTTP 409 to a failure and does not report saved", async () => {
    const change = vi.fn().mockRejectedValue(httpError(409));
    const saved = await saveSetWorkflow({
      itemId: "551",
      selectedId: "7",
      currentId: "4",
      allowedIds: ["4", "7"],
      change,
    });
    expect(saved).toEqual({ status: "http", http: 409 });
  });
});

const about: PSPathItem = {
  id: "16777215-101-552",
  name: "About",
  path: "/Sites/CI/About",
  type: "percPage",
  category: "page",
  leaf: true,
};

const choices = [
  { id: "4", name: "Simple" },
  { id: "7", name: "Local workflow" },
];

function catalogFor(currentId: string) {
  return async () => ({
    currentWorkflowId: currentId,
    choices,
  });
}

describe("set workflow on multi-selected items (#5155)", () => {
  it("still classifies a multi-count as multi for single-item callers (#5076)", () => {
    expect(classifySetWorkflowSelection({ item: page, selectedCount: 2 })).toMatchObject({
      status: "blocked",
      reason: "multi",
    });
  });

  it("plans pages and assets and does not write folders", () => {
    const plan = planSetWorkflowMulti([page, folder, about, page]);
    expect(plan).toEqual({
      status: "ready",
      targets: [
        { itemId: page.id, name: "Home" },
        { itemId: about.id, name: "About" },
      ],
      skippedFolderNames: ["CI"],
    });
    expect(planSetWorkflowMulti([folder, folder])).toMatchObject({
      status: "blocked",
      reason: "folder",
      name: "CI",
    });
    expect(planSetWorkflowMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const change = vi.fn();
    const loaded = await loadSetWorkflowMultiCatalog({
      items: [page, about, folder],
      loadChoices: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(change).not.toHaveBeenCalled();
  });

  it("shows each workflow only after that save and does not claim success on HTTP 409", async () => {
    const order: string[] = [];
    let releaseAbout: (value?: unknown) => void = () => undefined;
    const aboutGate = new Promise((resolve) => {
      releaseAbout = resolve;
    });
    const change = vi.fn(async (itemId: string) => {
      order.push(`post:${itemId}`);
      if (itemId === about.id) {
        await aboutGate;
        throw httpError(409);
      }
      return { workflowId: "7" };
    });
    const pending = saveSetWorkflowOnSelection({
      targets: [
        { itemId: String(page.id), name: "Home" },
        { itemId: String(about.id), name: "About" },
      ],
      skippedFolderNames: ["CI"],
      selectedId: "7",
      allowedIds: ["4", "7"],
      loadChoices: async (itemId) => {
        order.push(`get:${itemId}`);
        return {
          currentWorkflowId: "4",
          choices,
        };
      },
      change,
      onItemSaved: (item) => {
        order.push(`shown:${item.itemId}`);
      },
    });
    await vi.waitFor(() => expect(order).toContain(`post:${about.id}`));
    expect(order).toEqual([
      `get:${page.id}`,
      `post:${page.id}`,
      `shown:${page.id}`,
      `get:${about.id}`,
      `post:${about.id}`,
    ]);
    releaseAbout();
    const result = await pending;
    expect(result.status).toBe("partial");
    expect(result.workflowId).toBe("");
    expect(result.saved.map((row) => row.itemId)).toEqual([page.id]);
    expect(result.failures).toEqual([
      { itemId: about.id, name: "About", http: 409 },
    ]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([
      `shown:${page.id}`,
    ]);
    const described = describeSetWorkflowMultiSave(result, "Local workflow");
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.workflowId).toBe("");
    expect(described.workflowName).toBe("");
    expect(described.text).toContain("Not every selected item had its workflow set");
    expect(described.text).toContain("About (HTTP 409)");
    expect(described.text).toContain("Folders are not assigned a workflow: CI");
    expect(described.text).not.toContain("Workflow saved");
  });

  it("claims success only after every page and asset is saved", async () => {
    const change = vi.fn().mockResolvedValue({ workflowId: "7" });
    const shown: string[] = [];
    const result = await saveSetWorkflowOnSelection({
      targets: [
        { itemId: "42", name: "Home" },
        { itemId: "43", name: "About" },
      ],
      skippedFolderNames: ["News"],
      selectedId: "7",
      allowedIds: ["4", "7"],
      loadChoices: catalogFor("4"),
      change,
      onItemSaved: (item) => shown.push(item.itemId),
    });
    expect(change).toHaveBeenCalledTimes(2);
    expect(change).toHaveBeenNthCalledWith(1, "42", "7");
    expect(change).toHaveBeenNthCalledWith(2, "43", "7");
    expect(shown).toEqual(["42", "43"]);
    expect(result.status).toBe("saved");
    const described = describeSetWorkflowMultiSave(result, "Local workflow");
    expect(described).toMatchObject({
      kind: "success",
      reason: "folders-skipped",
      workflowId: "7",
      workflowName: "Local workflow",
    });
    expect(described.text).toContain("Workflow saved Local workflow");
    expect(described.text).toContain("News");
  });

  it.each([400, 403, 409])(
    "HTTP %s on one item is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetWorkflowOnSelection({
        targets: [
          { itemId: "42", name: "Home" },
          { itemId: "43", name: "About" },
        ],
        selectedId: "7",
        allowedIds: ["4", "7"],
        loadChoices: catalogFor("4"),
        change: vi.fn(async (itemId: string) => {
          if (itemId === "43") {
            throw httpError(status);
          }
          return { workflowId: "7" };
        }),
        onItemSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.itemId)).toEqual(["42"]);
      expect(result.failures).toEqual([{ itemId: "43", name: "About", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetWorkflowMultiSave(result, "Local workflow").text).not.toContain(
        "Workflow saved",
      );
    },
  );

  it("does not post when the chosen workflow is already current", async () => {
    const change = vi.fn();
    const shown = vi.fn();
    const result = await saveSetWorkflowOnSelection({
      targets: [{ itemId: "42", name: "Home" }],
      selectedId: "4",
      allowedIds: ["4", "7"],
      loadChoices: catalogFor("4"),
      change,
      onItemSaved: shown,
    });
    expect(change).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetWorkflowMultiSave(result, "Simple");
    expect(described.kind).toBe("error");
    expect(described.workflowId).toBe("");
    expect(described.text).not.toContain("Workflow saved");
  });
});

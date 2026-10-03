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
  gateExplorerWorkflowChange,
  loadSetWorkflowCatalog,
  saveSetWorkflow,
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

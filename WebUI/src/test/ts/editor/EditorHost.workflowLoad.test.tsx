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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import type {
  ItemStateTransition,
  ItemWorkflowChoices,
} from "../../../main/ts/api/contentExplorer/itemWorkflowApi";
import { EditorHost } from "../../../main/ts/editor/EditorHost";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  revision: 2,
  fields: [
    { name: "sys_title", value: "Home" },
    { name: "displaytitle", value: "Welcome" },
  ],
};

const transitions: ItemStateTransition = {
  stateName: "Draft",
  transitionTriggers: ["Approve"],
};

const choices: ItemWorkflowChoices = {
  currentWorkflowId: "4",
  choices: [{ id: "4", name: "Local" }],
};

function renderHost({
  loadFields,
  loadTransitions,
  loadWorkflowChoices,
}: {
  loadFields?: (id: string) => Promise<ItemEditorFields>;
  loadTransitions?: (id: string) => Promise<ItemStateTransition>;
  loadWorkflowChoices?: (id: string) => Promise<ItemWorkflowChoices>;
}) {
  render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue({
                checkOutUser: "admin",
                currentUser: "admin",
              })}
              loadFields={loadFields ?? vi.fn().mockResolvedValue(fields)}
              loadType={async () => ({ fields: [] })}
              loadTransitions={
                loadTransitions ?? (async () => ({ ...transitions }))
              }
              loadWorkflowChoices={
                loadWorkflowChoices ?? (async () => ({ ...choices }))
              }
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function openWorkflow(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-workflow")).toBeTruthy();
  });
}

describe("EditorHost workflow-transition panel load notice (#5769)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("a transition load failure shows a panel notice with retry, distinct from the empty state", async () => {
    const loadTransitions = vi
      .fn()
      .mockRejectedValueOnce({ status: 500, message: "boom" })
      .mockResolvedValue({ ...transitions });
    renderHost({ loadTransitions });
    await openWorkflow();
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-load-error")).toBeTruthy();
    });
    // The notice is a panel-level alert, not the empty-allowlist hint, and
    // no trigger is claimed while the list failed to load.
    expect(
      screen.getByTestId("editor-workflow-load-error").textContent,
    ).toMatch(/could not load the workflow/i);
    expect(screen.getByTestId("editor-retry-workflow-load")).toBeTruthy();
    expect(screen.queryByTestId("editor-workflow-empty")).toBeNull();
    expect(screen.queryByTestId("editor-workflow-trigger-Approve")).toBeNull();
    // Retry re-runs the transition load for the same contentId; success
    // renders the triggers and clears the notice.
    fireEvent.click(screen.getByTestId("editor-retry-workflow-load"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-workflow-trigger-Approve"),
      ).toBeTruthy();
    });
    expect(loadTransitions).toHaveBeenLastCalledWith("42");
    expect(screen.queryByTestId("editor-workflow-load-error")).toBeNull();
    expect(screen.getByTestId("editor-workflow-state").textContent).toMatch(
      /Draft/,
    );
  });

  it("a repeated retry failure keeps the notice without claiming a load", async () => {
    const loadTransitions = vi
      .fn()
      .mockRejectedValue({ status: 500, message: "boom" });
    renderHost({ loadTransitions });
    await openWorkflow();
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-load-error")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-retry-workflow-load"));
    await waitFor(() => {
      expect(loadTransitions.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
    expect(screen.getByTestId("editor-workflow-load-error")).toBeTruthy();
    expect(screen.queryByTestId("editor-workflow-trigger-Approve")).toBeNull();
    expect(screen.queryByTestId("editor-workflow-empty")).toBeNull();
  });

  it("a choices-only failure shows the same notice while loaded triggers still render", async () => {
    const loadWorkflowChoices = vi
      .fn()
      .mockRejectedValueOnce({ status: 500, message: "catalog boom" })
      .mockResolvedValue({ ...choices });
    renderHost({ loadWorkflowChoices });
    await openWorkflow();
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-load-error")).toBeTruthy();
    });
    // The transitions themselves loaded, so their triggers render alongside
    // the catalog notice; retry clears it.
    expect(
      screen.getByTestId("editor-workflow-trigger-Approve"),
    ).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-retry-workflow-load"));
    await waitFor(() => {
      expect(screen.queryByTestId("editor-workflow-load-error")).toBeNull();
    });
    expect(loadWorkflowChoices).toHaveBeenLastCalledWith("42");
    expect(
      screen.getByTestId("editor-workflow-trigger-Approve"),
    ).toBeTruthy();
  });

  it("an empty allowlist still shows the empty hint with no load notice", async () => {
    renderHost({
      loadTransitions: async () => ({ transitionTriggers: [] }),
      loadWorkflowChoices: async () => ({ choices: [] }),
    });
    await openWorkflow();
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-workflow-load-error")).toBeNull();
    expect(screen.queryByTestId("editor-retry-workflow-load")).toBeNull();
  });
});

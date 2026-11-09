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
import type { ItemRevisionsSummary } from "../../../main/ts/api/contentExplorer/itemRevisionsApi";
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

const historySummary: ItemRevisionsSummary = {
  restorable: false,
  revisions: [],
  comments: [
    {
      comment: "Looks good",
      commenter: "admin",
      commentType: "Approve",
      commentDate: "2026-04-12",
    },
    {
      comment: "",
      commenter: "",
      commentType: "",
      commentDate: "",
    },
  ],
};

function renderHost({
  mode = "edit",
  loadRevisions,
}: {
  mode?: string;
  loadRevisions?: (id: string) => Promise<ItemRevisionsSummary>;
}) {
  render(
    <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue({
                checkOutUser: "admin",
                currentUser: "admin",
              })}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={async () => ({ fields: [] })}
              loadTransitions={async () => ({ ...transitions })}
              loadWorkflowChoices={async () => ({ ...choices })}
              loadRevisions={loadRevisions}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EditorHost workflow history panel (#5829)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("opens the history from the toggle and lists recorded transitions", async () => {
    const loadRevisions = vi.fn().mockResolvedValue(historySummary);
    renderHost({ loadRevisions });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-history-toggle")).toBeTruthy();
    });
    // Collapsed by default: no fetch fires and no panel renders.
    expect(loadRevisions).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-workflow-history")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-workflow-history-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-history-table")).toBeTruthy();
    });
    expect(loadRevisions).toHaveBeenCalledWith("42");
    // Only the recorded row renders; the fully blank row is dropped.
    expect(
      screen.getByTestId("editor-workflow-history-row-0"),
    ).toBeTruthy();
    expect(
      screen.queryByTestId("editor-workflow-history-row-1"),
    ).toBeNull();
    expect(
      screen.getByTestId("editor-workflow-history-user-0").textContent,
    ).toBe("admin");
    expect(
      screen.getByTestId("editor-workflow-history-type-0").textContent,
    ).toBe("Approve");
    expect(
      screen.getByTestId("editor-workflow-history-comment-0").textContent,
    ).toBe("Looks good");
    // Toggling again hides the panel without refetching.
    fireEvent.click(screen.getByTestId("editor-workflow-history-toggle"));
    await waitFor(() => {
      expect(screen.queryByTestId("editor-workflow-history")).toBeNull();
    });
    expect(loadRevisions).toHaveBeenCalledTimes(1);
  });

  it("shows an empty state when no comments are recorded", async () => {
    const loadRevisions = vi
      .fn()
      .mockResolvedValue({ restorable: false, revisions: [], comments: [] });
    renderHost({ loadRevisions });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-history-toggle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-workflow-history-toggle"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-workflow-history-empty"),
      ).toBeTruthy();
    });
    expect(
      screen.queryByTestId("editor-workflow-history-table"),
    ).toBeNull();
  });

  it("a history load failure shows an error, not an empty history", async () => {
    const loadRevisions = vi
      .fn()
      .mockRejectedValue({ status: 403, message: "forbidden" });
    renderHost({ loadRevisions });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-history-toggle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-workflow-history-toggle"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-workflow-history-load-error"),
      ).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-workflow-history-load-error").textContent,
    ).toMatch(/could not load the workflow history/i);
    expect(
      screen.queryByTestId("editor-workflow-history-empty"),
    ).toBeNull();
    expect(
      screen.queryByTestId("editor-workflow-history-table"),
    ).toBeNull();
  });

  it("shows the toggle in view mode (read-only history)", async () => {
    const loadRevisions = vi.fn().mockResolvedValue(historySummary);
    renderHost({ mode: "view", loadRevisions });
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-history-toggle")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-workflow-history-toggle"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-history-table")).toBeTruthy();
    });
    expect(loadRevisions).toHaveBeenCalledWith("42");
  });

  it("hides the toggle in promote mode", async () => {
    renderHost({
      mode: "promote",
      loadRevisions: vi.fn().mockResolvedValue(historySummary),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-promote-form")).toBeTruthy();
    });
    expect(
      screen.queryByTestId("editor-workflow-history-toggle"),
    ).toBeNull();
  });
});

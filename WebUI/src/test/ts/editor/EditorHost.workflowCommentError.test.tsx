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
  fields: [{ name: "sys_title", value: "Home" }],
};

const transitions: ItemStateTransition = {
  stateName: "Draft",
  transitionTriggers: ["Submit", "Reject"],
};

const choices: ItemWorkflowChoices = {
  currentWorkflowId: "4",
  choices: [{ id: "4", name: "Local" }],
};

function renderHost({
  runTransition,
}: {
  runTransition?: (
    itemId: string,
    trigger: string,
    comment?: string,
    adhocAssignees?: readonly string[],
  ) => Promise<unknown>;
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
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={async () => ({ fields: [] })}
              loadTransitions={async () => ({ ...transitions })}
              loadWorkflowChoices={async () => ({ ...choices })}
              runTransition={
                runTransition ?? vi.fn().mockResolvedValue({ itemId: "42" })
              }
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function openReject(): Promise<void> {
  await waitFor(() => {
    expect(
      screen.getByTestId("editor-workflow-trigger-Reject"),
    ).toBeTruthy();
  });
}

function commentBox(): HTMLElement {
  return screen.getByTestId("editor-workflow-comment");
}

describe("EditorHost workflow comment error association (#6931)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("a comment-gate block marks the comment box invalid and describes it by the inline error", async () => {
    const runTransition = vi.fn().mockResolvedValue({ itemId: "42" });
    renderHost({ runTransition });
    await openReject();
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Reject"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-error")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-workflow-error").textContent).toMatch(
      /enter a comment/i,
    );
    expect(runTransition).not.toHaveBeenCalled();
    // The invalid box names the error; the error carries that id so
    // assistive tech announces where the fix goes.
    expect(commentBox().getAttribute("aria-invalid")).toBe("true");
    expect(commentBox().getAttribute("aria-describedby")).toBe(
      "editor-workflow-comment-error",
    );
    expect(
      screen.getByTestId("editor-workflow-error").getAttribute("id"),
    ).toBe("editor-workflow-comment-error");
    // Keyboard users land in the comment box where the fix happens.
    expect(document.activeElement).toBe(commentBox());
  });

  it("a successful transition clears the invalid marking with the error", async () => {
    const runTransition = vi.fn().mockResolvedValue({ itemId: "42" });
    renderHost({ runTransition });
    await openReject();
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Reject"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-error")).toBeTruthy();
    });
    expect(commentBox().getAttribute("aria-invalid")).toBe("true");
    fireEvent.change(commentBox(), { target: { value: "needs work" } });
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Reject"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-done")).toBeTruthy();
    });
    expect(runTransition).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("editor-workflow-error")).toBeNull();
    expect(commentBox().getAttribute("aria-invalid")).toBeNull();
    expect(commentBox().getAttribute("aria-describedby")).toBeNull();
  });

  it("a transport failure does not mark the comment box invalid", async () => {
    const runTransition = vi
      .fn()
      .mockRejectedValue({ status: 403, message: "nope" });
    renderHost({ runTransition });
    await openReject();
    fireEvent.change(commentBox(), { target: { value: "needs work" } });
    fireEvent.click(screen.getByTestId("editor-workflow-trigger-Submit"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-workflow-error")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-workflow-error").textContent).toMatch(
      /not allowed/i,
    );
    // A 403 is about permission, not the comment: leave the box unmarked.
    expect(commentBox().getAttribute("aria-invalid")).toBeNull();
    expect(commentBox().getAttribute("aria-describedby")).toBeNull();
    expect(
      screen.getByTestId("editor-workflow-error").getAttribute("id"),
    ).toBeNull();
  });
});

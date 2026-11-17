/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software,
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
import type { ItemStateTransition } from "../../../main/ts/api/contentExplorer/itemWorkflowApi";
import { EditorHost } from "../../../main/ts/editor/EditorHost";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

const fields: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  fields: [{ name: "sys_title", value: "Home" }],
};

function transitionsFor(triggers: string[]): ItemStateTransition {
  return { stateName: "Draft", transitionTriggers: triggers };
}

function renderHost(
  extra: Partial<React.ComponentProps<typeof EditorHost>> = {},
  mode = "edit",
) {
  return render(
    <MemoryRouter initialEntries={[`/editor?contentId=42&mode=${mode}`]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={vi.fn().mockResolvedValue(fields)}
              loadType={async () => ({
                fields: [{ name: "sys_title", label: "Title", readOnly: false }],
              })}
              loadTransitions={vi
                .fn()
                .mockResolvedValue(transitionsFor(["Submit", "Approve", "Reject"]))}
              {...extra}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

async function openReject(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("editor-reject-quick")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("editor-reject-quick"));
  await waitFor(() => {
    expect(screen.getByTestId("editor-approve-reject-dialog")).toBeTruthy();
  });
}

describe("EditorHost quick Approve/Reject bar (#6483)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows Approve and Reject only when the allowlist names them", async () => {
    renderHost({
      loadTransitions: vi
        .fn()
        .mockResolvedValue(transitionsFor(["Submit", "Approve"])),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-bar")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-approve-quick")).toBeTruthy();
    expect(screen.queryByTestId("editor-reject-quick")).toBeNull();
  });

  it("hides the bar when the allowlist names neither", async () => {
    renderHost({
      loadTransitions: vi.fn().mockResolvedValue(transitionsFor(["Submit"])),
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-approve-reject-bar")).toBeNull();
    expect(screen.queryByTestId("editor-approve-quick")).toBeNull();
    expect(screen.queryByTestId("editor-reject-quick")).toBeNull();
  });

  it("hides the bar in view mode", async () => {
    renderHost({}, "view");
    await waitFor(() => {
      expect(screen.getByTestId("editor-host")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-approve-reject-bar")).toBeNull();
  });

  it("reject blocks a blank comment before any server call", async () => {
    const runTransition = vi.fn(async () => undefined);
    renderHost({ runTransition });
    await openReject();
    expect(
      screen.getByTestId("editor-approve-reject-comment"),
    ).toBeTruthy();
    expect(screen.getByTestId("editor-approve-reject-required")).toBeTruthy();
    expect(
      (screen.getByTestId("editor-approve-reject-confirm") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    fireEvent.click(screen.getByTestId("editor-approve-reject-confirm"));
    expect(runTransition).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-approve-reject-done")).toBeNull();
  });

  it("reject with a comment runs the trigger and reports success", async () => {
    const runTransition = vi.fn(async () => undefined);
    const loadTransitions = vi
      .fn()
      .mockResolvedValueOnce(transitionsFor(["Submit", "Approve", "Reject"]))
      .mockResolvedValue(transitionsFor(["Submit"]));
    renderHost({ runTransition, loadTransitions });
    await openReject();
    fireEvent.change(screen.getByTestId("editor-approve-reject-comment"), {
      target: { value: "Needs rework" },
    });
    fireEvent.click(screen.getByTestId("editor-approve-reject-confirm"));
    await waitFor(() => {
      expect(runTransition).toHaveBeenCalledWith(
        "42",
        "Reject",
        "Needs rework",
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-done")).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-approve-reject-done").textContent,
    ).toMatch(/rejected/i);
    expect(
      screen.queryByTestId("editor-approve-reject-dialog"),
    ).toBeNull();
  });

  it("approve runs with an empty comment when the server does not require one", async () => {
    const runTransition = vi.fn(async () => undefined);
    renderHost({ runTransition });
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-quick")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-approve-quick"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-dialog")).toBeTruthy();
    });
    expect(
      screen.queryByTestId("editor-approve-reject-required"),
    ).toBeNull();
    fireEvent.click(screen.getByTestId("editor-approve-reject-confirm"));
    await waitFor(() => {
      expect(runTransition).toHaveBeenCalledWith("42", "Approve", undefined);
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-done")).toBeTruthy();
    });
  });

  it("blocks a blank approve when the server marks Approve comment-required", async () => {
    const runTransition = vi.fn(async () => undefined);
    renderHost({ runTransition, commentRequiredTriggers: ["Approve"] });
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-quick")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-approve-quick"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-dialog")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-approve-reject-required")).toBeTruthy();
    expect(
      (screen.getByTestId("editor-approve-reject-confirm") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    fireEvent.click(screen.getByTestId("editor-approve-reject-confirm"));
    expect(runTransition).not.toHaveBeenCalled();
  });

  it("cancel does not call the server", async () => {
    const runTransition = vi.fn(async () => undefined);
    renderHost({ runTransition });
    await openReject();
    fireEvent.click(screen.getByTestId("editor-approve-reject-cancel"));
    expect(runTransition).not.toHaveBeenCalled();
    expect(
      screen.queryByTestId("editor-approve-reject-dialog"),
    ).toBeNull();
    expect(screen.queryByTestId("editor-approve-reject-done")).toBeNull();
  });

  it("a 409 stays on the dialog and claims no success", async () => {
    const runTransition = vi
      .fn()
      .mockRejectedValue({ status: 409, message: "state rejects it" });
    renderHost({ runTransition });
    await openReject();
    fireEvent.change(screen.getByTestId("editor-approve-reject-comment"), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByTestId("editor-approve-reject-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-error")).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-approve-reject-error").textContent,
    ).toMatch(/409/);
    expect(
      screen.getByTestId("editor-approve-reject-dialog"),
    ).toBeTruthy();
    expect(screen.queryByTestId("editor-approve-reject-done")).toBeNull();
  });

  it("a NUL comment never reaches the server", async () => {
    const runTransition = vi.fn(async () => undefined);
    renderHost({ runTransition });
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-quick")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-approve-quick"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-dialog")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-approve-reject-comment"), {
      target: { value: "bad\u0000comment" },
    });
    fireEvent.click(screen.getByTestId("editor-approve-reject-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-approve-reject-error")).toBeTruthy();
    });
    expect(runTransition).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-approve-reject-done")).toBeNull();
  });
});

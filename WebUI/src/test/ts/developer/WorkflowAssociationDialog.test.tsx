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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { describe, expect, it, vi } from "vitest";
import type { WorkflowDef } from "../../../main/ts/api/developer/types";
import { WorkflowAssociationDialog } from "../../../main/ts/developer/WorkflowAssociationDialog";

const CATALOG: WorkflowDef[] = [
  { workflowName: "Default Workflow", workflowDescription: "Ships with the CMS" },
  { workflowName: "perc.review", workflowDescription: "Editorial review" },
];

function renderDialog(overrides: {
  associatedNames?: string[];
  loadWorkflows?: () => Promise<WorkflowDef[]>;
  onAssociate?: (name: string, def: WorkflowDef) => void;
  onCancel?: () => void;
} = {}) {
  const onAssociate = overrides.onAssociate ?? vi.fn();
  const onCancel = overrides.onCancel ?? vi.fn();
  render(
    <WorkflowAssociationDialog
      open
      associatedNames={overrides.associatedNames ?? []}
      loadWorkflows={overrides.loadWorkflows ?? (() => Promise.resolve(CATALOG))}
      onAssociate={onAssociate}
      onCancel={onCancel}
    />,
  );
  return { onAssociate, onCancel };
}

describe("WorkflowAssociationDialog (#6062 / FR §5.5)", () => {
  // Load-hardening for #6837 (sibling of the #6706/#6742 slotCreate timing
  // flake): under full-suite CPU contention the catalog load + passive
  // effects (Escape listener, focus trap) can exceed the 1s waitFor default,
  // so every async chain below carries an explicit 10s budget (inside the
  // 20s vitest testTimeout). No assertion is weakened.
  const WF_ASSOC_WAIT_MS = 10_000;

  // Click-after-ready for #6837: the confirm button stays disabled until a
  // non-duplicate row is picked (handleAssociate no-ops on null selection),
  // so a confirm click issued before the pick state flushes silently stages
  // nothing. Gate the click on the button being enabled.
  async function clickConfirmWhenReady() {
    await waitFor(
      () => {
        expect(
          (
            screen.getByTestId(
              "developer-wf-assoc-confirm",
            ) as HTMLButtonElement
          ).disabled,
        ).toBe(false);
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("developer-wf-assoc-confirm"));
  }

  it("associates the picked workflow on confirm", async () => {
    const { onAssociate, onCancel } = renderDialog();
    await waitFor(
      () => {
        expect(
          screen.getByTestId("developer-wf-assoc-row-perc.review"),
        ).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("developer-wf-assoc-pick-perc.review"));
    await clickConfirmWhenReady();
    expect(onAssociate).toHaveBeenCalledTimes(1);
    expect(onAssociate).toHaveBeenCalledWith("perc.review", CATALOG[1]);
    expect(onCancel).not.toHaveBeenCalled();
  });

  it("cancel stages nothing (edges unchanged)", async () => {
    const { onAssociate, onCancel } = renderDialog();
    await waitFor(
      () => {
        expect(
          screen.getByTestId("developer-wf-assoc-row-Default Workflow"),
        ).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("developer-wf-assoc-pick-Default Workflow"));
    fireEvent.click(screen.getByTestId("developer-wf-assoc-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onAssociate).not.toHaveBeenCalled();
  });

  it("Escape cancels without associating", async () => {
    const { onAssociate, onCancel } = renderDialog();
    await waitFor(
      () => {
        expect(
          screen.getByTestId("developer-wf-assoc-row-perc.review"),
        ).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    // De-flake for #6706 (sibling of the slotCreate timing flake): the
    // Escape listener attaches in a passive effect after the rows render,
    // so a synchronous keyDown under full-suite load can dispatch before
    // the listener exists and onCancel stays at 0. Dispatch inside the
    // waitFor so a missed dispatch retries; the loop stops at the first
    // pass, so exactly-once still pins a single cancel.
    await waitFor(
      () => {
        fireEvent.keyDown(window, { key: "Escape" });
        expect(onCancel).toHaveBeenCalledTimes(1);
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    expect(onAssociate).not.toHaveBeenCalled();
  });

  it("search narrows the catalog rows", async () => {
    renderDialog();
    await waitFor(
      () => {
        expect(
          screen.getByTestId("developer-wf-assoc-row-perc.review"),
        ).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.change(screen.getByTestId("developer-wf-assoc-search"), {
      target: { value: "review" },
    });
    // Filtered rows derive from sync useMemo state, but assert inside a
    // bounded wait so a slow effect flush under full-suite load retries
    // instead of failing the narrowed assertions outright.
    await waitFor(
      () => {
        expect(
          screen.queryByTestId("developer-wf-assoc-row-Default Workflow"),
        ).toBeNull();
        expect(
          screen.getByTestId("developer-wf-assoc-row-perc.review"),
        ).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
  });

  it("blocks confirm for an already-associated workflow", async () => {
    const { onAssociate } = renderDialog({ associatedNames: ["perc.review"] });
    await waitFor(
      () => {
        expect(
          screen.getByTestId("developer-wf-assoc-row-perc.review"),
        ).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("developer-wf-assoc-pick-perc.review"));
    // The duplicate hint derives from pick state flushed through effects;
    // wait for it instead of asserting synchronously after the click.
    await waitFor(
      () => {
        expect(screen.getByTestId("developer-wf-assoc-dup")).toBeTruthy();
        expect(
          (
            screen.getByTestId(
              "developer-wf-assoc-confirm",
            ) as HTMLButtonElement
          ).disabled,
        ).toBe(true);
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("developer-wf-assoc-confirm"));
    expect(onAssociate).not.toHaveBeenCalled();
  });

  it("shows the load error and still cancels cleanly", async () => {
    const loadWorkflows = vi.fn(() => Promise.reject(new Error("boom")));
    const { onAssociate, onCancel } = renderDialog({ loadWorkflows });
    await waitFor(
      () => {
        expect(screen.getByTestId("developer-wf-assoc-error")).toBeTruthy();
      },
      { timeout: WF_ASSOC_WAIT_MS },
    );
    fireEvent.click(screen.getByTestId("developer-wf-assoc-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onAssociate).not.toHaveBeenCalled();
  });

  it("renders nothing when closed without loading the catalog", () => {
    const loadWorkflows = vi.fn(() => Promise.resolve(CATALOG));
    render(
      <WorkflowAssociationDialog
        open={false}
        associatedNames={[]}
        loadWorkflows={loadWorkflows}
        onAssociate={() => undefined}
        onCancel={() => undefined}
      />,
    );
    expect(screen.queryByTestId("developer-wf-assoc-dialog")).toBeNull();
    expect(loadWorkflows).not.toHaveBeenCalled();
  });
});

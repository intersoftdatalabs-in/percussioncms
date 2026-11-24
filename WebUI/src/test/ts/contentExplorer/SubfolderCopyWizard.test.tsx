/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { PSCopyRequest } from "../../../main/ts/api/contentExplorer/types";
import { SubfolderCopyWizard } from "../../../main/ts/contentExplorer/wizards/SubfolderCopyWizard";
import { renderA11yGate } from "./a11y";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const SUBFOLDER_COPY_WIZARD_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const SUBFOLDER_COPY_WIZARD_TEST_TIMEOUT = 30_000;

describe("SubfolderCopyWizard", () => {
  it("renders at step 0 (source) with source field", () => {
    render(<SubfolderCopyWizard />);
    expect(screen.getByTestId("subfolder-copy-wizard")).toBeTruthy();
    expect(screen.getByTestId("subfolder-copy-step-source")).toBeTruthy();
  });

  it("Next is disabled at step 0 with empty source", () => {
    render(<SubfolderCopyWizard />);
    const next = screen.getByTestId("subfolder-copy-next") as HTMLButtonElement;
    expect(next.disabled).toBe(true);
  });

  it("full flow advances and submits via the supplied transport", { timeout: SUBFOLDER_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    render(<SubfolderCopyWizard submit={submit} />);
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.change(screen.getByTestId("subfolder-copy-target"), {
      target: { value: "/Sites/B" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    expect(screen.getByTestId("subfolder-copy-step-confirm")).toBeTruthy();
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    },
      { timeout: SUBFOLDER_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
    const req: PSCopyRequest = submit.mock.calls[0]?.[0];
    expect(req.sourcePath).toBe("/Sites/A");
    expect(req.targetPath).toBe("/Sites/B");
    expect(req).not.toHaveProperty("copy");
    expect(screen.getByTestId("subfolder-copy-progress").textContent).toBe(
      "Subfolder copy completed",
    );
  });

  it("Run captures the error when submit rejects", { timeout: SUBFOLDER_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockRejectedValue(new Error("mock fail"));
    render(<SubfolderCopyWizard submit={submit} />);
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.change(screen.getByTestId("subfolder-copy-target"), {
      target: { value: "/Sites/B" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalled();
    },
      { timeout: SUBFOLDER_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("subfolder-copy-progress").textContent).toContain(
      "mock fail",
    );
  });

  it("maps an ApiError 409 and reports failure without a success settle (#4750)", { timeout: SUBFOLDER_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: {},
    });
    const onSettled = vi.fn();
    render(<SubfolderCopyWizard submit={submit} onSettled={onSettled} />);
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.change(screen.getByTestId("subfolder-copy-target"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-run"));
    await waitFor(() => {
      expect(onSettled).toHaveBeenCalledWith(false);
    },
      { timeout: SUBFOLDER_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("subfolder-copy-progress").textContent).toContain(
      "HTTP 409",
    );
  });

  it("passes the destination path when copy succeeds (#4750)", { timeout: SUBFOLDER_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    const onSettled = vi.fn();
    render(<SubfolderCopyWizard submit={submit} onSettled={onSettled} />);
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.change(screen.getByTestId("subfolder-copy-target"), {
      target: { value: "/Sites/B" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    fireEvent.click(screen.getByTestId("subfolder-copy-run"));
    await waitFor(() => {
      expect(onSettled).toHaveBeenCalledWith(true, "/Sites/B");
    },
      { timeout: SUBFOLDER_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
  });

  it("Back walks to the previous step and does not dismiss", () => {
    const onDismiss = vi.fn();
    const submit = vi.fn();
    render(
      <SubfolderCopyWizard submit={submit} onDismiss={onDismiss} />,
    );
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    expect(screen.getByTestId("subfolder-copy-step-target")).toBeTruthy();
    fireEvent.click(screen.getByTestId("subfolder-copy-back"));
    expect(screen.getByTestId("subfolder-copy-step-source")).toBeTruthy();
    expect(screen.getByTestId("subfolder-copy-wizard")).toBeTruthy();
    expect(onDismiss).not.toHaveBeenCalled();
    expect(submit).not.toHaveBeenCalled();
  });

  it("Cancel with onDismiss dismisses instead of resetWizard (#3553)", () => {
    const onDismiss = vi.fn();
    const submit = vi.fn();
    render(
      <SubfolderCopyWizard submit={submit} onDismiss={onDismiss} />,
    );
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    expect(screen.getByTestId("subfolder-copy-step-target")).toBeTruthy();
    fireEvent.click(screen.getByTestId("subfolder-copy-cancel"));
    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(submit).not.toHaveBeenCalled();
    // Host unmounts; without unmount, Cancel must not reset to step 1.
    expect(screen.getByTestId("subfolder-copy-step-target")).toBeTruthy();
    expect(screen.queryByTestId("subfolder-copy-step-source")).toBeNull();
  });

  it("Cancel without onDismiss still resetWizard to step 0", () => {
    render(<SubfolderCopyWizard />);
    fireEvent.change(screen.getByTestId("subfolder-copy-source"), {
      target: { value: "/Sites/A" },
    });
    fireEvent.click(screen.getByTestId("subfolder-copy-next"));
    expect(screen.getByTestId("subfolder-copy-step-target")).toBeTruthy();
    fireEvent.click(screen.getByTestId("subfolder-copy-cancel"));
    expect(screen.getByTestId("subfolder-copy-step-source")).toBeTruthy();
    expect(screen.queryByTestId("subfolder-copy-step-target")).toBeNull();
  });

  it("passes the zero serious/critical axe-core gate (step 0)", { timeout: SUBFOLDER_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const { container } = render(<SubfolderCopyWizard />);
    await renderA11yGate(container);
  });
});

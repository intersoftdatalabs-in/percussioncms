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
import type { PSSiteCopyRequest } from "../../../main/ts/api/contentExplorer/types";
import { SiteCopyWizard } from "../../../main/ts/contentExplorer/wizards/SiteCopyWizard";
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
const SITE_COPY_WIZARD_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const SITE_COPY_WIZARD_TEST_TIMEOUT = 30_000;

describe("SiteCopyWizard", () => {
  it("renders the 5-step wizard at step 0 (source)", () => {
    render(<SiteCopyWizard />);
    expect(screen.getByTestId("site-copy-wizard")).toBeTruthy();
    expect(screen.getByTestId("site-copy-step-source")).toBeTruthy();
    expect(screen.getByTestId("site-copy-step-count").textContent).toMatch(
      /Step 1.*of 5/,
    );
  });

  it("Next is disabled at step 0 when source is empty", () => {
    render(<SiteCopyWizard />);
    const next = screen.getByTestId("site-copy-next") as HTMLButtonElement;
    expect(next.disabled).toBe(true);
  });

  it("filling source enables Next and advances to step 1", () => {
    render(<SiteCopyWizard />);
    fireEvent.change(screen.getByTestId("site-copy-source"), {
      target: { value: "SourceSite" },
    });
    const next = screen.getByTestId("site-copy-next") as HTMLButtonElement;
    expect(next.disabled).toBe(false);
    fireEvent.click(next);
    expect(screen.getByTestId("site-copy-step-target")).toBeTruthy();
  });

  it("Back returns to the previous step", () => {
    render(<SiteCopyWizard initialSource="A" initialTarget="B" />);
    fireEvent.click(screen.getByTestId("site-copy-next"));
    expect(screen.getByTestId("site-copy-step-target")).toBeTruthy();
    fireEvent.click(screen.getByTestId("site-copy-back"));
    expect(screen.getByTestId("site-copy-step-source")).toBeTruthy();
  });

  it("Run invokes the supplied submit and renders an ok summary", { timeout: SITE_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    render(<SiteCopyWizard submit={submit} />);
    fireEvent.change(screen.getByTestId("site-copy-source"), {
      target: { value: "A" },
    });
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.change(screen.getByTestId("site-copy-target"), {
      target: { value: "B" },
    });
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-next"));
    // Step 3 = confirm; Run button appears at the final step.
    expect(screen.getByTestId("site-copy-step-confirm")).toBeTruthy();
    fireEvent.click(screen.getByTestId("site-copy-next"));
    expect(screen.getByTestId("site-copy-step-progress")).toBeTruthy();
    fireEvent.click(screen.getByTestId("site-copy-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    },
      { timeout: SITE_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
    const req: PSSiteCopyRequest = submit.mock.calls[0]?.[0];
    expect(req.sourceSite).toBe("A");
    expect(req.targetSite).toBe("B");
    expect(screen.getByTestId("site-copy-progress").textContent).toContain(
      "Site copy completed",
    );
    expect(screen.getByTestId("site-copy-result-name").textContent).toContain(
      "B",
    );
  });

  it("Run captures the error when submit rejects", { timeout: SITE_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockRejectedValue(new Error("mock failure"));
    render(<SiteCopyWizard submit={submit} />);
    fireEvent.change(screen.getByTestId("site-copy-source"), {
      target: { value: "A" },
    });
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.change(screen.getByTestId("site-copy-target"), {
      target: { value: "B" },
    });
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-run"));
    await waitFor(() => {
      expect(submit).toHaveBeenCalledTimes(1);
    },
      { timeout: SITE_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("site-copy-progress").textContent).toContain(
      "mock failure",
    );
  });

  it("maps HTTP 409 from submit onto conflict chrome", { timeout: SITE_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const submit = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<SiteCopyWizard submit={submit} initialSource="A" initialTarget="A" />);
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-next"));
    fireEvent.click(screen.getByTestId("site-copy-run"));
    await waitFor(() => {
      expect(screen.getByTestId("site-copy-progress").textContent).toContain(
        "HTTP 409",
      );
    },
      { timeout: SITE_COPY_WIZARD_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("site-copy-result-name")).toBeNull();
  });

  it("passes the zero serious/critical axe-core gate (step 0)", { timeout: SITE_COPY_WIZARD_TEST_TIMEOUT }, async () => {
    const { container } = render(<SiteCopyWizard />);
    await renderA11yGate(container);
  });
});

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
import type { PSMoveFolderItem } from "../../../main/ts/api/contentExplorer/types";
import { SubfolderCopyWizard } from "../../../main/ts/contentExplorer/wizards/SubfolderCopyWizard";
import { renderA11yGate } from "./a11y";

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

  it("full flow advances and submits via the supplied transport", async () => {
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
    });
    const req: PSMoveFolderItem = submit.mock.calls[0]?.[0];
    expect(req.sourcePath).toBe("/Sites/A");
    expect(req.targetPath).toBe("/Sites/B");
    expect(req.copy).toBe(true);
    expect(screen.getByTestId("subfolder-copy-progress").textContent).toBe(
      "Subfolder copy completed",
    );
  });

  it("Run captures the error when submit rejects", async () => {
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
    });
    expect(screen.getByTestId("subfolder-copy-progress").textContent).toContain(
      "mock fail",
    );
  });

  it("passes the zero serious/critical axe-core gate (step 0)", async () => {
    const { container } = render(<SubfolderCopyWizard />);
    await renderA11yGate(container);
  });
});

/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getTemplateDetail,
  listSlots,
} from "../../../main/ts/api/developer/assemblyApi";
import { TemplateDetailPanel } from "../../../main/ts/developer/TemplateDetailPanel";
import * as sourceViewer from "../../../main/ts/developer/templateSourceViewer";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  getTemplateDetail: vi.fn(),
  listSlots: vi.fn().mockResolvedValue([]),
  updateTemplateDetail: vi.fn(),
}));

const getTemplateDetailMock = vi.mocked(getTemplateDetail);
const listSlotsMock = vi.mocked(listSlots);

const multiLineSource = "<html>\n<body>$sys.variables\n</body>\n</html>";

describe("TemplateDetailPanel", () => {
  beforeEach(() => {
    getTemplateDetailMock.mockReset();
    listSlotsMock.mockReset();
    listSlotsMock.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders bindings slots and source", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.page",
      label: "Page",
      bindings: [{ executionOrder: 1, variable: "$x", expression: "1" }],
      slots: [{ name: "target", label: "Target" }],
      templateSource: multiLineSource,
      designGaps: ["read-only"],
    });
    listSlotsMock.mockResolvedValue([
      { name: "target", label: "Target", description: "Main slot" },
    ]);
    const onBack = vi.fn();
    render(<TemplateDetailPanel idOrName="perc.page" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-bindings")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-lines")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-ln-1")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-ln-4")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-copy")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-tpl-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("toggles preview highlight and copy feedback", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.page",
      label: "Page",
      templateSource: multiLineSource,
    });
    const copySpy = vi.spyOn(sourceViewer, "copyTextToClipboard").mockResolvedValue(true);
    render(<TemplateDetailPanel idOrName="perc.page" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-tpl-source-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-preview")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-source-hl-1")).toBeTruthy();
    expect(screen.queryByTestId("developer-tpl-source-edit")).toBeNull();

    fireEvent.click(screen.getByTestId("developer-tpl-source-copy"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-copy-feedback").textContent).toMatch(
        /Copied/i,
      );
    });
    expect(copySpy).toHaveBeenCalledWith(multiLineSource);

    fireEvent.click(screen.getByTestId("developer-tpl-source-mode"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    });
  });

  it("shows error when detail load fails", async () => {
    getTemplateDetailMock.mockRejectedValue({ status: 404, statusText: "Not Found" });
    render(<TemplateDetailPanel idOrName="missing" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-detail-error").textContent).toMatch(/404/);
  });
});

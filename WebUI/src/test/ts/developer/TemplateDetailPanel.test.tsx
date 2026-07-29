/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getTemplateDetail } from "../../../main/ts/api/developer/assemblyApi";
import { TemplateDetailPanel } from "../../../main/ts/developer/TemplateDetailPanel";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  getTemplateDetail: vi.fn(),
}));

const getTemplateDetailMock = vi.mocked(getTemplateDetail);

describe("TemplateDetailPanel", () => {
  beforeEach(() => {
    getTemplateDetailMock.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders bindings slots and source", async () => {
    getTemplateDetailMock.mockResolvedValue({
      name: "perc.page",
      label: "Page",
      bindings: [{ executionOrder: 1, variable: "$x", expression: "1" }],
      slots: [{ name: "target", label: "Target" }],
      templateSource: "<html/>",
      designGaps: ["read-only"],
    });
    const onBack = vi.fn();
    render(<TemplateDetailPanel idOrName="perc.page" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-bindings")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-tpl-back"));
    expect(onBack).toHaveBeenCalled();
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

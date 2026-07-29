/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { DeveloperShell } from "../../../main/ts/developer/DeveloperShell";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  listContentTypes: vi.fn().mockResolvedValue([
    {
      name: "percPage",
      label: "Page",
      description: "A page",
      guid: { stringValue: "0-2-301", uuid: 301 },
    },
  ]),
}));

describe("DeveloperShell", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("renders shell and loads content types by default", async () => {
    render(<DeveloperShell embedded />);
    expect(screen.getByTestId("perc-developer-shell")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    expect(screen.getByText("percPage")).toBeTruthy();
    expect(screen.getByText("Page")).toBeTruthy();
  });

  it("shows placeholder for unimplemented sections", async () => {
    render(<DeveloperShell initialSection="templates" embedded />);
    expect(screen.getByTestId("tab-developer-templates").getAttribute("aria-selected")).toBe(
      "true",
    );
    expect(screen.getByTestId("developer-placeholder")).toBeTruthy();

    fireEvent.click(screen.getByTestId("tab-developer-content-types"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-panel")).toBeTruthy();
    });
  });
});

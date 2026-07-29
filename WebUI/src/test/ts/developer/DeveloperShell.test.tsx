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
  getContentTypeDetail: vi.fn().mockResolvedValue({
    name: "percPage",
    label: "Page",
    description: "A page",
    enabled: true,
    fields: [
      {
        name: "sys_title",
        label: "Title",
        fieldType: "system",
        dataType: "text",
        control: "sys_EditBox",
        searchable: true,
      },
    ],
    allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
    defaultWorkflow: { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
    allowedTemplates: [{ name: "perc.page", label: "Page" }],
    designGaps: ["Field validation rules not exposed"],
  }),
}));

vi.mock("../../../main/ts/api/developer/keywordsApi", () => ({
  listKeywords: vi.fn().mockResolvedValue([
    {
      label: "Status",
      value: "status",
      description: "Status keyword",
      choices: [
        { label: "Open", value: "open" },
        { label: "Closed", value: "closed" },
      ],
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

  it("opens content type detail from list row", async () => {
    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-fields-table")).toBeTruthy();
    expect(screen.getByText("sys_title")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-workflows")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-templates")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ct-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
  });

  it("loads keywords section", async () => {
    render(<DeveloperShell initialSection="keywords" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-table")).toBeTruthy();
    });
    expect(screen.getByText("Status")).toBeTruthy();
    expect(screen.getByText("2")).toBeTruthy();
  });
});

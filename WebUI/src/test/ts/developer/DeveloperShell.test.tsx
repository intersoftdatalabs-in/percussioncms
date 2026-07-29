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
        required: true,
        readOnly: false,
        occurrence: "required",
        hasValidation: true,
        hasVisibilityRules: false,
        hasInputTranslation: false,
        hasOutputTranslation: false,
      },
    ],
    allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
    defaultWorkflow: { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
    allowedTemplates: [{ name: "perc.page", label: "Page" }],
    designGaps: [
      "Field rule flags are exposed (validation/visibility/transforms present); full rule expressions and control properties are not",
    ],
  }),
}));

vi.mock("../../../main/ts/api/developer/keywordsApi", () => ({
  listKeywords: vi.fn().mockResolvedValue([
    {
      label: "Status",
      value: "status",
      description: "Status keyword",
      guid: { uuid: 42, stringValue: "0-37-42" },
      choices: [
        { label: "Open", value: "open" },
        { label: "Closed", value: "closed" },
      ],
    },
  ]),
  getKeyword: vi.fn().mockResolvedValue({
    label: "Status",
    value: "status",
    description: "Status keyword",
    guid: { uuid: 42, stringValue: "0-37-42" },
    choices: [{ label: "Open", value: "open" }],
  }),
  createKeyword: vi.fn(),
  updateKeyword: vi.fn(),
  deleteKeyword: vi.fn(),
}));

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn().mockResolvedValue([
    {
      templateId: 1,
      templateName: "perc.page",
      templateLabel: "Page",
      templateDescription: "Default page",
    },
  ]),
  getTemplateDetail: vi.fn().mockResolvedValue({
    templateId: 1,
    name: "perc.page",
    label: "Page",
    description: "Default page",
    assembler: "Java/JEXL",
    outputFormat: "Page",
    templateType: "Shared",
    aaType: "Normal",
    mimeType: "text/html",
    variant: false,
    bindings: [{ executionOrder: 1, variable: "$sys.item", expression: "$sys.item" }],
    slots: [{ name: "target", label: "Target" }],
    templateSource: "<html/>",
    designGaps: ["Create / update / delete / lock not supported (read-only)"],
  }),
  listSlots: vi.fn().mockResolvedValue([
    { name: "target", label: "Target", description: "Main slot" },
  ]),
  getSlotDetail: vi.fn().mockResolvedValue({
    name: "target",
    label: "Target",
    description: "Main slot",
    slotType: "REGULAR",
    systemSlot: false,
    finderName: "sys_SlotContentFinder",
    finderArguments: { template: "rffSnTitleLink" },
    associations: [
      {
        contentTypeGuid: { stringValue: "0-2-301", uuid: 301 },
        templateGuid: { stringValue: "0-10-1", uuid: 1 },
      },
    ],
    designGaps: ["Create / update / delete / lock not supported (read-only)"],
  }),
  listCommunities: vi.fn().mockResolvedValue([
    { id: 10, name: "Default", label: "Default", description: "Default Community" },
  ]),
  getCommunityDetail: vi.fn().mockResolvedValue({
    id: 10,
    name: "Default",
    label: "Default",
    description: "Default Community",
    guid: { stringValue: "0-13-10", uuid: 10 },
    roleList: [
      { roleId: 1, roleName: "Admin", roleGuid: { stringValue: "0-14-1", uuid: 1 } },
      { roleId: 2, roleName: "Author", roleGuid: { stringValue: "0-14-2", uuid: 2 } },
    ],
  }),
}));

vi.mock("../../../main/ts/api/developer/pipelinesApi", () => ({
  listApplications: vi.fn().mockResolvedValue([
    {
      id: 1,
      name: "sys_cmpDocuments",
      description: "System content editor app",
      enabled: true,
      appType: "CONTENT_EDITOR",
      appRoot: "sys_cmpDocuments",
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

  it("loads pipelines catalog section", async () => {
    render(<DeveloperShell initialSection="pipelines" embedded />);
    expect(screen.getByTestId("tab-developer-pipelines").getAttribute("aria-selected")).toBe(
      "true",
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-pipe-table")).toBeTruthy();
    });
    expect(screen.getAllByText("sys_cmpDocuments").length).toBeGreaterThan(0);
    expect(screen.getByText("CONTENT_EDITOR")).toBeTruthy();

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
    expect(screen.getByTestId("developer-ct-field-rules").textContent).toMatch(/validation/);
    // Occurrence cell value is lowercase "required" (distinct from "Required" header)
    expect(screen.getByTestId("developer-ct-field-occurrence").textContent).toBe("required");
    expect(screen.getByTestId("developer-ct-workflows")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-templates")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ct-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
  });

  it("loads keywords section and opens editor", async () => {
    render(<DeveloperShell initialSection="keywords" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-table")).toBeTruthy();
    });
    expect(screen.getByText("Status")).toBeTruthy();
    expect(screen.getByText("2")).toBeTruthy();
    expect(screen.getByTestId("developer-kw-new")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-kw-new"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-editor")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-kw-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-table")).toBeTruthy();
    });
  });

  it("loads templates slots and communities catalogs", async () => {
    const { unmount } = render(<DeveloperShell initialSection="templates" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
    expect(screen.getByText("perc.page")).toBeTruthy();
    unmount();

    const r2 = render(<DeveloperShell initialSection="slots" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-table")).toBeTruthy();
    });
    expect(screen.getByText("target")).toBeTruthy();
    // Open is button-only (row click removed for a11y / selectionKey null safety)
    fireEvent.click(screen.getByRole("button", { name: /Open Target/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-slot-args")).toBeTruthy();
    expect(screen.getByTestId("developer-slot-associations")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-slot-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-table")).toBeTruthy();
    });
    r2.unmount();

    render(<DeveloperShell initialSection="communities" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
    expect(screen.getByText("Default Community")).toBeTruthy();
  });

  it("opens community detail from list row", async () => {
    render(<DeveloperShell initialSection="communities" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Default/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-roles")).toBeTruthy();
    expect(screen.getByText("Admin")).toBeTruthy();
    expect(screen.getByTestId("developer-comm-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-comm-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
  });

  it("opens template detail from list row", async () => {
    render(<DeveloperShell initialSection="templates" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Page/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-bindings")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-tpl-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
  });
});

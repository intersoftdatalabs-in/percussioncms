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
    guid: { stringValue: "0-2-301", uuid: 301 },
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
      {
        name: "page_title",
        label: "Page title",
        fieldType: "local",
        dataType: "text",
        control: "sys_EditBox",
        searchable: false,
        required: false,
        readOnly: false,
        occurrence: "optional",
      },
    ],
    allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
    defaultWorkflow: { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
    allowedTemplates: [{ name: "perc.page", label: "Page" }],
    designGaps: [
      "Field rule flags are exposed (validation/visibility/transforms present); full rule expressions and control properties are not",
    ],
  }),
  updateContentTypeDetail: vi.fn().mockImplementation(async (_id, body) => ({
    name: "percPage",
    label: body.label ?? "Page",
    description: body.description ?? "A page",
    enabled: body.enabled ?? true,
    guid: { stringValue: "0-2-301", uuid: 301 },
    fields: [
      {
        name: "sys_title",
        label: "Title",
        fieldType: "system",
        searchable: true,
        required: true,
        occurrence: "required",
      },
      {
        name: "page_title",
        label: "Page title",
        fieldType: "local",
        searchable: body.fields?.find((f: { name?: string }) => f.name === "page_title")
          ?.searchable ?? false,
        required: body.fields?.find((f: { name?: string }) => f.name === "page_title")
          ?.required ?? false,
        occurrence: "optional",
      },
    ],
    allowedWorkflows:
      body.allowedWorkflows ??
      [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
    defaultWorkflow:
      body.defaultWorkflow ??
      { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
    allowedTemplates: body.allowedTemplates ?? [{ name: "perc.page", label: "Page" }],
    designGaps: [],
  })),
}));

const { defaultAclPayload } = vi.hoisted(() => ({
  defaultAclPayload: {
    id: 1,
    name: "object-acl",
    guid: { stringValue: "0-4-1", uuid: 1 },
    aclEntries: [
      {
        id: 10,
        name: "Default",
        type: { type: "ROLE", name: "Default" },
        permissions: [
          { permission: "READ" },
          { permission: "UPDATE" },
          // Unknown/custom permission must be sticky on save (not dropped)
          { permission: "CUSTOM_LEGACY" },
        ],
      },
      {
        id: 11,
        name: "Admin",
        type: { type: "ROLE", name: "Admin" },
        permissions: [{ permission: "OWNER" }],
      },
    ],
  },
}));

vi.mock("../../../main/ts/api/developer/aclApi", async () => {
  const actual = await vi.importActual<
    typeof import("../../../main/ts/api/developer/aclApi")
  >("../../../main/ts/api/developer/aclApi");
  return {
    ...actual,
    getAclForObject: vi.fn().mockResolvedValue(defaultAclPayload),
    saveObjectAcl: vi.fn().mockResolvedValue(undefined),
    createObjectAcl: vi.fn().mockImplementation(async (_guid, owner) => ({
      id: 99,
      name: "new-object-acl",
      guid: { stringValue: "0-4-99", uuid: 99 },
      aclEntries: [
        {
          id: 100,
          name: owner.name,
          type: { type: owner.type, name: owner.name },
          permissions: [{ permission: "OWNER" }],
        },
      ],
    })),
  };
});

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
    guid: { stringValue: "0-10-1", uuid: 1 },
    assembler: "Java/JEXL",
    outputFormat: "Page",
    templateType: "Shared",
    aaType: "Normal",
    mimeType: "text/html",
    variant: false,
    bindings: [{ executionOrder: 1, variable: "$sys.item", expression: "$sys.item" }],
    slots: [{ name: "target", label: "Target" }],
    templateSource: "<html/>",
    designGaps: ["Create / delete / lock not supported via this API"],
  }),
  updateTemplateDetail: vi.fn().mockImplementation(async (_id, body) => ({
    templateId: 1,
    name: "perc.page",
    label: body.label ?? "Page",
    description: body.description ?? "Default page",
    templateSource: body.templateSource ?? "<html/>",
    bindings: body.bindings ?? [
      { executionOrder: 1, variable: "$sys.item", expression: "$sys.item" },
    ],
    slots: body.slots ?? [{ name: "target", label: "Target" }],
    designGaps: ["Create / delete / lock not supported via this API"],
  })),
  listSlots: vi.fn().mockResolvedValue([
    { name: "target", label: "Target", description: "Main slot" },
    { name: "sidebar", label: "Sidebar", description: "Side slot" },
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
    designGaps: ["Create / delete / lock not supported via this API"],
  }),
  updateSlotDetail: vi.fn().mockImplementation(async (_id, body) => ({
    name: "target",
    label: body.label ?? "Target",
    description: body.description ?? "Main slot",
    associations: body.associations ?? [
      {
        contentTypeGuid: { stringValue: "0-2-301", uuid: 301 },
        templateGuid: { stringValue: "0-10-1", uuid: 1 },
      },
    ],
    designGaps: ["Create / delete / lock not supported via this API"],
  })),
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
  listAvailableRoles: vi.fn().mockResolvedValue([
    { roleId: 1, roleName: "Admin", roleGuid: { stringValue: "0-14-1", uuid: 1 } },
    { roleId: 2, roleName: "Author", roleGuid: { stringValue: "0-14-2", uuid: 2 } },
    { roleId: 3, roleName: "Editor", roleGuid: { stringValue: "0-14-3", uuid: 3 } },
  ]),
  updateCommunityRoles: vi.fn().mockResolvedValue({
    id: 10,
    name: "Default",
    label: "Default",
    roleList: [
      { roleId: 1, roleName: "Admin", roleGuid: { stringValue: "0-14-1", uuid: 1 } },
      { roleId: 3, roleName: "Editor", roleGuid: { stringValue: "0-14-3", uuid: 3 } },
    ],
  }),
  getCommunityVisibility: vi.fn().mockResolvedValue([
    {
      name: "percPage",
      label: "Page",
      type: "NODEDEF",
      guid: { stringValue: "0-2-301", uuid: 301 },
    },
    {
      name: "perc.page",
      label: "Page template",
      type: "TEMPLATE",
      guid: { stringValue: "0-10-1", uuid: 1 },
    },
  ]),
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
  getApplicationDetail: vi.fn().mockResolvedValue({
    id: 1,
    name: "sys_cmpDocuments",
    description: "System content editor app",
    enabled: true,
    appType: "CONTENT_EDITOR",
    appRoot: "sys_cmpDocuments",
    dataSets: [],
    designGaps: [],
  }),
}));

vi.mock("../../../main/ts/api/developer/itemFiltersApi", () => ({
  listItemFilters: vi.fn().mockResolvedValue([
    { name: "public", description: "Public", rules: [] },
  ]),
  getItemFilterDetail: vi.fn().mockResolvedValue({
    name: "public",
    rules: [],
  }),
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

  it("loads item filters catalog section", async () => {
    render(<DeveloperShell initialSection="item-filters" embedded />);
    expect(screen.getByTestId("tab-developer-item-filters").getAttribute("aria-selected")).toBe(
      "true",
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-if-table")).toBeTruthy();
    });
    expect(screen.getByText("public")).toBeTruthy();
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
    expect(screen.getAllByTestId("developer-ct-field-rules")[0].textContent).toMatch(/validation/);
    // Occurrence cell value is lowercase "required" (distinct from "Required" header)
    expect(screen.getAllByTestId("developer-ct-field-occurrence")[0].textContent).toBe("required");
    expect(screen.getByTestId("developer-ct-workflows")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-templates")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-label")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-save")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ct-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
  });

  it("edits content type field searchable and saves with design lock path", async () => {
    const { updateContentTypeDetail } = await import(
      "../../../main/ts/api/developer/contentTypesApi"
    );
    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-field-search-page_title")).toBeTruthy();
    });
    const saveBtn = screen.getByTestId("developer-ct-save");
    expect((saveBtn as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("developer-ct-field-search-page_title"));
    expect((saveBtn as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(saveBtn);
    await waitFor(() => {
      expect(updateContentTypeDetail).toHaveBeenCalled();
    });
    const body = (updateContentTypeDetail as ReturnType<typeof vi.fn>).mock.calls.at(-1)?.[1];
    expect(body.fields).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: "page_title", searchable: true }),
      ]),
    );
    // Field-only save must not wipe associations
    expect(body.allowedWorkflows).toBeUndefined();
    expect(body.allowedTemplates).toBeUndefined();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
  });

  it("edits content type workflow and template associations on save", async () => {
    const { updateContentTypeDetail } = await import(
      "../../../main/ts/api/developer/contentTypesApi"
    );
    (updateContentTypeDetail as ReturnType<typeof vi.fn>).mockClear();
    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-wf-add-name"), {
      target: { value: "Standard Workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-wf-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-row-1")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-tpl-add-name"), {
      target: { value: "perc.page.summary" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-tpl-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-row-1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(updateContentTypeDetail).toHaveBeenCalled();
    });
    const body = (updateContentTypeDetail as ReturnType<typeof vi.fn>).mock.calls.at(-1)?.[1];
    expect(body.allowedWorkflows).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: "Simple Workflow" }),
        expect.objectContaining({ name: "Standard Workflow" }),
      ]),
    );
    expect(body.defaultWorkflow).toEqual(
      expect.objectContaining({ name: "Simple Workflow" }),
    );
    expect(body.allowedTemplates).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: "perc.page" }),
        expect.objectContaining({ name: "perc.page.summary" }),
      ]),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
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
    expect(screen.getByTestId("developer-slot-label")).toBeTruthy();
    expect(screen.getByTestId("developer-slot-save")).toBeTruthy();
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
    const { getCommunityVisibility } = await import(
      "../../../main/ts/api/developer/assemblyApi"
    );
    (getCommunityVisibility as ReturnType<typeof vi.fn>).mockResolvedValue([
      {
        name: "percPage",
        label: "Page",
        type: "NODEDEF",
        guid: { stringValue: "0-2-301", uuid: 301 },
      },
      {
        name: "perc.page",
        label: "Page template",
        type: "TEMPLATE",
        guid: { stringValue: "0-10-1", uuid: 1 },
      },
    ]);
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
    expect(screen.getByText("Editor")).toBeTruthy();
    expect(screen.getByTestId("developer-comm-roles-save")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-table")).toBeTruthy();
    });
    expect(screen.getByText("NODEDEF")).toBeTruthy();
    expect(getCommunityVisibility).toHaveBeenCalled();
    expect(screen.getByTestId("developer-comm-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-comm-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
  });

  it("shows empty community visibility state", async () => {
    const { getCommunityVisibility } = await import(
      "../../../main/ts/api/developer/assemblyApi"
    );
    (getCommunityVisibility as ReturnType<typeof vi.fn>).mockResolvedValueOnce([]);
    render(<DeveloperShell initialSection="communities" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Default/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-empty")).toBeTruthy();
    });
  });

  it("shows community visibility error state", async () => {
    const { getCommunityVisibility } = await import(
      "../../../main/ts/api/developer/assemblyApi"
    );
    (getCommunityVisibility as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      Object.assign(new Error("vis fail"), { status: 500 }),
    );
    render(<DeveloperShell initialSection="communities" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Default/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-error").textContent).toMatch(
        /visibility|fail|error/i,
      );
    });
  });

  it("shows no-guid message when community detail lacks guid", async () => {
    const { getCommunityDetail, getCommunityVisibility } = await import(
      "../../../main/ts/api/developer/assemblyApi"
    );
    (getCommunityVisibility as ReturnType<typeof vi.fn>).mockClear();
    (getCommunityDetail as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      id: 10,
      name: "Default",
      label: "Default",
      description: "Default Community",
      // no guid
      roleList: [
        { roleId: 1, roleName: "Admin", roleGuid: { stringValue: "0-14-1", uuid: 1 } },
      ],
    });
    render(<DeveloperShell initialSection="communities" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Default/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-visibility-error").textContent).toMatch(
        /GUID not available/i,
      );
    });
    expect(getCommunityVisibility).not.toHaveBeenCalled();
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
    expect(screen.getByTestId("developer-tpl-bindings-table")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-slots-table")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-source-edit")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-save")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-binding-add")).toBeTruthy();
    expect(screen.getByTestId("developer-tpl-gaps")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-tpl-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
  });

  it("edits template bindings and slot membership and saves", async () => {
    const { updateTemplateDetail } = await import("../../../main/ts/api/developer/assemblyApi");
    render(<DeveloperShell initialSection="templates" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Page/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-binding-var-0")).toBeTruthy();
    });
    const saveBtn = screen.getByTestId("developer-tpl-save");
    expect((saveBtn as HTMLButtonElement).disabled).toBe(true);

    fireEvent.change(screen.getByTestId("developer-tpl-binding-expr-0"), {
      target: { value: "$sys.item.fields.title" },
    });
    fireEvent.click(screen.getByTestId("developer-tpl-slot-check-name:sidebar"));
    expect((saveBtn as HTMLButtonElement).disabled).toBe(false);

    fireEvent.click(saveBtn);
    await waitFor(() => {
      expect(updateTemplateDetail).toHaveBeenCalled();
    });
    const body = (updateTemplateDetail as ReturnType<typeof vi.fn>).mock.calls.at(-1)?.[1];
    expect(body.bindings[0].expression).toBe("$sys.item.fields.title");
    expect(body.slots.map((s: { name?: string }) => s.name)).toEqual(
      expect.arrayContaining(["target", "sidebar"]),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-detail-notice").textContent).toMatch(/saved/i);
    });
  });

  it("edits slot associations and saves", async () => {
    const { updateSlotDetail } = await import("../../../main/ts/api/developer/assemblyApi");
    render(<DeveloperShell initialSection="slots" embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByRole("button", { name: /Open Target/i }));
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-assoc-table")).toBeTruthy();
    });
    const saveBtn = screen.getByTestId("developer-slot-save");
    expect((saveBtn as HTMLButtonElement).disabled).toBe(true);

    fireEvent.change(screen.getByTestId("developer-slot-assoc-ct"), {
      target: { value: "0-2-999" },
    });
    fireEvent.change(screen.getByTestId("developer-slot-assoc-tpl"), {
      target: { value: "0-10-99" },
    });
    fireEvent.click(screen.getByTestId("developer-slot-assoc-add"));
    expect((saveBtn as HTMLButtonElement).disabled).toBe(false);

    fireEvent.click(saveBtn);
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalled();
    });
    const body = (updateSlotDetail as ReturnType<typeof vi.fn>).mock.calls.at(-1)?.[1];
    expect(body.associations).toHaveLength(2);
    expect(body.associations[1].contentTypeGuid.stringValue).toBe("0-2-999");
    expect(body.associations[1].templateGuid.stringValue).toBe("0-10-99");
    await waitFor(() => {
      expect(screen.getByTestId("developer-slot-detail-notice").textContent).toMatch(/saved/i);
    });
  });

  it("edits object ACL permissions on content type detail and saves", async () => {
    const { saveObjectAcl, getAclForObject } = await import(
      "../../../main/ts/api/developer/aclApi"
    );
    (saveObjectAcl as ReturnType<typeof vi.fn>).mockClear();
    (getAclForObject as ReturnType<typeof vi.fn>).mockResolvedValue(defaultAclPayload);
    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-table")).toBeTruthy();
    });
    const saveBtn = screen.getByTestId("developer-ct-acl-save");
    expect((saveBtn as HTMLButtonElement).disabled).toBe(true);

    const deleteCheck = screen.getByTestId("developer-ct-acl-perm-id:10-DELETE");
    expect((deleteCheck as HTMLInputElement).checked).toBe(false);
    fireEvent.click(deleteCheck);
    expect((deleteCheck as HTMLInputElement).checked).toBe(true);
    expect((saveBtn as HTMLButtonElement).disabled).toBe(false);

    fireEvent.click(saveBtn);
    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    const payload = (saveObjectAcl as ReturnType<typeof vi.fn>).mock.calls[0][0];
    const defaultEntry = payload.aclEntries.find((e: { id?: number }) => e.id === 10);
    const perms = defaultEntry.permissions.map((p: { permission: string }) => p.permission);
    expect(perms).toEqual(
      expect.arrayContaining(["READ", "UPDATE", "DELETE", "CUSTOM_LEGACY"]),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-notice").textContent).toMatch(/saved/i);
    });
  });

  it("reports ACL reload error separately when save succeeds but reload fails", async () => {
    const { saveObjectAcl, getAclForObject } = await import(
      "../../../main/ts/api/developer/aclApi"
    );
    (saveObjectAcl as ReturnType<typeof vi.fn>).mockClear();
    (saveObjectAcl as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
    let getCalls = 0;
    (getAclForObject as ReturnType<typeof vi.fn>).mockImplementation(async () => {
      getCalls += 1;
      if (getCalls === 1) {
        return defaultAclPayload;
      }
      const err = new Error("reload boom") as Error & { status?: number };
      err.status = 500;
      throw err;
    });

    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-acl-perm-id:10-DELETE"));
    fireEvent.click(screen.getByTestId("developer-ct-acl-save"));

    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    await waitFor(() => {
      const notice = screen.getByTestId("developer-ct-acl-notice").textContent || "";
      expect(notice).toMatch(/saved/i);
    });
    await waitFor(() => {
      const err = screen.getByTestId("developer-ct-acl-error").textContent || "";
      expect(err).toMatch(/could not reload/i);
      expect(err).not.toMatch(/^Could not save object ACL/);
    });
  });

  it("adds and removes ACL entries then saves full entry list", async () => {
    const { saveObjectAcl, getAclForObject } = await import(
      "../../../main/ts/api/developer/aclApi"
    );
    (saveObjectAcl as ReturnType<typeof vi.fn>).mockClear();
    (getAclForObject as ReturnType<typeof vi.fn>).mockResolvedValue(defaultAclPayload);

    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-table")).toBeTruthy();
    });

    // Remove Admin entry (id 11)
    fireEvent.click(screen.getByTestId("developer-ct-acl-remove-id:11"));
    expect(screen.queryByTestId("developer-ct-acl-row-id:11")).toBeNull();

    // Add Editor role
    fireEvent.change(screen.getByTestId("developer-ct-acl-add-name"), {
      target: { value: "Editor" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-acl-add-type"), {
      target: { value: "ROLE" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-acl-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-row-__new:1")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-ct-acl-save"));
    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    const payload = (saveObjectAcl as ReturnType<typeof vi.fn>).mock.calls.at(-1)?.[0];
    const names = payload.aclEntries.map(
      (e: { name?: string; principal?: { name?: string } }) =>
        e.name || e.principal?.name,
    );
    expect(names).toEqual(expect.arrayContaining(["Default", "Editor"]));
    expect(names).not.toEqual(expect.arrayContaining(["Admin"]));
    const editor = payload.aclEntries.find(
      (e: { name?: string }) => e.name === "Editor",
    );
    expect(editor.type.type).toBe("ROLE");
    expect(editor.permissions.map((p: { permission: string }) => p.permission)).toEqual(
      expect.arrayContaining(["READ"]),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-notice").textContent).toMatch(/saved/i);
    });
  });

  it("rejects duplicate ACL entry name+type without adding a row", async () => {
    const { getAclForObject } = await import("../../../main/ts/api/developer/aclApi");
    (getAclForObject as ReturnType<typeof vi.fn>).mockResolvedValue(defaultAclPayload);

    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-table")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("developer-ct-acl-add-name"), {
      target: { value: "Default" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-acl-add-type"), {
      target: { value: "ROLE" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-acl-add"));

    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-error").textContent).toMatch(
        /already exists/i,
      );
    });
    expect(screen.queryByTestId("developer-ct-acl-row-__new:1")).toBeNull();
  });

  it("creates an ACL when object has none (404)", async () => {
    const { getAclForObject, createObjectAcl } = await import(
      "../../../main/ts/api/developer/aclApi"
    );
    (getAclForObject as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      Object.assign(new Error("not found"), { status: 404 }),
    );
    (createObjectAcl as ReturnType<typeof vi.fn>).mockClear();

    render(<DeveloperShell embedded />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-row"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-acl-create-form")).toBeTruthy();

    fireEvent.change(screen.getByTestId("developer-ct-acl-owner-name"), {
      target: { value: "admin" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-acl-owner-type"), {
      target: { value: "USER" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-acl-create"));

    await waitFor(() => {
      expect(createObjectAcl).toHaveBeenCalledWith(
        "0-2-301",
        expect.objectContaining({ name: "admin", type: "USER" }),
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-acl-row-id:100")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-notice").textContent).toMatch(/saved/i);
    });
  });
});

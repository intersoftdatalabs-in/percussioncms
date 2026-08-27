/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import * as fieldRulesApi from "../../../main/ts/api/developer/contentTypeFieldRules";
import { catalogColors } from "../../../main/ts/developer/catalogStyles";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ContentTypeDetailPanel } from "../../../main/ts/developer/ContentTypeDetailPanel";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  getContentTypeDetail: vi.fn(),
  updateContentTypeDetail: vi.fn(),
  setContentTypeEnabled: vi.fn(),
  setContentTypeAllowedWorkflows: vi.fn(),
  lockContentType: vi.fn(),
  unlockContentType: vi.fn(),
  getContentTypeAllowedTemplates: vi.fn(),
  replaceContentTypeAllowedTemplates: vi.fn(),
  getFieldControlProperties: vi.fn(),
  replaceFieldControlProperties: vi.fn(),
  getContentTypeItemExits: vi.fn(),
  replaceContentTypeItemExits: vi.fn(),
}));

vi.mock("../../../main/ts/api/developer/contentTypeFieldRules", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypeFieldRules")>();
  return {
    ...actual,
    getContentTypeFieldRuleExpressions: vi.fn(),
    replaceContentTypeFieldRuleExpressions: vi.fn(),
  };
});

// ObjectAclSection loads ACL via separate API; stub so detail success path stays isolated.
vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: (props: {
    objectGuid?: string | null;
    objectKind?: string | null;
    testIdPrefix?: string;
  }) => (
    <div
      data-testid={`${props.testIdPrefix ?? "developer-acl"}-stub`}
      data-object-guid={props.objectGuid ?? ""}
      data-object-kind={props.objectKind ?? ""}
    />
  ),
}));

const getContentTypeDetail = contentTypesApi.getContentTypeDetail as ReturnType<typeof vi.fn>;
const updateContentTypeDetail = contentTypesApi.updateContentTypeDetail as ReturnType<
  typeof vi.fn
>;
const setContentTypeEnabled = contentTypesApi.setContentTypeEnabled as ReturnType<typeof vi.fn>;
const setContentTypeAllowedWorkflows = contentTypesApi.setContentTypeAllowedWorkflows as ReturnType<
  typeof vi.fn
>;
const lockContentType = contentTypesApi.lockContentType as ReturnType<typeof vi.fn>;
const unlockContentType = contentTypesApi.unlockContentType as ReturnType<typeof vi.fn>;
const getContentTypeAllowedTemplates =
  contentTypesApi.getContentTypeAllowedTemplates as ReturnType<typeof vi.fn>;
const replaceContentTypeAllowedTemplates =
  contentTypesApi.replaceContentTypeAllowedTemplates as ReturnType<typeof vi.fn>;
const getFieldControlProperties = contentTypesApi.getFieldControlProperties as ReturnType<
  typeof vi.fn
>;
const replaceFieldControlProperties =
  contentTypesApi.replaceFieldControlProperties as ReturnType<typeof vi.fn>;
const getContentTypeItemExits = contentTypesApi.getContentTypeItemExits as ReturnType<typeof vi.fn>;
const replaceContentTypeItemExits =
  contentTypesApi.replaceContentTypeItemExits as ReturnType<typeof vi.fn>;
const getContentTypeFieldRuleExpressions =
  fieldRulesApi.getContentTypeFieldRuleExpressions as ReturnType<typeof vi.fn>;
const replaceContentTypeFieldRuleExpressions =
  fieldRulesApi.replaceContentTypeFieldRuleExpressions as ReturnType<typeof vi.fn>;

const emptyItemExits = {
  inputTranslations: [] as Array<{ extension?: string; parameters?: Array<{ value?: string }> }>,
  outputTranslations: [] as Array<{ extension?: string }>,
  validations: [] as Array<{ extension?: string }>,
  preExits: [] as Array<{ extension?: string }>,
  postExits: [] as Array<{ extension?: string }>,
};

const sampleDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  hideFromMenu: false,
  appName: "rx_cm",
  guid: { stringValue: "0-2-301" },
  fields: [],
  allowedWorkflows: [],
  allowedTemplates: [],
  designGaps: [] as Array<string | { code?: string; message?: string }>,
};

describe("ContentTypeDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getContentTypeDetail.mockReset();
    updateContentTypeDetail.mockReset();
    setContentTypeEnabled.mockReset();
    setContentTypeAllowedWorkflows.mockReset();
    lockContentType.mockReset();
    unlockContentType.mockReset();
    getContentTypeAllowedTemplates.mockReset();
    replaceContentTypeAllowedTemplates.mockReset();
    getFieldControlProperties.mockReset();
    replaceFieldControlProperties.mockReset();
    getContentTypeItemExits.mockReset();
    replaceContentTypeItemExits.mockReset();
    getContentTypeFieldRuleExpressions.mockReset();
    replaceContentTypeFieldRuleExpressions.mockReset();
    lockContentType.mockResolvedValue({ locker: "Admin", remainingTime: 30 });
    unlockContentType.mockResolvedValue(undefined);
    updateContentTypeDetail.mockImplementation(async (_id, body) => ({
      ...sampleDetail,
      ...body,
    }));
    setContentTypeEnabled.mockImplementation(async (_id, enabled: boolean) => ({
      ...sampleDetail,
      enabled,
    }));
    setContentTypeAllowedWorkflows.mockImplementation(async (_id, body) => ({
      ...sampleDetail,
      allowedWorkflows: body.allowedWorkflows,
      defaultWorkflow: body.defaultWorkflow ?? null,
    }));
    replaceContentTypeAllowedTemplates.mockImplementation(async (_id, templates) => templates);
    getContentTypeAllowedTemplates.mockImplementation(async () => []);
    getFieldControlProperties.mockResolvedValue({ properties: [] });
    replaceFieldControlProperties.mockImplementation(async (_id, _field, properties) => ({
      properties,
    }));
    getContentTypeItemExits.mockResolvedValue({ ...emptyItemExits });
    replaceContentTypeItemExits.mockImplementation(async (_id, body) => body);
    getContentTypeFieldRuleExpressions.mockResolvedValue({
      fieldName: "sys_title",
      validation: [],
      visibility: [],
      inputTranslation: [],
      outputTranslation: [],
    });
    replaceContentTypeFieldRuleExpressions.mockImplementation(async (_id, fieldName, body) => ({
      fieldName,
      validation: body.validation ?? [],
      visibility: body.visibility ?? [],
      inputTranslation: body.inputTranslation ?? [],
      outputTranslation: body.outputTranslation ?? [],
    }));
  });

  it("renders lock toolbar while detail is loading so workflow lock is findable (#3835)", async () => {
    let resolveDetail: (value: typeof sampleDetail) => void = () => undefined;
    getContentTypeDetail.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveDetail = resolve;
        }),
    );
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    expect(screen.getByTestId("developer-ct-lock-toolbar")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-control-props")).toBeTruthy();
    expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-cp-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect(screen.queryByTestId("developer-ct-wf-add-name")).toBeNull();
    resolveDetail({
      ...sampleDetail,
      allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
      defaultWorkflow: { name: "Simple Workflow", isDefault: true },
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    expect((screen.getByTestId("developer-ct-wf-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-wf-add") as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
  });

  it("loads detail on success and supports back", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-title").textContent).toContain("Page");
    expect(screen.getByTestId("developer-ct-detail-name").textContent).toBe("percPage");
    expect(screen.getByTestId("developer-ct-fields-table")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
    const back = screen.getByTestId("developer-ct-back");
    expect(back.getAttribute("aria-label")).toBe("Back to list");
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty workflows and templates when detail has none", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [],
      allowedTemplates: [],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue(new SessionRedirectError());
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ct-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-ct-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      `${DEV_MSG.CT_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getContentTypeDetail.mockRejectedValue(new Error("network down"));
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      `${DEV_MSG.CT_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ct-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getContentTypeDetail.mockRejectedValue("boom");
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toBe(
      DEV_MSG.CT_DETAIL_ERROR,
    );
  });

  it("mounts ObjectAclSection with content-type kind and object guid (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    const acl = screen.getByTestId("developer-ct-acl-stub");
    expect(acl.getAttribute("data-object-kind")).toBe("content-type");
    expect(acl.getAttribute("data-object-guid")).toBe("0-2-301");
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-301");
  });

  it("uses catalogGuid fallback when detail guid has no stringValue (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
    });
    render(
      <ContentTypeDetailPanel
        idOrName="percPage"
        catalogGuid="0-2-9"
        onBack={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-9");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-9",
    );
  });

  it("uses guidString when nested guid is absent (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: "0-2-88",
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-88");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-88",
    );
  });

  it("synthesizes object guid from host/type/uuid parts on detail (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: { hostId: 0, type: 2, uuid: 301 },
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("0-2-301");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-301",
    );
  });

  it("passes empty guid to ObjectAclSection when none can be resolved (#3319)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      guid: undefined,
      guidString: undefined,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-guid").textContent).toBe("—");
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe("");
  });

  it("renders structured designGaps message with data-gap-code (REST-GAPS-01)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      designGaps: [
        { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
        "legacy free-text gap",
      ],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-gaps")).toBeTruthy();
    });
    const gaps = screen.getByTestId("developer-ct-gaps");
    expect(gaps.textContent).toContain("Item-level pre/post exits not exposed");
    expect(gaps.textContent).toContain("legacy free-text gap");
    const coded = gaps.querySelector('[data-gap-code="CT_ITEM_EXITS"]');
    expect(coded).toBeTruthy();
    expect(coded?.textContent).toBe("Item-level pre/post exits not exposed");
  });

  it("unwraps singleton association objects and JAXB DesignGap envelope (#3712)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      name: "percArchiveList",
      label: "Archive",
      fields: { empty: false },
      childFieldSets: "rx_shared",
      allowedWorkflows: { name: "Simple Workflow", label: "Simple Workflow", isDefault: true },
      allowedTemplates: {
        NamedObjectRef: { name: "perc.page", label: "Page" },
      },
      designGaps: {
        DesignGap: { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
      },
    });
    render(<ContentTypeDetailPanel idOrName="percArchiveList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-detail-title").textContent).toContain("Archive");
    expect(screen.getByTestId("developer-ct-wf-row-0").textContent).toContain("Simple Workflow");
    expect(screen.getByTestId("developer-ct-tpl-row-0").textContent).toContain("perc.page");
    expect(screen.getByTestId("developer-ct-child-sets").textContent).toContain("rx_shared");
    expect(screen.getByTestId("developer-ct-gaps").textContent).toContain(
      "Item-level pre/post exits not exposed",
    );
    expect(
      screen.getByTestId("developer-ct-gaps").querySelector('[data-gap-code="CT_ITEM_EXITS"]'),
    ).toBeTruthy();
    expect(screen.queryByTestId("developer-ct-detail-error")).toBeNull();
  });

  it("does not crash when designGaps is a Jackson empty-collection bean (#3712)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: { empty: false },
      allowedWorkflows: { empty: true },
      allowedTemplates: { empty: true },
      designGaps: { empty: true },
    });
    render(<ContentTypeDetailPanel idOrName="percArchiveList" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-wf-empty")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
    expect(screen.queryByTestId("developer-ct-gaps")).toBeNull();
    expect(screen.queryByTestId("developer-ct-detail-error")).toBeNull();
  });

  it("keeps description read-only and save disabled until lock (#3744)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-description")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-unlock") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect(screen.getByTestId("developer-ct-lock").getAttribute("aria-label")).toBe("Lock");
    expect(screen.getByTestId("developer-ct-unlock").getAttribute("aria-label")).toBe("Unlock");
    expect(screen.getByTestId("developer-ct-save").getAttribute("aria-label")).toBe(
      "Save content type",
    );
  });

  it("locks, saves a description, then unlocks without wrapping PUT (#3744)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect(lockContentType).toHaveBeenCalledWith("percPage");
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
    fireEvent.change(screen.getByTestId("developer-ct-description"), {
      target: { value: "Updated description" },
    });
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(updateContentTypeDetail).toHaveBeenCalled();
    });
    expect(updateContentTypeDetail).toHaveBeenCalledWith(
      "percPage",
      expect.objectContaining({ description: "Updated description" }),
    );
    const saveBody = updateContentTypeDetail.mock.calls.at(-1)?.[1] as {
      enabled?: boolean;
      allowedTemplates?: unknown;
    };
    expect(saveBody.enabled).toBeUndefined();
    expect(saveBody.allowedTemplates).toBeUndefined();
    expect(setContentTypeEnabled).not.toHaveBeenCalled();
    expect(replaceContentTypeAllowedTemplates).not.toHaveBeenCalled();
    expect(unlockContentType).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    fireEvent.click(screen.getByTestId("developer-ct-unlock"));
    await waitFor(() => {
      expect(unlockContentType).toHaveBeenCalledWith("percPage");
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    });
    expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  it("surfaces lock errors and does not enable save (#3744)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    lockContentType.mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: null });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not lock content type.",
      );
    });
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).disabled).toBe(true);
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    fireEvent.click(screen.getByTestId("developer-ct-enabled"));
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).checked).toBe(true);
  });

  it("clears the held lock when save returns 409 (#3744)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    updateContentTypeDetail.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-description"), {
      target: { value: "Will fail" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  it("locks, replaces allowed templates via dedicated PUT then GET (#3783)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedTemplates: [{ name: "perc.page", label: "Page" }],
    });
    replaceContentTypeAllowedTemplates.mockResolvedValueOnce([]);
    getContentTypeAllowedTemplates.mockResolvedValueOnce([]);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-row-0")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.click(screen.getByTestId("developer-ct-tpl-remove-0"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-empty")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(replaceContentTypeAllowedTemplates).toHaveBeenCalledWith("percPage", []);
    });
    expect(getContentTypeAllowedTemplates).toHaveBeenCalledWith("percPage");
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
  });

  it("adds an existing template id after lock and PUTs the new set (#3783)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedTemplates: [{ name: "perc.page", label: "Page" }],
    });
    const next = [
      { name: "perc.page" },
      { name: "perc.page.summary", label: "perc.page.summary" },
    ];
    replaceContentTypeAllowedTemplates.mockResolvedValueOnce(next);
    getContentTypeAllowedTemplates.mockResolvedValueOnce(next);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
        false,
      );
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
      expect(replaceContentTypeAllowedTemplates).toHaveBeenCalled();
    });
    expect(replaceContentTypeAllowedTemplates).toHaveBeenCalledWith(
      "percPage",
      expect.arrayContaining([
        expect.objectContaining({ name: "perc.page" }),
        expect.objectContaining({ name: "perc.page.summary" }),
      ]),
    );
    expect(getContentTypeAllowedTemplates).toHaveBeenCalledWith("percPage");
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-row-1").textContent).toContain(
        "perc.page.summary",
      );
    });
  });

  it("clears the held lock when allowedTemplates PUT returns 409 (#3783)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedTemplates: [{ name: "perc.page", label: "Page" }],
    });
    replaceContentTypeAllowedTemplates.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-tpl-remove-0") as HTMLButtonElement).disabled).toBe(
        false,
      );
    });
    fireEvent.click(screen.getByTestId("developer-ct-tpl-remove-0"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  it("renders name, disabled template add, and lock toolbar while detail is loading (#3836)", async () => {
    let resolveDetail: (value: typeof sampleDetail) => void = () => undefined;
    getContentTypeDetail.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveDetail = resolve;
        }),
    );
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    expect(screen.getByTestId("developer-ct-lock-toolbar")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-detail-name").textContent).toBe("percPage");
    expect(screen.getByTestId("developer-ct-templates")).toBeTruthy();
    expect(screen.queryByTestId("developer-ct-tpl-empty")).toBeNull();
    const toolbarBg = (screen.getByTestId("developer-ct-lock-toolbar") as HTMLElement).style
      .background;
    expect(
      toolbarBg === catalogColors.surface ||
        /rgb\(\s*255\s*,\s*255\s*,\s*255\s*\)/i.test(toolbarBg),
    ).toBe(true);
    expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-tpl-add") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(true);
    resolveDetail({
      ...sampleDetail,
      allowedTemplates: [{ name: "perc.page", label: "Page" }],
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    expect(screen.getByTestId("developer-ct-tpl-row-0")).toBeTruthy();
    expect(screen.queryByTestId("developer-ct-tpl-empty")).toBeNull();
    expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
  });

  it("ignores template add/remove while unlocked (#3836)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedTemplates: [{ name: "perc.page", label: "Page" }],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-tpl-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-tpl-add-name"), {
      target: { value: "perc.page.summary" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-tpl-add"));
    fireEvent.click(screen.getByTestId("developer-ct-tpl-remove-0"));
    expect(screen.queryByTestId("developer-ct-tpl-row-1")).toBeNull();
    expect(screen.getByTestId("developer-ct-tpl-row-0").textContent).toContain("perc.page");
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("keeps template editors disabled after 409 lock and does not steal (#3836)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedTemplates: [{ name: "perc.page", label: "Page" }],
    });
    lockContentType.mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: null });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not lock content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-tpl-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("unlocks on back when the session holds the lock (#3744)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={onBack} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect(lockContentType).toHaveBeenCalled();
    });
    fireEvent.click(screen.getByTestId("developer-ct-back"));
    await waitFor(() => {
      expect(unlockContentType).toHaveBeenCalledWith("percPage");
    });
    expect(onBack).toHaveBeenCalled();
  });

  it("renders lock toolbar and disabled enabled chrome while detail is loading (#3834)", async () => {
    let resolveDetail: (value: typeof sampleDetail) => void = () => undefined;
    getContentTypeDetail.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveDetail = resolve;
        }),
    );
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    expect(screen.getByTestId("developer-ct-lock-toolbar")).toBeTruthy();
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    resolveDetail(sampleDetail);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).disabled).toBe(true);
  });

  it("keeps the enabled toggle disabled until lock (#3781)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-enabled")).toBeTruthy();
    });
    const box = screen.getByTestId("developer-ct-enabled") as HTMLInputElement;
    expect(box.disabled).toBe(true);
    expect(box.checked).toBe(true);
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect(setContentTypeEnabled).not.toHaveBeenCalled();
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
  });

  it("saves enabled via dedicated PUT after lock (#3781)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.click(screen.getByTestId("developer-ct-enabled"));
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).checked).toBe(false);
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(setContentTypeEnabled).toHaveBeenCalledWith("percPage", false);
    });
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    expect(unlockContentType).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
  });

  it("saves enabled with dedicated PUT first then description with bulk PUT (#3781)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-description"), {
      target: { value: "Updated description" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-enabled"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(setContentTypeEnabled).toHaveBeenCalledWith("percPage", false);
    });
    expect(updateContentTypeDetail).toHaveBeenCalledWith(
      "percPage",
      expect.objectContaining({ description: "Updated description" }),
    );
    const saveBody = updateContentTypeDetail.mock.calls.at(-1)?.[1] as {
      enabled?: boolean;
    };
    expect(saveBody.enabled).toBeUndefined();
    expect(setContentTypeEnabled.mock.invocationCallOrder[0]).toBeLessThan(
      updateContentTypeDetail.mock.invocationCallOrder[0],
    );
  });

  it("does not bulk PUT when enabled PUT fails (#3781)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    setContentTypeEnabled.mockRejectedValueOnce({
      status: 500,
      statusText: "Error",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-description"), {
      target: { value: "Updated description" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-enabled"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(setContentTypeEnabled).toHaveBeenCalled();
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
  });

  it("keeps enabled from dedicated PUT when bulk PUT fails (#3781)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    updateContentTypeDetail.mockRejectedValueOnce({
      status: 500,
      statusText: "Error",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-description"), {
      target: { value: "Updated description" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-enabled"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(setContentTypeEnabled).toHaveBeenCalledWith("percPage", false);
    expect(updateContentTypeDetail).toHaveBeenCalled();
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).checked).toBe(false);
    expect((screen.getByTestId("developer-ct-description") as HTMLInputElement).value).toBe(
      "Updated description",
    );
  });

  it("clears the held lock when enabled PUT returns 409 (#3781)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    setContentTypeEnabled.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.click(screen.getByTestId("developer-ct-enabled"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-enabled") as HTMLInputElement).disabled).toBe(true);
  });

  it("ignores workflow add/remove while unlocked (#3835)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
      defaultWorkflow: { name: "Simple Workflow", isDefault: true },
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-wf-add-name"), {
      target: { value: "Standard Workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-wf-add"));
    fireEvent.click(screen.getByTestId("developer-ct-wf-remove-0"));
    expect(screen.queryByTestId("developer-ct-wf-row-1")).toBeNull();
    expect(screen.getByTestId("developer-ct-wf-row-0").textContent).toContain("Simple Workflow");
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("keeps workflow editors disabled until lock (#3782)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
      defaultWorkflow: { name: "Simple Workflow", isDefault: true },
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-row-0")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-ct-wf-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-wf-add") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-wf-remove-0") as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("saves allowed workflows via CD-08 PUT without bulk PUT or unlock (#3782)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [{ name: "Simple Workflow", label: "Simple Workflow", isDefault: true }],
      defaultWorkflow: { name: "Simple Workflow", isDefault: true },
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-wf-add-name") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-wf-add-name"), {
      target: { value: "Standard Workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-wf-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-wf-row-1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(setContentTypeAllowedWorkflows).toHaveBeenCalled();
    });
    expect(setContentTypeAllowedWorkflows).toHaveBeenCalledWith(
      "percPage",
      expect.objectContaining({
        allowedWorkflows: expect.arrayContaining([
          expect.objectContaining({ name: "Simple Workflow" }),
          expect.objectContaining({ name: "Standard Workflow" }),
        ]),
        defaultWorkflow: expect.objectContaining({ name: "Simple Workflow" }),
      }),
    );
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    expect(unlockContentType).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
  });

  it("blocks save without a held lock (#3782)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-save")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    expect(setContentTypeAllowedWorkflows).not.toHaveBeenCalled();
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    expect(lockContentType).not.toHaveBeenCalled();
  });

  it("clears the held lock when allowedWorkflows PUT returns 409 (#3782)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      allowedWorkflows: [{ name: "Simple Workflow", isDefault: true }],
    });
    setContentTypeAllowedWorkflows.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-wf-add-name") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-wf-add-name"), {
      target: { value: "Standard Workflow" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-wf-add"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect(lockContentType).toHaveBeenCalledTimes(1);
  });

  it("renders control property chrome while detail is loading (#3894)", async () => {
    let resolveDetail: (value: typeof sampleDetail) => void = () => undefined;
    getContentTypeDetail.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveDetail = resolve;
        }),
    );
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    expect(screen.getByTestId("developer-ct-control-props")).toBeTruthy();
    expect((screen.getByTestId("developer-ct-cp-field") as HTMLSelectElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-cp-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-cp-add") as HTMLButtonElement).disabled).toBe(true);
    resolveDetail({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    await waitFor(() => {
      expect(getFieldControlProperties).toHaveBeenCalledWith("percPage", "sys_title");
    });
    expect((screen.getByTestId("developer-ct-cp-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  it("keeps a control-properties GET 404 in the section, not the panel banner (#3894)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title" }],
    });
    getFieldControlProperties.mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-cp-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-cp-error").textContent).toContain(
      "Could not load control property values.",
    );
    expect(screen.queryByTestId("developer-ct-detail-error")).toBeNull();
    expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
  });

  it("keeps control property editors disabled until lock (#3894)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title" }],
    });
    getFieldControlProperties.mockResolvedValue({
      fieldName: "sys_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-cp-row-0")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-ct-cp-value-0") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-cp-add-name") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-cp-add") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ct-cp-remove-0") as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("ignores control property edits while unlocked (#3894)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title" }],
    });
    getFieldControlProperties.mockResolvedValue({
      properties: [{ name: "height", value: "200" }],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-cp-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-cp-value-0"), {
      target: { value: "201" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-cp-add-name"), {
      target: { value: "width" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-cp-add"));
    fireEvent.click(screen.getByTestId("developer-ct-cp-remove-0"));
    expect((screen.getByTestId("developer-ct-cp-value-0") as HTMLInputElement).value).toBe("200");
    expect(screen.queryByTestId("developer-ct-cp-row-1")).toBeNull();
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
  });

  it("saves control property values via CD-07 PUT then GET without bulk PUT (#3894)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title" }],
    });
    getFieldControlProperties
      .mockResolvedValueOnce({
        fieldName: "sys_title",
        control: "sys_EditBox",
        properties: [{ name: "height", value: "200" }],
      })
      .mockResolvedValueOnce({
        fieldName: "sys_title",
        control: "sys_EditBox",
        properties: [{ name: "height", value: "201" }],
      });
    replaceFieldControlProperties.mockResolvedValueOnce({
      properties: [{ name: "height", value: "201" }],
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-cp-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-cp-value-0") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-cp-value-0"), {
      target: { value: "201" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(replaceFieldControlProperties).toHaveBeenCalledWith("percPage", "sys_title", [
        { name: "height", value: "201" },
      ]);
    });
    expect(getFieldControlProperties).toHaveBeenCalledWith("percPage", "sys_title");
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    expect(unlockContentType).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
    expect((screen.getByTestId("developer-ct-cp-value-0") as HTMLInputElement).value).toBe("201");
  });

  it("adds a control property after lock and PUTs the new set (#3894)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title" }],
    });
    getFieldControlProperties
      .mockResolvedValueOnce({ properties: [{ name: "height", value: "200" }] })
      .mockResolvedValueOnce({
        properties: [
          { name: "height", value: "200" },
          { name: "width", value: "400" },
        ],
      });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-cp-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-cp-add-name") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-cp-add-name"), {
      target: { value: "width" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-cp-add-value"), {
      target: { value: "400" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-cp-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-cp-row-1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(replaceFieldControlProperties).toHaveBeenCalledWith("percPage", "sys_title", [
        { name: "height", value: "200" },
        { name: "width", value: "400" },
      ]);
    });
  });

  it("clears the held lock when controlProperties PUT returns 409 (#3894)", async () => {
    getContentTypeDetail.mockResolvedValue({
      ...sampleDetail,
      fields: [{ name: "sys_title", label: "Title" }],
    });
    getFieldControlProperties.mockResolvedValue({
      properties: [{ name: "height", value: "200" }],
    });
    replaceFieldControlProperties.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-cp-value-0") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-cp-value-0"), {
      target: { value: "201" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-cp-value-0") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect(lockContentType).toHaveBeenCalledTimes(1);
  });

  it("shows item-level exits chrome while loading and keeps editors disabled until lock (#3895)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    expect(screen.getByTestId("developer-ct-item-exits")).toBeTruthy();
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    expect((screen.getByTestId("developer-ct-ie-in-add-fqn") as HTMLInputElement).disabled).toBe(
      true,
    );
    expect((screen.getByTestId("developer-ct-ie-in-add") as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByTestId("developer-ct-ie-in-empty")).toBeTruthy();
  });

  it("ignores item-exit add/remove while unlocked (#3895)", async () => {
    getContentTypeItemExits.mockResolvedValue({
      ...emptyItemExits,
      inputTranslations: [
        { extension: "Java/global/percussion/generic/sys_ToUpperCase", parameters: [{ value: "sys_title" }] },
      ],
    });
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-ie-in-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-ct-ie-in-add-fqn"), {
      target: { value: "Java/global/percussion/generic/sys_ToLowerCase" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-ie-in-add"));
    fireEvent.click(screen.getByTestId("developer-ct-ie-in-remove-0"));
    expect(screen.queryByTestId("developer-ct-ie-in-row-1")).toBeNull();
    expect(screen.getByTestId("developer-ct-ie-in-row-0").textContent).toContain("sys_ToUpperCase");
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect(replaceContentTypeItemExits).not.toHaveBeenCalled();
  });

  it("locks, replaces item-level exits via dedicated PUT (#3895)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    getContentTypeItemExits.mockResolvedValue({ ...emptyItemExits });
    const next = {
      ...emptyItemExits,
      inputTranslations: [
        {
          extension: "Java/global/percussion/generic/sys_ToUpperCase",
          parameters: [{ value: "sys_title" }],
        },
      ],
    };
    replaceContentTypeItemExits.mockResolvedValueOnce(next);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-ie-in-add-fqn") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-ie-in-add-fqn"), {
      target: { value: "Java/global/percussion/generic/sys_ToUpperCase" },
    });
    fireEvent.change(screen.getByTestId("developer-ct-ie-in-add-param"), {
      target: { value: "sys_title" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-ie-in-add"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-ie-in-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(replaceContentTypeItemExits).toHaveBeenCalled();
    });
    const putBody = replaceContentTypeItemExits.mock.calls.at(-1)?.[1] as {
      inputTranslations?: Array<{ extension?: string }>;
    };
    expect(putBody.inputTranslations?.[0]?.extension).toBe(
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    expect(unlockContentType).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
  });

  it("clears the held lock when itemExits PUT returns 409 (#3895)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    replaceContentTypeItemExits.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: null,
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-ie-in-add-fqn") as HTMLInputElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-ie-in-add-fqn"), {
      target: { value: "Java/global/percussion/generic/sys_ToUpperCase" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-ie-in-add"));
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not save content type.",
      );
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-ie-in-add-fqn") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  const sampleWithField = {
    ...sampleDetail,
    fields: [{ name: "sys_title", label: "Title", fieldType: "system" }],
  };

  it("keeps field-rule expression editors disabled until lock (#3896)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleWithField);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-field-rule-expressions")).toBeTruthy();
    });
    await waitFor(() => {
      expect(getContentTypeFieldRuleExpressions).toHaveBeenCalledWith("percPage", "sys_title");
    });
    const validation = screen.getByTestId("developer-ct-fr-validation") as HTMLTextAreaElement;
    expect(validation.disabled).toBe(true);
    fireEvent.change(validation, { target: { value: 'sys_title <> "#nope"' } });
    expect(validation.value).toBe("");
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect(replaceContentTypeFieldRuleExpressions).not.toHaveBeenCalled();
  });

  it("saves field-rule expressions via dedicated PUT after lock (#3896)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleWithField);
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect(getContentTypeFieldRuleExpressions).toHaveBeenCalledWith("percPage", "sys_title");
    });
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-fr-validation") as HTMLTextAreaElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-fr-validation"), {
      target: { value: 'sys_title <> "#3896-field-rule"' },
    });
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(replaceContentTypeFieldRuleExpressions).toHaveBeenCalled();
    });
    const call = replaceContentTypeFieldRuleExpressions.mock.calls.at(-1);
    expect(call?.[0]).toBe("percPage");
    expect(call?.[1]).toBe("sys_title");
    expect(call?.[2].validation?.[0].conditionals?.[0].value).toBe("#3896-field-rule");
    expect(updateContentTypeDetail).not.toHaveBeenCalled();
    expect(unlockContentType).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Locked by you");
  });

  it("clears the held lock when field-rule PUT returns 409 (#3896)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleWithField);
    replaceContentTypeFieldRuleExpressions.mockRejectedValueOnce({
      status: 409,
      statusText: "Conflict",
      body: { message: "Locked by another user" },
    });
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={() => undefined} />);
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-lock") as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect((screen.getByTestId("developer-ct-fr-validation") as HTMLTextAreaElement).disabled).toBe(
        false,
      );
    });
    fireEvent.change(screen.getByTestId("developer-ct-fr-validation"), {
      target: { value: 'sys_title <> "#3896"' },
    });
    fireEvent.click(screen.getByTestId("developer-ct-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-fr-error").textContent).toMatch(
        /Could not save field rule expressions/i,
      );
    });
    expect(screen.getByTestId("developer-ct-detail-error").textContent).toMatch(
      /Could not save content type/i,
    );
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
    expect((screen.getByTestId("developer-ct-fr-validation") as HTMLTextAreaElement).disabled).toBe(
      true,
    );
  });
});

/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ContentTypeDetailPanel } from "../../../main/ts/developer/ContentTypeDetailPanel";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  getContentTypeDetail: vi.fn(),
  updateContentTypeDetail: vi.fn(),
  setContentTypeEnabled: vi.fn(),
  setContentTypeAllowedWorkflows: vi.fn(),
  lockContentType: vi.fn(),
  unlockContentType: vi.fn(),
}));

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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
    };
    expect(saveBody.enabled).toBeUndefined();
    expect(setContentTypeEnabled).not.toHaveBeenCalled();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-lock"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail-error").textContent).toContain(
        "Could not lock content type.",
      );
    });
    expect((screen.getByTestId("developer-ct-save") as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByTestId("developer-ct-lock-status").textContent).toBe("Not locked");
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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

  it("unlocks on back when the session holds the lock (#3744)", async () => {
    getContentTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<ContentTypeDetailPanel idOrName="percPage" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
      expect(screen.getByTestId("developer-ct-lock")).toBeTruthy();
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
});

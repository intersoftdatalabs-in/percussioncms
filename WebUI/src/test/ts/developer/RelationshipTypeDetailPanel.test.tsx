/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as relationshipTypesApi from "../../../main/ts/api/developer/relationshipTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { RelationshipTypeDetailPanel } from "../../../main/ts/developer/RelationshipTypeDetailPanel";

vi.mock("../../../main/ts/api/developer/relationshipTypesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/relationshipTypesApi")
  >();
  return {
    ...actual,
    listRelationshipTypes: vi.fn(),
    getRelationshipTypeDetail: vi.fn(),
    createRelationshipType: vi.fn(),
    updateRelationshipType: vi.fn(),
    deleteRelationshipType: vi.fn(),
  };
});

const getRelationshipTypeDetail = relationshipTypesApi.getRelationshipTypeDetail as ReturnType<
  typeof vi.fn
>;
const createRelationshipType = relationshipTypesApi.createRelationshipType as ReturnType<
  typeof vi.fn
>;
const updateRelationshipType = relationshipTypesApi.updateRelationshipType as ReturnType<
  typeof vi.fn
>;
const deleteRelationshipType = relationshipTypesApi.deleteRelationshipType as ReturnType<
  typeof vi.fn
>;

const sampleDetail = {
  name: "ActiveAssembly",
  label: "Active Assembly",
  category: "rs_activeassembly",
  categoryLabel: "Active Assembly",
  type: "system",
  systemType: true,
  userType: false,
  description: "AA relationship",
  allowCloning: true,
  useOwnerRevision: true,
  useDependentRevision: false,
  effects: [
    {
      name: "sys_PublishRequired",
      activationEndPoint: "owner",
      extensionRef: "Java/global/percussion/relationship/sys_PublishRequired",
    },
  ],
  systemProperties: [{ name: "rs_useownerrevision", value: "yes" }],
  userProperties: [{ name: "slotid", value: "0" }],
  designGaps: ["gap-a"],
};

const sampleUserDetail = {
  name: "MyUserRel",
  label: "My User Rel",
  category: "rs_generic",
  categoryLabel: "Generic",
  type: "user",
  systemType: false,
  userType: true,
  description: "user type",
  allowCloning: false,
  useOwnerRevision: false,
  useDependentRevision: false,
  effects: [],
  systemProperties: [],
  userProperties: [],
  cloneOverrides: [],
  designGaps: ["gap-user"],
};

describe("RelationshipTypeDetailPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getRelationshipTypeDetail.mockReset();
    createRelationshipType.mockReset();
    updateRelationshipType.mockReset();
    deleteRelationshipType.mockReset();
  });

  it("loads system detail as read-only (no save/delete)", async () => {
    getRelationshipTypeDetail.mockResolvedValue(sampleDetail);
    const onBack = vi.fn();
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-title").textContent).toContain(
      "Active Assembly",
    );
    expect(screen.getByTestId("developer-rt-system-readonly")).toBeTruthy();
    expect(screen.queryByTestId("developer-rt-save")).toBeNull();
    expect(screen.queryByTestId("developer-rt-delete")).toBeNull();
    expect(screen.getByTestId("developer-rt-label")).toBeDisabled();
    expect(screen.getByTestId("developer-rt-effects-table")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-sysprops-table")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-userprops-table")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-gaps").textContent).toContain("gap-a");
    expect(screen.getByTestId("developer-rt-effect-cond-empty-0")).toBeTruthy();
    expect(screen.queryByTestId("developer-rt-effect-cond-add-0")).toBeNull();
    expect(screen.getByTestId("developer-rt-clone-overrides")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-clone-empty")).toBeTruthy();
    expect(screen.queryByTestId("developer-rt-clone-add")).toBeNull();
    expect(getRelationshipTypeDetail).toHaveBeenCalledWith("ActiveAssembly");
    fireEvent.click(screen.getByTestId("developer-rt-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("creates a user relationship type with category", async () => {
    createRelationshipType.mockResolvedValue(sampleUserDetail);
    const onSaved = vi.fn();
    render(
      <RelationshipTypeDetailPanel
        idOrName={null}
        catalog={[sampleDetail]}
        onBack={() => undefined}
        onSaved={onSaved}
      />,
    );
    expect(screen.getByTestId("developer-rt-detail-title").textContent).toContain(
      DEV_MSG.RT_NEW,
    );
    const saveBtn = screen.getByTestId("developer-rt-save");
    expect(saveBtn).toBeDisabled();
    fireEvent.change(screen.getByTestId("developer-rt-name"), {
      target: { value: "MyUserRel" },
    });
    fireEvent.change(screen.getByTestId("developer-rt-category"), {
      target: { value: "rs_generic" },
    });
    fireEvent.change(screen.getByTestId("developer-rt-label"), {
      target: { value: "My User Rel" },
    });
    expect(saveBtn).not.toBeDisabled();
    fireEvent.click(saveBtn);
    await waitFor(() => {
      expect(createRelationshipType).toHaveBeenCalled();
    });
    expect(createRelationshipType.mock.calls[0][0]).toMatchObject({
      name: "MyUserRel",
      category: "rs_generic",
      label: "My User Rel",
    });
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(screen.getByTestId("developer-rt-editor-notice").textContent).toBe(DEV_MSG.RT_SAVED);
  });

  it("disables save until a user relationship type is dirty", async () => {
    getRelationshipTypeDetail.mockResolvedValue(sampleUserDetail);
    render(
      <RelationshipTypeDetailPanel idOrName="MyUserRel" onBack={() => undefined} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-save")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-save")).toBeDisabled();
    fireEvent.change(screen.getByTestId("developer-rt-label"), {
      target: { value: "Changed" },
    });
    expect(screen.getByTestId("developer-rt-save")).not.toBeDisabled();
  });

  it("updates and deletes a user relationship type", async () => {
    getRelationshipTypeDetail.mockResolvedValue(sampleUserDetail);
    updateRelationshipType.mockResolvedValue({
      ...sampleUserDetail,
      label: "Updated",
    });
    deleteRelationshipType.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <RelationshipTypeDetailPanel
        idOrName="MyUserRel"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-save")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-rt-system-readonly")).toBeNull();
    expect(screen.getByTestId("developer-rt-save")).toBeDisabled();
    fireEvent.change(screen.getByTestId("developer-rt-label"), {
      target: { value: "Updated" },
    });
    expect(screen.getByTestId("developer-rt-save")).not.toBeDisabled();
    fireEvent.click(screen.getByTestId("developer-rt-save"));
    await waitFor(() => {
      expect(updateRelationshipType).toHaveBeenCalledWith(
        "MyUserRel",
        expect.objectContaining({ label: "Updated" }),
      );
    });

    fireEvent.click(screen.getByTestId("developer-rt-delete"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-catalog-confirm-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(deleteRelationshipType).toHaveBeenCalledWith("MyUserRel");
    });
    expect(onDeleted).toHaveBeenCalled();
  });

  it("saves effect conditions and execution contexts on a user type", async () => {
    const withEffect = {
      ...sampleUserDetail,
      effects: [
        {
          name: "sys_Literal",
          extensionRef: "Java/global/percussion/generic/sys_Literal",
          activationEndPoint: "owner",
          conditions: [],
          executionContexts: [],
        },
      ],
    };
    getRelationshipTypeDetail.mockResolvedValue(withEffect);
    updateRelationshipType.mockResolvedValue({
      ...withEffect,
      effects: [
        {
          ...withEffect.effects[0],
          conditions: [{ variable: "sys_title", operator: "=", value: "yes" }],
          executionContexts: ["PreConstruction"],
        },
      ],
    });
    render(<RelationshipTypeDetailPanel idOrName="MyUserRel" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-effect-cond-add-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-rt-effect-cond-add-0"));
    fireEvent.change(screen.getByTestId("developer-rt-effect-cond-var-0-0"), {
      target: { value: "sys_title" },
    });
    fireEvent.change(screen.getByTestId("developer-rt-effect-cond-value-0-0"), {
      target: { value: "yes" },
    });
    fireEvent.click(screen.getByTestId("developer-rt-effect-ctx-0-PreConstruction"));
    fireEvent.click(screen.getByTestId("developer-rt-save"));
    await waitFor(() => {
      expect(updateRelationshipType).toHaveBeenCalled();
    });
    const body = updateRelationshipType.mock.calls[0][1] as {
      effects?: Array<{
        conditions?: Array<{ variable?: string; value?: string }>;
        executionContexts?: string[];
      }>;
    };
    expect(body.effects?.[0]?.conditions?.[0]?.variable).toBe("sys_title");
    expect(body.effects?.[0]?.conditions?.[0]?.value).toBe("yes");
    expect(body.effects?.[0]?.executionContexts).toContain("PreConstruction");
  });

  it("blocks save when an effect condition is incomplete", async () => {
    const withEffect = {
      ...sampleUserDetail,
      effects: [
        {
          name: "sys_Literal",
          extensionRef: "Java/global/percussion/generic/sys_Literal",
          activationEndPoint: "owner",
          conditions: [],
          executionContexts: [],
        },
      ],
    };
    getRelationshipTypeDetail.mockResolvedValue(withEffect);
    render(<RelationshipTypeDetailPanel idOrName="MyUserRel" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-effect-cond-add-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-rt-effect-cond-add-0"));
    fireEvent.change(screen.getByTestId("developer-rt-effect-cond-value-0-0"), {
      target: { value: "yes" },
    });
    expect(screen.getByTestId("developer-rt-effect-incomplete")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-save")).toBeDisabled();
  });

  it("shows empty effects/props sections when detail has none", async () => {
    getRelationshipTypeDetail.mockResolvedValue({
      ...sampleDetail,
      effects: [],
      systemProperties: [],
      userProperties: [],
    });
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-effects")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-effects").textContent).toContain(DEV_MSG.RT_NONE);
    expect(screen.getByTestId("developer-rt-sysprops").textContent).toContain(DEV_MSG.RT_NONE);
    expect(screen.getByTestId("developer-rt-userprops").textContent).toContain(DEV_MSG.RT_NONE);
    expect(screen.queryByTestId("developer-rt-effects-table")).toBeNull();
    expect(screen.queryByTestId("developer-rt-sysprops-table")).toBeNull();
    expect(screen.queryByTestId("developer-rt-userprops-table")).toBeNull();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getRelationshipTypeDetail.mockRejectedValue(new SessionRedirectError());
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-rt-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-rt-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getRelationshipTypeDetail.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      `${DEV_MSG.RT_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getRelationshipTypeDetail.mockRejectedValue(new Error("network down"));
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      `${DEV_MSG.RT_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-rt-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getRelationshipTypeDetail.mockRejectedValue("boom");
    render(<RelationshipTypeDetailPanel idOrName="ActiveAssembly" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-detail-error").textContent).toBe(
      DEV_MSG.RT_DETAIL_ERROR,
    );
  });

  it("saves cloning field overrides on a user type", async () => {
    getRelationshipTypeDetail.mockResolvedValue(sampleUserDetail);
    updateRelationshipType.mockResolvedValue({
      ...sampleUserDetail,
      cloneOverrides: [
        {
          fieldName: "sys_title",
          extensionRef: "Java/global/percussion/generic/sys_Literal",
          extensionParams: ["cloned"],
        },
      ],
    });
    render(<RelationshipTypeDetailPanel idOrName="MyUserRel" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-clone-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-rt-clone-add"));
    fireEvent.change(screen.getByTestId("developer-rt-clone-field-0"), {
      target: { value: "sys_title" },
    });
    fireEvent.change(screen.getByTestId("developer-rt-clone-ext-0"), {
      target: { value: "Java/global/percussion/generic/sys_Literal" },
    });
    fireEvent.change(screen.getByTestId("developer-rt-clone-params-0"), {
      target: { value: "cloned" },
    });
    expect(screen.getByTestId("developer-rt-save")).not.toBeDisabled();
    fireEvent.click(screen.getByTestId("developer-rt-save"));
    await waitFor(() => {
      expect(updateRelationshipType).toHaveBeenCalledWith(
        "MyUserRel",
        expect.objectContaining({
          cloneOverrides: [
            {
              fieldName: "sys_title",
              extensionRef: "Java/global/percussion/generic/sys_Literal",
              extensionParams: ["cloned"],
            },
          ],
        }),
      );
    });
  });

  it("clears cloning field overrides with cloneOverrides:[] and ignores JAXB empty bean (#4527)", async () => {
    getRelationshipTypeDetail.mockResolvedValue({
      ...sampleUserDetail,
      cloneOverrides: [
        {
          fieldName: "sys_title",
          extensionRef: "Java/global/percussion/generic/sys_Literal",
          extensionParams: ["cloned"],
        },
      ],
    });
    updateRelationshipType.mockResolvedValue({
      ...sampleUserDetail,
      // JAXB empty collection must not rehydrate a phantom row after clear-save.
      cloneOverrides: {},
    });
    render(<RelationshipTypeDetailPanel idOrName="MyUserRel" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-clone-field-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-rt-clone-remove-0"));
    expect(screen.getByTestId("developer-rt-clone-empty")).toBeTruthy();
    expect(screen.getByTestId("developer-rt-save")).not.toBeDisabled();
    fireEvent.click(screen.getByTestId("developer-rt-save"));
    await waitFor(() => {
      expect(updateRelationshipType).toHaveBeenCalledWith(
        "MyUserRel",
        expect.objectContaining({ cloneOverrides: [], clearCloneOverrides: true }),
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-clone-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-rt-clone-field-0")).toBeNull();
  });
});

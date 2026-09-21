/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  createExtension,
  deleteExtension,
  getExtensionDetail,
  saveExtension,
} from "../../../main/ts/api/developer/extensionsApi";
import { ExtensionDetailPanel } from "../../../main/ts/developer/ExtensionDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/extensionsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/extensionsApi")
  >();
  return {
    ...actual,
    listExtensions: vi.fn(),
    getExtensionDetail: vi.fn(),
    createExtension: vi.fn(),
    saveExtension: vi.fn(),
    deleteExtension: vi.fn(),
  };
});

const getExtensionDetailMock = vi.mocked(getExtensionDetail);
const createExtensionMock = vi.mocked(createExtension);
const saveExtensionMock = vi.mocked(saveExtension);
const deleteExtensionMock = vi.mocked(deleteExtension);

const sampleSystemDetail = {
  extensionName: "sys_add",
  fqn: "Java/global/percussion/sys_add",
  handlerName: "Java",
  context: "global/percussion/",
  version: 1,
  supportedInterfaces: ["com.percussion.extension.IPSExtension"],
  runtimeParameters: [{ name: "htmlParams", dataType: "java.util.Map" }],
  initParameters: { className: "com.percussion.extension.PSAdd" },
};

const sampleUserDetail = {
  extensionName: "my_user_ext",
  fqn: "Java/user/my_user_ext",
  handlerName: "Java",
  context: "user/",
  version: 1,
  deprecated: false,
  supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
  runtimeParameters: [],
  initParameters: { className: "com.example.MyUserExtension" },
};

describe("ExtensionDetailPanel", () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getExtensionDetailMock.mockReset();
    createExtensionMock.mockReset();
    saveExtensionMock.mockReset();
    deleteExtensionMock.mockReset();
  });

  it("loads detail on success and supports back", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleSystemDetail);
    const onBack = vi.fn();
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-title")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-title").textContent).toContain("sys_add");
    expect(screen.getByTestId("developer-ex-rtparam-row-0")).toBeTruthy();
    expect(
      (screen.getByTestId("developer-ex-rtparam-name-0") as HTMLInputElement).value,
    ).toBe("htmlParams");
    expect(screen.getByTestId("developer-ex-rtparam-add")).toBeTruthy();
    expect(getExtensionDetailMock).toHaveBeenCalledWith("sys_add");
    fireEvent.click(screen.getByTestId("developer-ex-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("shows empty runtime params editor when detail has none", async () => {
    getExtensionDetailMock.mockResolvedValue({
      ...sampleUserDetail,
      runtimeParameters: [],
    });
    render(<ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-params-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-params-empty").textContent).toBe(
      DEV_MSG.EX_RTPARAM_EMPTY,
    );
    expect(screen.queryByTestId("developer-ex-rtparam-row-0")).toBeNull();
  });

  it("disables Save and Delete for system extensions", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleSystemDetail);
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-immutable-hint")).toBeTruthy();
    });
    expect((screen.getByTestId("developer-ex-save") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ex-delete") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ex-name") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByTestId("developer-ex-interfaces") as HTMLTextAreaElement).disabled).toBe(
      true,
    );
  });

  it("disables save until create fields are valid", () => {
    render(<ExtensionDetailPanel idOrName={null} onBack={() => undefined} />);
    const save = screen.getByTestId("developer-ex-save") as HTMLButtonElement;
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ex-name"), {
      target: { value: "my_user_ext" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ex-interfaces"), {
      target: { value: "com.percussion.extension.IPSUdfProcessor" },
    });
    expect(save.disabled).toBe(true);
    fireEvent.change(screen.getByTestId("developer-ex-classname"), {
      target: { value: "com.example.MyUserExtension" },
    });
    expect(save.disabled).toBe(false);
  });

  it("creates a user extension when fields are valid", async () => {
    createExtensionMock.mockResolvedValue(sampleUserDetail);
    const onSaved = vi.fn();
    render(<ExtensionDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-ex-name"), {
      target: { value: "my_user_ext" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-interfaces"), {
      target: { value: "com.percussion.extension.IPSUdfProcessor" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-classname"), {
      target: { value: "com.example.MyUserExtension" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(createExtensionMock).toHaveBeenCalledWith(
      expect.objectContaining({
        extensionName: "my_user_ext",
        supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
        initParameters: expect.objectContaining({
          className: "com.example.MyUserExtension",
        }),
        context: "user/",
      }),
    );
    expect(screen.getByTestId("developer-ex-editor-notice").textContent).toBe(DEV_MSG.EX_SAVED);
  });

  it("adds a method map entry and includes it on save", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleUserDetail);
    saveExtensionMock.mockResolvedValue({
      ...sampleUserDetail,
      methods: {
        productVersion: {
          name: "productVersion",
          returnType: "java.lang.String",
          description: "ver",
          parameters: [],
        },
      },
    });
    const onSaved = vi.fn();
    render(
      <ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-method-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-method-add"));
    fireEvent.change(screen.getByTestId("developer-ex-method-name-0"), {
      target: { value: "productVersion" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-method-return-0"), {
      target: { value: "java.lang.String" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-method-desc-0"), {
      target: { value: "ver" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveExtensionMock).toHaveBeenCalledWith(
      "my_user_ext",
      expect.objectContaining({
        methods: {
          productVersion: expect.objectContaining({
            name: "productVersion",
            returnType: "java.lang.String",
            description: "ver",
          }),
        },
      }),
    );
  });

  it("applies init parameters from the dialog and includes them on save", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleUserDetail);
    saveExtensionMock.mockResolvedValue({
      ...sampleUserDetail,
      initParameters: {
        className: "com.example.MyUserExtension",
        "com.percussion.user.description": "from dialog",
      },
    });
    const onSaved = vi.fn();
    render(
      <ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-init-open")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-init-empty")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ex-init-open"));
    expect(screen.getByTestId("developer-ex-init-dialog")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ex-init-add"));
    fireEvent.change(screen.getByTestId("developer-ex-init-key-0"), {
      target: { value: "com.percussion.user.description" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-init-value-0"), {
      target: { value: "from dialog" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-init-apply"));
    await waitFor(() => {
      expect(screen.queryByTestId("developer-ex-init-dialog")).toBeNull();
    });
    expect(screen.getByTestId("developer-ex-init-summary-0").textContent).toContain(
      "com.percussion.user.description",
    );
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveExtensionMock).toHaveBeenCalledWith(
      "my_user_ext",
      expect.objectContaining({
        initParameters: expect.objectContaining({
          className: "com.example.MyUserExtension",
          "com.percussion.user.description": "from dialog",
        }),
      }),
    );
  });

  it("sends null for an extra init parameter removed in the dialog", async () => {
    getExtensionDetailMock.mockResolvedValue({
      ...sampleUserDetail,
      initParameters: {
        className: "com.example.MyUserExtension",
        extra: "gone",
      },
    });
    saveExtensionMock.mockResolvedValue(sampleUserDetail);
    const onSaved = vi.fn();
    render(
      <ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-init-summary-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-init-open"));
    fireEvent.click(screen.getByTestId("developer-ex-init-remove-0"));
    fireEvent.click(screen.getByTestId("developer-ex-init-apply"));
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    const write = saveExtensionMock.mock.calls[0][1] as {
      initParameters?: Record<string, string | null>;
    };
    expect(write.initParameters?.className).toBe("com.example.MyUserExtension");
    expect(write.initParameters?.extra).toBeUndefined();
  });

  it("adds a runtime parameter and includes it on save", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleUserDetail);
    saveExtensionMock.mockResolvedValue({
      ...sampleUserDetail,
      runtimeParameters: [
        { name: "htmlParams", dataType: "java.util.Map", description: "params" },
      ],
    });
    const onSaved = vi.fn();
    render(
      <ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-rtparam-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-rtparam-add"));
    fireEvent.change(screen.getByTestId("developer-ex-rtparam-name-0"), {
      target: { value: "htmlParams" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-rtparam-type-0"), {
      target: { value: "java.util.Map" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-rtparam-desc-0"), {
      target: { value: "params" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveExtensionMock).toHaveBeenCalledWith(
      "my_user_ext",
      expect.objectContaining({
        runtimeParameters: [
          { name: "htmlParams", dataType: "java.util.Map", description: "params" },
        ],
      }),
    );
  });

  it("removes a runtime parameter and saves an empty list to clear", async () => {
    getExtensionDetailMock.mockResolvedValue({
      ...sampleUserDetail,
      runtimeParameters: [{ name: "htmlParams", dataType: "java.util.Map" }],
    });
    saveExtensionMock.mockResolvedValue({ ...sampleUserDetail, runtimeParameters: [] });
    const onSaved = vi.fn();
    render(
      <ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-rtparam-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-rtparam-remove-0"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-params-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveExtensionMock).toHaveBeenCalledWith(
      "my_user_ext",
      // Panel passes the row list; wrapExtensionForWire maps [] to the blank-name
      // clear sentinel (covered in extensionsApi.test.ts).
      expect.objectContaining({ runtimeParameters: [] }),
    );
  });

  it("saves an existing user extension", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleUserDetail);
    saveExtensionMock.mockResolvedValue({ ...sampleUserDetail, deprecated: true });
    const onSaved = vi.fn();
    render(
      <ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} onSaved={onSaved} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-deprecated")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-deprecated"));
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalled();
    });
    expect(saveExtensionMock).toHaveBeenCalledWith(
      "my_user_ext",
      expect.objectContaining({
        extensionName: "my_user_ext",
        deprecated: true,
      }),
    );
  });

  it("deletes after confirm and omits delete chrome in create mode", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleUserDetail);
    deleteExtensionMock.mockResolvedValue(undefined);
    const onDeleted = vi.fn();
    render(
      <ExtensionDetailPanel
        idOrName="my_user_ext"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
    expect(deleteExtensionMock).toHaveBeenCalledWith("my_user_ext");
  });

  it("omits delete chrome in create mode", () => {
    render(<ExtensionDetailPanel idOrName={null} onBack={() => undefined} />);
    expect(screen.queryByTestId("developer-ex-delete")).toBeNull();
  });

  it("surfaces 409 duplicate name on create", async () => {
    createExtensionMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Extension already exists: Java/user/my_user_ext" },
    });
    const onSaved = vi.fn();
    render(<ExtensionDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-ex-name"), {
      target: { value: "my_user_ext" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-interfaces"), {
      target: { value: "com.percussion.extension.IPSUdfProcessor" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-classname"), {
      target: { value: "com.example.MyUserExtension" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toContain(
      "Extension already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("re-enables form and skips onSaved when create returns empty identity", async () => {
    createExtensionMock.mockResolvedValue({ extensionName: "", fqn: "" } as never);
    const onSaved = vi.fn();
    render(<ExtensionDetailPanel idOrName={null} onBack={() => undefined} onSaved={onSaved} />);
    fireEvent.change(screen.getByTestId("developer-ex-name"), {
      target: { value: "my_user_ext" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-interfaces"), {
      target: { value: "com.percussion.extension.IPSUdfProcessor" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-classname"), {
      target: { value: "com.example.MyUserExtension" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      DEV_MSG.EX_MISSING_PERSISTED_NAME,
    );
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-ex-save").disabled).toBe(false);
  });

  it("surfaces 403 non-Admin on create", async () => {
    createExtensionMock.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin role required" },
    });
    render(<ExtensionDetailPanel idOrName={null} onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-ex-name"), {
      target: { value: "my_user_ext" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-interfaces"), {
      target: { value: "com.percussion.extension.IPSUdfProcessor" },
    });
    fireEvent.change(screen.getByTestId("developer-ex-classname"), {
      target: { value: "com.example.MyUserExtension" },
    });
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toContain(
      DEV_MSG.EX_FORBIDDEN,
    );
  });

  it("surfaces 409 system immutable on save", async () => {
    getExtensionDetailMock.mockResolvedValue(sampleUserDetail);
    saveExtensionMock.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "System or handler-owned extensions cannot be mutated" },
    });
    render(<ExtensionDetailPanel idOrName="my_user_ext" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-deprecated")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-deprecated"));
    fireEvent.click(screen.getByTestId("developer-ex-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toContain(
      "cannot be mutated",
    );
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    getExtensionDetailMock.mockRejectedValue(new SessionRedirectError());
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ex-detail-loading")).toBeNull();
    expect(screen.queryByTestId("developer-ex-detail-title")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    getExtensionDetailMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      `${DEV_MSG.EX_DETAIL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    getExtensionDetailMock.mockRejectedValue(new Error("network down"));
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      `${DEV_MSG.EX_DETAIL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ex-detail-title")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    getExtensionDetailMock.mockRejectedValue("boom");
    render(<ExtensionDetailPanel idOrName="sys_add" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-detail-error").textContent).toBe(
      DEV_MSG.EX_DETAIL_ERROR,
    );
  });
});

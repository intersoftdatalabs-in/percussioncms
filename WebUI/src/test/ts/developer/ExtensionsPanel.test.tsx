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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  createExtension,
  deleteExtension,
  getExtensionDetail,
  listExtensions,
} from "../../../main/ts/api/developer/extensionsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ExtensionsPanel } from "../../../main/ts/developer/ExtensionsPanel";

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

const listExtensionsMock = vi.mocked(listExtensions);
const getExtensionDetailMock = vi.mocked(getExtensionDetail);
const createExtensionMock = vi.mocked(createExtension);
const deleteExtensionMock = vi.mocked(deleteExtension);

const sampleExtension = {
  extensionName: "sys_add",
  handlerName: "Java",
  context: "global/percussion/",
  fqn: "Java/global/percussion/sys_add",
  category: "sys",
};

const sampleUser = {
  extensionName: "my_user_ext",
  handlerName: "Java",
  context: "user/",
  fqn: "Java/user/my_user_ext",
  supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
  initParameters: { className: "com.example.MyUserExtension" },
  runtimeParameters: [],
};

describe("ExtensionsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listExtensionsMock.mockReset();
    getExtensionDetailMock.mockReset();
    createExtensionMock.mockReset();
    deleteExtensionMock.mockReset();
  });

  it("lists extensions and opens detail", async () => {
    listExtensionsMock.mockResolvedValue([sampleExtension]);
    getExtensionDetailMock.mockResolvedValue({
      ...sampleExtension,
      supportedInterfaces: ["com.percussion.extension.IPSExtension"],
      runtimeParameters: [{ name: "htmlParams", dataType: "java.util.Map" }],
    });
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-table").textContent).toContain("sys_add");
    expect(screen.getByTestId("developer-ex-table").textContent).toContain(DEV_MSG.EX_FLAG_SYSTEM);
    fireEvent.click(screen.getByTestId("developer-ex-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-params-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ex-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
  });

  it("opens create chrome from New extension", async () => {
    listExtensionsMock.mockResolvedValue([sampleExtension]);
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-new"));
    expect(screen.getByTestId("developer-ex-detail")).toBeTruthy();
    expect(screen.getByTestId("developer-ex-detail-title").textContent).toContain(DEV_MSG.EX_NEW);
  });

  it("reloads catalog after create and delete", async () => {
    listExtensionsMock.mockResolvedValue([sampleExtension]);
    createExtensionMock.mockResolvedValue(sampleUser);
    deleteExtensionMock.mockResolvedValue(undefined);
    getExtensionDetailMock.mockResolvedValue(sampleUser);
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-new"));
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
      expect(listExtensionsMock.mock.calls.length).toBeGreaterThan(1);
    });

    listExtensionsMock.mockResolvedValue([sampleExtension, sampleUser]);
    fireEvent.click(screen.getByTestId("developer-ex-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
    // Open user row (second open button if both present)
    const opens = screen.getAllByTestId("developer-ex-open");
    fireEvent.click(opens[opens.length - 1]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ex-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-list-notice")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-list-notice").textContent).toBe(DEV_MSG.EX_DELETED);
  });

  it("shows empty state when API returns no extensions", async () => {
    listExtensionsMock.mockResolvedValue([]);
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-new")).toBeTruthy();
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listExtensionsMock.mockRejectedValue(new SessionRedirectError());
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
    expect(screen.queryByTestId("developer-ex-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listExtensionsMock.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(`${DEV_MSG.EX_ERROR} (500)`);
  });

  it("shows Error.message via panelErrMsg", async () => {
    listExtensionsMock.mockRejectedValue(new Error("network down"));
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(
      `${DEV_MSG.EX_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ex-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listExtensionsMock.mockRejectedValue("boom");
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-error").textContent).toBe(DEV_MSG.EX_ERROR);
  });
});

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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as api from "../../../main/ts/api/developer/contentTypesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { OBJECT_SORTER_STORAGE_KEY } from "../../../main/ts/developer/objectSorter";
import { ObjectSorterPanel } from "../../../main/ts/developer/ObjectSorterPanel";

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypesApi")>();
  return {
    ...actual,
    listContentTypes: vi.fn(),
  };
});

const listContentTypes = api.listContentTypes as ReturnType<typeof vi.fn>;

describe("ObjectSorterPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    sessionStorage.clear();
    listContentTypes.mockReset();
  });

  it("lists the current content-type catalog sorted by label", async () => {
    listContentTypes.mockResolvedValue([
      { name: "zetaType", label: "Alpha" },
      { name: "alphaType", label: "Zulu" },
    ]);
    render(<ObjectSorterPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-os-table")).toBeTruthy();
    });
    const names = screen.getAllByTestId("developer-os-name").map((n) => n.textContent);
    expect(names).toEqual(["zetaType", "alphaType"]);
    expect(screen.getByTestId("developer-os-session-note").textContent).toBe(
      DEV_MSG.OS_SESSION_ONLY,
    );
  });

  it("changes sort to name Z-A and persists in sessionStorage", async () => {
    listContentTypes.mockResolvedValue([
      { name: "zetaType", label: "Alpha" },
      { name: "alphaType", label: "Zulu" },
    ]);
    render(<ObjectSorterPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-os-mode")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-os-mode"), {
      target: { value: "name-desc" },
    });
    const names = screen.getAllByTestId("developer-os-name").map((n) => n.textContent);
    expect(names).toEqual(["zetaType", "alphaType"]);
    expect(sessionStorage.getItem(OBJECT_SORTER_STORAGE_KEY)).toContain("name-desc");
    expect(screen.getByTestId("developer-os-panel").getAttribute("data-os-mode")).toBe(
      "name-desc",
    );
  });

  it("custom order move-down reorders rows and sticks in sessionStorage", async () => {
    listContentTypes.mockResolvedValue([
      { name: "zetaType", label: "Alpha" },
      { name: "alphaType", label: "Zulu" },
    ]);
    render(<ObjectSorterPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-os-mode")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-os-mode"), {
      target: { value: "custom" },
    });
    await waitFor(() => {
      expect(screen.getAllByTestId("developer-os-move-down").length).toBeGreaterThan(0);
    });
    fireEvent.click(screen.getAllByTestId("developer-os-move-down")[0]);
    const names = screen.getAllByTestId("developer-os-name").map((n) => n.textContent);
    expect(names).toEqual(["alphaType", "zetaType"]);
    expect(sessionStorage.getItem(OBJECT_SORTER_STORAGE_KEY)).toContain("custom");
  });

  it("shows empty state when the catalog is empty", async () => {
    listContentTypes.mockResolvedValue([]);
    render(<ObjectSorterPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-os-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-os-empty").textContent).toBe(DEV_MSG.OS_EMPTY);
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue(new SessionRedirectError());
    render(<ObjectSorterPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-os-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-os-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
  });
});

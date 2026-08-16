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

import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import {
  AssetWizard,
  resolveAssetCreateContentType,
} from "@/home/create/AssetWizard";
import * as homeApi from "@/api/home/homeApi";

vi.mock("@/api/home/homeApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/home/homeApi")>();
  return {
    ...actual,
    fetchAssetTypes: vi.fn().mockResolvedValue([
      {
        id: "percImage",
        name: "Image",
        label: "Image",
        contentTypeName: "percImageAsset",
      },
    ]),
    fetchFolderChildren: vi.fn().mockResolvedValue([]),
    formatApiError: vi.fn((_e: unknown, fallback: string) => fallback),
  };
});

describe("resolveAssetCreateContentType", () => {
  it("prefers contentTypeName over widget id", () => {
    expect(
      resolveAssetCreateContentType({
        id: "percImage",
        name: "Image",
        contentTypeName: "percImageAsset",
      }),
    ).toBe("percImageAsset");
  });

  it("falls back to widget id when contentTypeName is blank", () => {
    expect(
      resolveAssetCreateContentType({
        id: "percRawHtml",
        name: "Raw HTML",
        contentTypeName: "  ",
      }),
    ).toBe("percRawHtml");
  });
});

describe("AssetWizard opener", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
  });

  it("creates via itemmanagement and opens the React editor host", async () => {
    const hrefWrites: string[] = [];
    const hrefDescriptor = Object.getOwnPropertyDescriptor(
      window.location,
      "href",
    );
    Object.defineProperty(window.location, "href", {
      configurable: true,
      get: () => hrefDescriptor?.get?.() ?? "http://localhost/",
      set: (v: string) => {
        hrefWrites.push(String(v));
      },
    });

    const reserved = {
      closed: false,
      close: vi.fn(),
      location: { href: "about:blank" },
      focus: vi.fn(),
    } as unknown as Window;
    const order: string[] = [];
    const reservePopup = vi.fn(() => {
      order.push("reserve");
      return reserved;
    });
    const createItem = vi.fn().mockImplementation(async () => {
      order.push("create");
      return {
        itemId: "88",
        folderPath: "//Assets",
        name: "New-percImageAsset",
        contentType: "percImageAsset",
      };
    });
    const openCreated = vi.fn().mockResolvedValue(true);

    render(
      <AssetWizard
        onBack={() => undefined}
        createItem={createItem}
        openCreated={openCreated}
        reservePopup={reservePopup}
      />,
    );
    await waitFor(() => screen.getByTestId("asset-wizard"));
    fireEvent.click(screen.getByTestId("asset-wizard-submit"));
    await waitFor(() => {
      expect(createItem).toHaveBeenCalled();
      expect(openCreated).toHaveBeenCalled();
    });
    expect(createItem.mock.calls[0]?.[0]).toMatchObject({
      contentType: "percImageAsset",
      folderPath: "/Assets",
    });
    expect(openCreated.mock.calls[0]?.[0]).toMatchObject({
      id: "88",
      path: "/Assets/New-percImageAsset",
    });
    expect(openCreated.mock.calls[0]?.[1]).toMatchObject({
      reservedWindow: reserved,
    });
    expect(order).toEqual(["reserve", "create"]);
    expect(hrefWrites.some((h) => /editAsset\.jsp|view=editor/.test(h))).toBe(
      false,
    );
    expect(homeApi.fetchAssetTypes).toHaveBeenCalled();

    if (hrefDescriptor) {
      Object.defineProperty(window.location, "href", hrefDescriptor);
    }
  });

  it("does not navigate when type is missing", async () => {
    vi.mocked(homeApi.fetchAssetTypes).mockResolvedValueOnce([
      { id: "percImage", name: "Image", label: "Image" },
      { id: "percFile", name: "File", label: "File" },
    ]);
    const createItem = vi.fn();
    const openCreated = vi.fn();
    render(
      <AssetWizard
        onBack={() => undefined}
        createItem={createItem}
        openCreated={openCreated}
      />,
    );
    await waitFor(() => screen.getByTestId("asset-wizard"));
    fireEvent.click(screen.getByTestId("asset-wizard-submit"));
    expect(createItem).not.toHaveBeenCalled();
    expect(openCreated).not.toHaveBeenCalled();
  });

  it("surfaces CREATE_NOT_AUTHORIZED when createItem rejects", async () => {
    const reserved = {
      closed: false,
      close: vi.fn(),
    } as unknown as Window;
    const reservePopup = vi.fn(() => reserved);
    const createItem = vi.fn().mockRejectedValue(new Error("forbidden"));
    const openCreated = vi.fn();
    render(
      <AssetWizard
        onBack={() => undefined}
        createItem={createItem}
        openCreated={openCreated}
        reservePopup={reservePopup}
      />,
    );
    await waitFor(() => screen.getByTestId("asset-wizard"));
    fireEvent.click(screen.getByTestId("asset-wizard-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("asset-wizard-error").textContent).toBe(
        "Create Not Authorized",
      );
    });
    expect(openCreated).not.toHaveBeenCalled();
    expect(reserved.close).toHaveBeenCalled();
    expect(screen.queryByTestId("asset-wizard-open-editor")).toBeNull();
  });

  it("surfaces CREATE_OPEN_EDITOR_FAILED and retries via Open", async () => {
    const reserved = {
      closed: false,
      close: vi.fn(),
    } as unknown as Window;
    const reservePopup = vi.fn(() => reserved);
    const createItem = vi.fn().mockResolvedValue({
      itemId: "91",
      folderPath: "//Assets",
      name: "stub",
      contentType: "percImageAsset",
    });
    const openCreated = vi
      .fn()
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(true);
    render(
      <AssetWizard
        onBack={() => undefined}
        createItem={createItem}
        openCreated={openCreated}
        reservePopup={reservePopup}
      />,
    );
    await waitFor(() => screen.getByTestId("asset-wizard"));
    fireEvent.click(screen.getByTestId("asset-wizard-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("asset-wizard-error").textContent).toBe(
        "The page was created but the editor could not open.",
      );
    });
    expect(reserved.close).toHaveBeenCalled();
    const retry = screen.getByTestId("asset-wizard-open-editor");
    fireEvent.click(retry);
    await waitFor(() => {
      expect(screen.queryByTestId("asset-wizard-error")).toBeNull();
    });
    expect(openCreated).toHaveBeenCalledTimes(2);
    expect(openCreated.mock.calls[1]?.[0]).toMatchObject({ id: "91" });
    expect(screen.queryByTestId("asset-wizard-open-editor")).toBeNull();
  });
});

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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getSite, updateSite } from "@/api/developer/sitesApi";
import { SiteBaseUrlPanel } from "@/publishing/components/SiteBaseUrlPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteBaseUrlPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      description: "keep me",
      baseUrl: "https://before.example",
    });
  });

  it("shows the saved base URL after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      baseUrl: "https://after.example/root",
    });
    const { unmount } = render(<SiteBaseUrlPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-base-url")).toHaveTextContent(
      "https://before.example",
    );
    fireEvent.click(screen.getByTestId("publish-site-base-url-edit"));
    fireEvent.change(screen.getByTestId("publish-site-base-url-input"), {
      target: { value: "  https://after.example/root  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-base-url-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        baseUrl: "https://after.example/root",
      });
    });
    expect(screen.getByTestId("publish-site-base-url")).toHaveTextContent(
      "https://after.example/root",
    );
    expect(screen.getByTestId("publish-site-base-url-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-base-url-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      baseUrl: "https://after.example/root",
    });
    render(<SiteBaseUrlPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-base-url")).toHaveTextContent(
      "https://after.example/root",
    );
  });

  it("does not PUT on cancel, an unchanged URL, or an empty or invalid URL", async () => {
    render(<SiteBaseUrlPanel siteName="NightlySite" />);
    await screen.findByText("https://before.example");
    fireEvent.click(screen.getByTestId("publish-site-base-url-edit"));
    fireEvent.change(screen.getByTestId("publish-site-base-url-input"), {
      target: { value: "https://discard.example" },
    });
    fireEvent.click(screen.getByTestId("publish-site-base-url-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-base-url")).toHaveTextContent(
      "https://before.example",
    );
    expect(screen.queryByTestId("publish-site-base-url-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-base-url-edit"));
    fireEvent.change(screen.getByTestId("publish-site-base-url-input"), {
      target: { value: "  https://before.example  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-base-url-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-base-url-saved")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-base-url-edit"));
    fireEvent.change(screen.getByTestId("publish-site-base-url-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("publish-site-base-url-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-base-url-error").textContent?.toLowerCase()).toContain(
      "invalid",
    );
    expect(screen.queryByTestId("publish-site-base-url-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-base-url-form")).toBeTruthy();

    fireEvent.change(screen.getByTestId("publish-site-base-url-input"), {
      target: { value: "javascript:alert(1)" },
    });
    fireEvent.click(screen.getByTestId("publish-site-base-url-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-base-url")).toHaveTextContent(
      "https://before.example",
    );
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteBaseUrlPanel siteName="NightlySite" />);
    await screen.findByText("https://before.example");
    fireEvent.click(screen.getByTestId("publish-site-base-url-edit"));
    fireEvent.change(screen.getByTestId("publish-site-base-url-input"), {
      target: { value: "https://blocked.example" },
    });
    fireEvent.click(screen.getByTestId("publish-site-base-url-save"));
    const alert = await screen.findByTestId("publish-site-base-url-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-base-url-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-base-url")).toHaveTextContent(
      "https://before.example",
    );
    expect(screen.queryByTestId("publish-site-base-url-saved")).toBeNull();
  });
});

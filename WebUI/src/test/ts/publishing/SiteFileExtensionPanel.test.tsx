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
import { SiteFileExtensionPanel } from "@/publishing/components/SiteFileExtensionPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteFileExtensionPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      defaultFileExtention: "html",
      description: "keep",
      baseUrl: "https://keep.example",
    });
  });

  it("shows the saved extension after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      defaultFileExtention: "htm",
    });
    const { unmount } = render(<SiteFileExtensionPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-file-extension")).toHaveTextContent("html");
    fireEvent.click(screen.getByTestId("publish-site-file-extension-edit"));
    fireEvent.change(screen.getByTestId("publish-site-file-extension-input"), {
      target: { value: "  .htm  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-file-extension-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        defaultFileExtention: "htm",
      });
    });
    expect(screen.getByTestId("publish-site-file-extension")).toHaveTextContent("htm");
    expect(screen.getByTestId("publish-site-file-extension-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-file-extension-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      defaultFileExtention: "htm",
    });
    render(<SiteFileExtensionPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-file-extension")).toHaveTextContent("htm");
  });

  it("does not PUT on cancel, unchanged text, empty, or an invalid extension", async () => {
    render(<SiteFileExtensionPanel siteName="NightlySite" />);
    await screen.findByText("html");
    fireEvent.click(screen.getByTestId("publish-site-file-extension-edit"));
    fireEvent.change(screen.getByTestId("publish-site-file-extension-input"), {
      target: { value: "php" },
    });
    fireEvent.click(screen.getByTestId("publish-site-file-extension-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-file-extension")).toHaveTextContent("html");
    expect(screen.queryByTestId("publish-site-file-extension-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-file-extension-edit"));
    fireEvent.change(screen.getByTestId("publish-site-file-extension-input"), {
      target: { value: "  .html  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-file-extension-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-file-extension-saved")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-file-extension-edit"));
    fireEvent.change(screen.getByTestId("publish-site-file-extension-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("publish-site-file-extension-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-file-extension-error").textContent?.toLowerCase()).toContain(
      "empty",
    );
    expect(screen.queryByTestId("publish-site-file-extension-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-file-extension-form")).toBeTruthy();

    fireEvent.change(screen.getByTestId("publish-site-file-extension-input"), {
      target: { value: "html/php" },
    });
    fireEvent.click(screen.getByTestId("publish-site-file-extension-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-file-extension-error").textContent?.toLowerCase()).toContain(
      "invalid",
    );
    expect(screen.queryByTestId("publish-site-file-extension-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-file-extension")).toHaveTextContent("html");
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteFileExtensionPanel siteName="NightlySite" />);
    await screen.findByText("html");
    fireEvent.click(screen.getByTestId("publish-site-file-extension-edit"));
    fireEvent.change(screen.getByTestId("publish-site-file-extension-input"), {
      target: { value: "php" },
    });
    fireEvent.click(screen.getByTestId("publish-site-file-extension-save"));
    const alert = await screen.findByTestId("publish-site-file-extension-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-file-extension-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-file-extension")).toHaveTextContent("html");
    expect(screen.queryByTestId("publish-site-file-extension-saved")).toBeNull();
  });
});

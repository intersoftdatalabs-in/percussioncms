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
import { SiteFolderRootPanel } from "@/publishing/components/SiteFolderRootPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteFolderRootPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      folderRoot: "//Sites/Old",
      description: "keep",
      baseUrl: "https://keep.example",
    });
  });

  it("shows the saved path after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      folderRoot: "//Sites/Nightly",
    });
    const { unmount } = render(<SiteFolderRootPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-folder-root")).toHaveTextContent(
      "//Sites/Old",
    );
    fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
    fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
      target: { value: "  \\\\Sites\\Nightly  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        folderRoot: "//Sites/Nightly",
      });
    });
    expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent("//Sites/Nightly");
    expect(screen.getByTestId("publish-site-folder-root-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-folder-root-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      folderRoot: "//Sites/Nightly",
    });
    render(<SiteFolderRootPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-folder-root")).toHaveTextContent(
      "//Sites/Nightly",
    );
  });

  it("does not PUT on cancel, unchanged text, empty, or an unsafe path", async () => {
    render(<SiteFolderRootPanel siteName="NightlySite" />);
    await screen.findByText("//Sites/Old");
    fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
    fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
      target: { value: "//Sites/Other" },
    });
    fireEvent.click(screen.getByTestId("publish-site-folder-root-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent("//Sites/Old");
    expect(screen.queryByTestId("publish-site-folder-root-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
    fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
      target: { value: "  \\\\Sites\\Old  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
    fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-folder-root-error").textContent?.toLowerCase()).toContain(
      "empty",
    );
    expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-folder-root-form")).toBeTruthy();

    fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
      target: { value: "//Sites/../etc" },
    });
    fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-folder-root-error").textContent?.toLowerCase()).toContain(
      "invalid",
    );
    expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent("//Sites/Old");
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteFolderRootPanel siteName="NightlySite" />);
    await screen.findByText("//Sites/Old");
    fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
    fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
      target: { value: "//Sites/Blocked" },
    });
    fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
    const alert = await screen.findByTestId("publish-site-folder-root-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-folder-root-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent("//Sites/Old");
    expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();
  });
});

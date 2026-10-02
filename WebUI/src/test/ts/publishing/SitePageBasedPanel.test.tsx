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
import { SitePageBasedPanel } from "@/publishing/components/SitePageBasedPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SitePageBasedPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      pageBasedSite: false,
    });
  });

  it("shows the saved flag after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      pageBasedSite: true,
    });
    const { unmount } = render(<SitePageBasedPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-page-based")).toHaveTextContent("no");
    fireEvent.click(screen.getByTestId("publish-site-page-based-edit"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-input"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        pageBasedSite: true,
      });
    });
    expect(screen.getByTestId("publish-site-page-based")).toHaveTextContent("yes");
    expect(screen.getByTestId("publish-site-page-based-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-page-based-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      pageBasedSite: true,
    });
    render(<SitePageBasedPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-page-based")).toHaveTextContent("yes");
  });

  it("does not PUT on cancel and restores the previous value", async () => {
    render(<SitePageBasedPanel siteName="NightlySite" />);
    await screen.findByText("no");
    fireEvent.click(screen.getByTestId("publish-site-page-based-edit"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-input"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-page-based")).toHaveTextContent("no");
    expect(screen.queryByTestId("publish-site-page-based-form")).toBeNull();
    expect(screen.queryByTestId("publish-site-page-based-saved")).toBeNull();
  });

  it("does not PUT when the checkbox is unchanged", async () => {
    render(<SitePageBasedPanel siteName="NightlySite" />);
    await screen.findByText("no");
    fireEvent.click(screen.getByTestId("publish-site-page-based-edit"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-page-based-saved")).toBeNull();
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SitePageBasedPanel siteName="NightlySite" />);
    await screen.findByText("no");
    fireEvent.click(screen.getByTestId("publish-site-page-based-edit"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-input"));
    fireEvent.click(screen.getByTestId("publish-site-page-based-save"));
    expect(await screen.findByTestId("publish-site-page-based-error")).toHaveTextContent(snippet);
    expect(screen.getByTestId("publish-site-page-based-form")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-page-based-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-page-based")).toHaveTextContent("no");
  });
});

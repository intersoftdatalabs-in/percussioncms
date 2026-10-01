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
import { SiteCanonicalDistPanel } from "@/publishing/components/SiteCanonicalDistPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteCanonicalDistPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalDist: "pages",
      description: "keep",
      baseUrl: "https://keep.example",
    });
  });

  it("shows the saved canonical distribution after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalDist: "sections",
    });
    const { unmount } = render(<SiteCanonicalDistPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-canonical-dist")).toHaveTextContent(
      "pages",
    );
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-edit"));
    fireEvent.change(screen.getByTestId("publish-site-canonical-dist-input"), {
      target: { value: "sections" },
    });
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        canonicalDist: "sections",
      });
    });
    expect(screen.getByTestId("publish-site-canonical-dist")).toHaveTextContent("sections");
    expect(screen.getByTestId("publish-site-canonical-dist-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-canonical-dist-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalDist: "sections",
    });
    render(<SiteCanonicalDistPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-canonical-dist")).toHaveTextContent(
      "sections",
    );
  });

  it("does not PUT on cancel or when the destination is unchanged", async () => {
    render(<SiteCanonicalDistPanel siteName="NightlySite" />);
    await screen.findByText("pages");
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-edit"));
    fireEvent.change(screen.getByTestId("publish-site-canonical-dist-input"), {
      target: { value: "sections" },
    });
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-canonical-dist")).toHaveTextContent("pages");
    expect(screen.queryByTestId("publish-site-canonical-dist-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-edit"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-canonical-dist-saved")).toBeNull();
  });

  it("does not claim success for a blank or unsupported stored value until a valid save", async () => {
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalDist: "   ",
    });
    const { unmount } = render(<SiteCanonicalDistPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-canonical-dist")).toHaveTextContent("—");
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-edit"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-canonical-dist-saved")).toBeNull();
    unmount();

    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalDist: "widgets",
    });
    render(<SiteCanonicalDistPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-canonical-dist")).toHaveTextContent(
      "widgets",
    );
    expect(screen.queryByTestId("publish-site-canonical-dist-saved")).toBeNull();
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteCanonicalDistPanel siteName="NightlySite" />);
    await screen.findByText("pages");
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-edit"));
    fireEvent.change(screen.getByTestId("publish-site-canonical-dist-input"), {
      target: { value: "sections" },
    });
    fireEvent.click(screen.getByTestId("publish-site-canonical-dist-save"));
    const alert = await screen.findByTestId("publish-site-canonical-dist-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-canonical-dist-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-canonical-dist")).toHaveTextContent("pages");
    expect(screen.queryByTestId("publish-site-canonical-dist-saved")).toBeNull();
  });
});

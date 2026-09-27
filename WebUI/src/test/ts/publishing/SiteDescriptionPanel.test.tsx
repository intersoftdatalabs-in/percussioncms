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
import { SiteDescriptionPanel } from "@/publishing/components/SiteDescriptionPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteDescriptionPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      description: "Before",
      baseUrl: "https://keep.example",
    });
  });

  it("shows the saved description after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      description: "After hours",
    });
    const { unmount } = render(<SiteDescriptionPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-description")).toHaveTextContent("Before");
    fireEvent.click(screen.getByTestId("publish-site-description-edit"));
    fireEvent.change(screen.getByTestId("publish-site-description-input"), {
      target: { value: "  After hours  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-description-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        description: "After hours",
      });
    });
    expect(screen.getByTestId("publish-site-description")).toHaveTextContent("After hours");
    expect(screen.getByTestId("publish-site-description-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-description-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      description: "After hours",
    });
    render(<SiteDescriptionPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-description")).toHaveTextContent(
      "After hours",
    );
  });

  it("does not PUT on cancel or when the text is unchanged", async () => {
    render(<SiteDescriptionPanel siteName="NightlySite" />);
    await screen.findByText("Before");
    fireEvent.click(screen.getByTestId("publish-site-description-edit"));
    fireEvent.change(screen.getByTestId("publish-site-description-input"), {
      target: { value: "Discard me" },
    });
    fireEvent.click(screen.getByTestId("publish-site-description-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-description")).toHaveTextContent("Before");
    expect(screen.queryByTestId("publish-site-description-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-description-edit"));
    fireEvent.change(screen.getByTestId("publish-site-description-input"), {
      target: { value: "  Before  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-description-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-description-saved")).toBeNull();
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteDescriptionPanel siteName="NightlySite" />);
    await screen.findByText("Before");
    fireEvent.click(screen.getByTestId("publish-site-description-edit"));
    fireEvent.change(screen.getByTestId("publish-site-description-input"), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByTestId("publish-site-description-save"));
    const alert = await screen.findByTestId("publish-site-description-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-description-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-description")).toHaveTextContent("Before");
    expect(screen.queryByTestId("publish-site-description-saved")).toBeNull();
  });
});

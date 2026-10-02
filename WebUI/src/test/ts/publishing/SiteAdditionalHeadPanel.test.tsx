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
import { SiteAdditionalHeadPanel } from "@/publishing/components/SiteAdditionalHeadPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

const MARKUP = '<meta name="x"><script>window.__headExec=1</script>';

describe("SiteAdditionalHeadPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      description: "keep",
      siteAdditionalHeadContent: "<meta name=\"old\">",
    });
  });

  it("shows saved markup as text after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      siteAdditionalHeadContent: MARKUP,
    });
    const { unmount, container } = render(<SiteAdditionalHeadPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-additional-head")).toHaveTextContent(
      "<meta name=\"old\">",
    );
    expect(container.querySelector("script")).toBeNull();
    fireEvent.click(screen.getByTestId("publish-site-additional-head-edit"));
    fireEvent.change(screen.getByTestId("publish-site-additional-head-input"), {
      target: { value: `  ${MARKUP}  ` },
    });
    fireEvent.click(screen.getByTestId("publish-site-additional-head-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        siteAdditionalHeadContent: MARKUP,
      });
    });
    expect(screen.getByTestId("publish-site-additional-head")).toHaveTextContent(MARKUP);
    expect(container.querySelector("script")).toBeNull();
    expect(screen.getByTestId("publish-site-additional-head-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-additional-head-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      siteAdditionalHeadContent: MARKUP,
    });
    const again = render(<SiteAdditionalHeadPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-additional-head")).toHaveTextContent(MARKUP);
    expect(again.container.querySelector("script")).toBeNull();
  });

  it("clears stored content with an empty save", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      siteAdditionalHeadContent: "",
    });
    render(<SiteAdditionalHeadPanel siteName="NightlySite" />);
    await screen.findByText("<meta name=\"old\">");
    fireEvent.click(screen.getByTestId("publish-site-additional-head-edit"));
    fireEvent.change(screen.getByTestId("publish-site-additional-head-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("publish-site-additional-head-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        siteAdditionalHeadContent: "",
      });
    });
    expect(screen.getByTestId("publish-site-additional-head")).toHaveTextContent("—");
  });

  it("does not PUT on cancel or when the text is unchanged", async () => {
    render(<SiteAdditionalHeadPanel siteName="NightlySite" />);
    await screen.findByText("<meta name=\"old\">");
    fireEvent.click(screen.getByTestId("publish-site-additional-head-edit"));
    fireEvent.change(screen.getByTestId("publish-site-additional-head-input"), {
      target: { value: "discard" },
    });
    fireEvent.click(screen.getByTestId("publish-site-additional-head-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-additional-head")).toHaveTextContent(
      "<meta name=\"old\">",
    );
    expect(screen.queryByTestId("publish-site-additional-head-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-additional-head-edit"));
    fireEvent.change(screen.getByTestId("publish-site-additional-head-input"), {
      target: { value: "  <meta name=\"old\">  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-additional-head-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-additional-head-saved")).toBeNull();
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteAdditionalHeadPanel siteName="NightlySite" />);
    await screen.findByText("<meta name=\"old\">");
    fireEvent.click(screen.getByTestId("publish-site-additional-head-edit"));
    fireEvent.change(screen.getByTestId("publish-site-additional-head-input"), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByTestId("publish-site-additional-head-save"));
    const alert = await screen.findByTestId("publish-site-additional-head-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-additional-head-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-additional-head")).toHaveTextContent(
      "<meta name=\"old\">",
    );
    expect(screen.queryByTestId("publish-site-additional-head-saved")).toBeNull();
  });
});

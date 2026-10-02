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
import { SiteCanonicalReplacePanel } from "@/publishing/components/SiteCanonicalReplacePanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteCanonicalReplacePanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalReplace: true,
    });
  });

  it("shows the saved flag after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalReplace: false,
    });
    const { unmount } = render(<SiteCanonicalReplacePanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-canonical-replace")).toHaveTextContent(
      "yes",
    );
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-edit"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-input"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        canonicalReplace: false,
      });
    });
    expect(screen.getByTestId("publish-site-canonical-replace")).toHaveTextContent("no");
    expect(screen.getByTestId("publish-site-canonical-replace-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-canonical-replace-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      canonicalReplace: false,
    });
    render(<SiteCanonicalReplacePanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-canonical-replace")).toHaveTextContent(
      "no",
    );
  });

  it("does not PUT on cancel and restores the previous value", async () => {
    render(<SiteCanonicalReplacePanel siteName="NightlySite" />);
    await screen.findByText("yes");
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-edit"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-input"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-canonical-replace")).toHaveTextContent("yes");
    expect(screen.queryByTestId("publish-site-canonical-replace-form")).toBeNull();
    expect(screen.queryByTestId("publish-site-canonical-replace-saved")).toBeNull();
  });

  it("does not PUT when the checkbox is unchanged", async () => {
    render(<SiteCanonicalReplacePanel siteName="NightlySite" />);
    await screen.findByText("yes");
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-edit"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-canonical-replace-saved")).toBeNull();
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteCanonicalReplacePanel siteName="NightlySite" />);
    await screen.findByText("yes");
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-edit"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-input"));
    fireEvent.click(screen.getByTestId("publish-site-canonical-replace-save"));
    expect(await screen.findByTestId("publish-site-canonical-replace-error")).toHaveTextContent(
      snippet,
    );
    expect(screen.getByTestId("publish-site-canonical-replace-form")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-canonical-replace-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-canonical-replace")).toHaveTextContent("yes");
  });
});

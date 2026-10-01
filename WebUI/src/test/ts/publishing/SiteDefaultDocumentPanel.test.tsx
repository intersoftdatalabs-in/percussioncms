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
import { SiteDefaultDocumentPanel } from "@/publishing/components/SiteDefaultDocumentPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

describe("SiteDefaultDocumentPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      defaultDocument: "index.html",
      description: "keep",
      baseUrl: "https://keep.example",
    });
  });

  it("shows the saved default document after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      defaultDocument: "home.html",
    });
    const { unmount } = render(<SiteDefaultDocumentPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-default-document")).toHaveTextContent(
      "index.html",
    );
    fireEvent.click(screen.getByTestId("publish-site-default-document-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-document-input"), {
      target: { value: "  home.html  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-document-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        defaultDocument: "home.html",
      });
    });
    expect(screen.getByTestId("publish-site-default-document")).toHaveTextContent("home.html");
    expect(screen.getByTestId("publish-site-default-document-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-default-document-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      defaultDocument: "home.html",
    });
    render(<SiteDefaultDocumentPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-default-document")).toHaveTextContent(
      "home.html",
    );
  });

  it("does not PUT on cancel, unchanged text, or an empty value", async () => {
    render(<SiteDefaultDocumentPanel siteName="NightlySite" />);
    await screen.findByText("index.html");
    fireEvent.click(screen.getByTestId("publish-site-default-document-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-document-input"), {
      target: { value: "discard.html" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-document-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-default-document")).toHaveTextContent("index.html");
    expect(screen.queryByTestId("publish-site-default-document-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-default-document-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-document-input"), {
      target: { value: "  index.html  " },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-document-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-default-document-saved")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-default-document-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-document-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-document-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-default-document-error").textContent?.toLowerCase()).toContain(
      "empty",
    );
    expect(screen.queryByTestId("publish-site-default-document-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-default-document-form")).toBeTruthy();
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteDefaultDocumentPanel siteName="NightlySite" />);
    await screen.findByText("index.html");
    fireEvent.click(screen.getByTestId("publish-site-default-document-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-document-input"), {
      target: { value: "nope.html" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-document-save"));
    const alert = await screen.findByTestId("publish-site-default-document-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-default-document-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-default-document")).toHaveTextContent("index.html");
    expect(screen.queryByTestId("publish-site-default-document-saved")).toBeNull();
  });
});

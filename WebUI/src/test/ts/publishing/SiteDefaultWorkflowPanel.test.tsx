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
import { listWorkflows } from "@/api/developer/workflowsApi";
import { SiteDefaultWorkflowPanel } from "@/publishing/components/SiteDefaultWorkflowPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

vi.mock("@/api/developer/workflowsApi", () => ({
  listWorkflows: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;
const listWorkflowsMock = listWorkflows as ReturnType<typeof vi.fn>;

describe("SiteDefaultWorkflowPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    listWorkflowsMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      workflowName: "Standard",
      description: "keep",
    });
    listWorkflowsMock.mockResolvedValue([
      { workflowName: "Local Content" },
      { workflowName: "Standard" },
      { workflowName: " standard " },
    ]);
  });

  it("shows the saved workflow after PUT and a remount refresh", async () => {
    updateSiteMock.mockResolvedValue({
      name: "NightlySite",
      workflowName: "Local Content",
    });
    const { unmount } = render(<SiteDefaultWorkflowPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-default-workflow")).toHaveTextContent(
      "Standard",
    );
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-edit"));
    fireEvent.change(await screen.findByTestId("publish-site-default-workflow-choice"), {
      target: { value: "Local Content" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-save"));
    await waitFor(() => {
      expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
        name: "NightlySite",
        workflowName: "Local Content",
      });
    });
    expect(screen.getByTestId("publish-site-default-workflow")).toHaveTextContent("Local Content");
    expect(screen.getByTestId("publish-site-default-workflow-saved")).toBeTruthy();
    expect(screen.queryByTestId("publish-site-default-workflow-form")).toBeNull();

    unmount();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      workflowName: "Local Content",
    });
    render(<SiteDefaultWorkflowPanel siteName="NightlySite" />);
    expect(await screen.findByTestId("publish-site-default-workflow")).toHaveTextContent(
      "Local Content",
    );
  });

  it("does not PUT on cancel, an unchanged name, or an empty choice", async () => {
    render(<SiteDefaultWorkflowPanel siteName="NightlySite" />);
    await screen.findByText("Standard");
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-edit"));
    fireEvent.change(await screen.findByTestId("publish-site-default-workflow-choice"), {
      target: { value: "Local Content" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-cancel"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-site-default-workflow")).toHaveTextContent("Standard");
    expect(screen.queryByTestId("publish-site-default-workflow-form")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-default-workflow-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-workflow-choice"), {
      target: { value: "Standard" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(screen.queryByTestId("publish-site-default-workflow-saved")).toBeNull();

    fireEvent.click(screen.getByTestId("publish-site-default-workflow-edit"));
    fireEvent.change(screen.getByTestId("publish-site-default-workflow-choice"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-save"));
    expect(updateSiteMock).not.toHaveBeenCalled();
    expect(
      screen.getByTestId("publish-site-default-workflow-error").textContent?.toLowerCase(),
    ).toContain("empty");
    expect(screen.queryByTestId("publish-site-default-workflow-saved")).toBeNull();
    expect(screen.getByTestId("publish-site-default-workflow-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-default-workflow")).toHaveTextContent("Standard");
  });

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])("keeps HTTP %s on the form without a success notice", async (status, snippet) => {
    updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
    render(<SiteDefaultWorkflowPanel siteName="NightlySite" />);
    await screen.findByText("Standard");
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-edit"));
    fireEvent.change(await screen.findByTestId("publish-site-default-workflow-choice"), {
      target: { value: "Local Content" },
    });
    fireEvent.click(screen.getByTestId("publish-site-default-workflow-save"));
    const alert = await screen.findByTestId("publish-site-default-workflow-error");
    expect(alert.textContent?.toLowerCase()).toContain(snippet);
    expect(screen.getByTestId("publish-site-default-workflow-form")).toBeTruthy();
    expect(screen.getByTestId("publish-site-default-workflow")).toHaveTextContent("Standard");
    expect(screen.queryByTestId("publish-site-default-workflow-saved")).toBeNull();
  });
});

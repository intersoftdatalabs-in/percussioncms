/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { afterEach, describe, expect, it, vi } from "vitest";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { SiteWorkspace } from "@/publishing/sections/SiteWorkspace";

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
  publishIncrementalWithApproval: vi.fn(),
  getIncrementalItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  getIncrementalRelatedItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([
    {
      serverId: 7,
      serverName: "FTP-Prod",
      name: "FTP-Prod",
      siteId: 42,
      serverType: "PRODUCTION",
    },
  ]),
  createServer: vi.fn(),
  deleteServer: vi.fn(),
  getServer: vi.fn(),
  updateServer: vi.fn(),
  isEC2Instance: vi.fn().mockResolvedValue(false),
  fetchAvailableRegions: vi.fn().mockResolvedValue([]),
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([]),
}));

const publishApi = await import("@/api/publishing/publishApi");

function renderWorkspace(): void {
  render(
    <DirtyFormProvider>
      <SiteWorkspace
        site={{ name: "MySite", siteId: 42 }}
        initialServerId="7"
        onBack={vi.fn()}
      />
    </DirtyFormProvider>,
  );
}

describe("SiteWorkspace full publish (issue #936)", () => {
  afterEach(() => {
    vi.mocked(publishApi.publishSite).mockReset();
    vi.mocked(publishApi.incrementalPublishSite).mockReset();
    vi.mocked(publishApi.publishIncrementalWithApproval).mockReset();
  });

  it("renders the FTP BADCONFIG warning when connectivity fails", async () => {
    vi.mocked(publishApi.publishSite).mockResolvedValue({
      SitePublishResponse: {
        status: "BADCONFIG",
        delivered: "0",
        failures: "0",
        jobid: 0,
        warningMessage:
          "Could not connect to publishing server, please check publishing server configuration.",
      },
    });

    renderWorkspace();

    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });

    fireEvent.click(screen.getByRole("button", { name: /Full$/ }));

    await waitFor(() => {
      expect(
        screen.getByText(
          "Could not connect to publishing server, please check publishing server configuration.",
        ),
      ).toBeTruthy();
    });

    expect(publishApi.publishSite).toHaveBeenCalledWith("MySite", "FTP-Prod");
  });

  it("shows success for a normally completed publish", async () => {
    vi.mocked(publishApi.publishSite).mockResolvedValue({
      SitePublishResponse: {
        status: "Edition completed",
        delivered: "1",
        failures: "0",
        jobid: 999,
      },
    });

    renderWorkspace();

    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });

    fireEvent.click(screen.getByRole("button", { name: /Full$/ }));

    await waitFor(() => {
      expect(screen.getByText("perc.ui.publish.title@Publish Request")).toBeTruthy();
    });
  });

  it("uses the localized FORBIDDEN message when a 403 is rejected by the catch branch", async () => {
    vi.mocked(publishApi.publishSite).mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "You shall not pass",
    });

    renderWorkspace();

    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });

    fireEvent.click(screen.getByRole("button", { name: /Full$/ }));

    await waitFor(() => {
      expect(
        screen.getByText("perc.ui.publish.modern@Publish Forbidden"),
      ).toBeTruthy();
    });
    expect(screen.queryByText("You shall not pass")).toBeNull();
  });

  it("renders BADCONFIG for the incremental publish path", async () => {
    vi.mocked(publishApi.incrementalPublishSite).mockResolvedValue({
      SitePublishResponse: {
        status: "BADCONFIG",
        delivered: "0",
        failures: "0",
        jobid: 0,
        warningMessage:
          "Could not connect to publishing server, please check publishing server configuration.",
      },
    });

    renderWorkspace();

    await waitFor(() => {
      expect(screen.getByText("FTP-Prod")).toBeTruthy();
    });

    const incrementalButton = screen.getByTestId("publish-incremental-confirm");
    fireEvent.click(incrementalButton);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Could not connect to publishing server, please check publishing server configuration.",
        ),
      ).toBeTruthy();
    });

    expect(publishApi.incrementalPublishSite).toHaveBeenCalledWith(
      "MySite",
      "FTP-Prod",
    );
  });
});

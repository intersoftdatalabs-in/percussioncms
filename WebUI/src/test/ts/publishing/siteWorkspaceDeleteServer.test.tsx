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
import { afterEach, describe, expect, it, vi } from "vitest";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { SiteWorkspace } from "@/publishing/sections/SiteWorkspace";

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
  publishIncrementalWithApproval: vi.fn(),
  getIncrementalItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
  removeIncrementalQueueItem: vi.fn().mockResolvedValue(undefined),
  clearIncrementalQueue: vi.fn().mockResolvedValue(undefined),
  getIncrementalRelatedItems: vi.fn().mockResolvedValue({ items: [], totalCount: 0 }),
}));

const ftp = {
  serverId: 7,
  serverName: "FTP-Prod",
  name: "FTP-Prod",
  siteId: 42,
  serverType: "PRODUCTION",
  type: "File",
};

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([
    {
      serverId: 7,
      serverName: "FTP-Prod",
      name: "FTP-Prod",
      siteId: 42,
      serverType: "PRODUCTION",
      type: "File",
    },
  ]),
  createServer: vi.fn(),
  deleteServer: vi.fn(),
  getServer: vi.fn().mockResolvedValue({
    serverId: 7,
    serverName: "FTP-Prod",
    name: "FTP-Prod",
    siteId: 42,
    serverType: "PRODUCTION",
    type: "File",
  }),
  updateServer: vi.fn(),
  isEC2Instance: vi.fn().mockResolvedValue(false),
  fetchAvailableRegions: vi.fn().mockResolvedValue([]),
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn().mockResolvedValue({
    itemId: "",
    startDate: "",
    endDate: "",
    comments: "",
  }),
  saveItemScheduleDates: vi.fn().mockResolvedValue(undefined),
}));

const serversApi = await import("@/api/publishing/serversApi");

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

async function openEditor(): Promise<void> {
  renderWorkspace();
  await screen.findByTestId("publish-edit-server");
  fireEvent.click(screen.getByTestId("publish-edit-server"));
  await screen.findByTestId("publish-delete-server");
}

describe("SiteWorkspace delete delivery server", () => {
  afterEach(() => {
    vi.mocked(serversApi.deleteServer).mockReset();
    vi.mocked(serversApi.listServers).mockResolvedValue([ftp]);
    vi.unstubAllGlobals();
  });

  it("confirm removes the server from the list", async () => {
    vi.stubGlobal("confirm", vi.fn().mockReturnValue(true));
    vi.mocked(serversApi.deleteServer).mockImplementation(async () => {
      vi.mocked(serversApi.listServers).mockResolvedValue([]);
      return [];
    });
    await openEditor();
    fireEvent.click(screen.getByTestId("publish-delete-server"));
    await waitFor(() => expect(serversApi.deleteServer).toHaveBeenCalledWith(42, "7"));
    await waitFor(() => {
      expect(screen.queryByTestId("publish-server-editor")).toBeNull();
    });
    expect(screen.queryByText("FTP-Prod")).toBeNull();
  });

  it("cancel does not delete", async () => {
    vi.stubGlobal("confirm", vi.fn().mockReturnValue(false));
    await openEditor();
    fireEvent.click(screen.getByTestId("publish-delete-server"));
    expect(serversApi.deleteServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("publish-server-editor")).toBeTruthy();
  });

  it("failure text stays and the list is unchanged", async () => {
    vi.stubGlobal("confirm", vi.fn().mockReturnValue(true));
    vi.mocked(serversApi.deleteServer).mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: {
        message: "The server is being used by other user and cannot be deleted.",
      },
    });
    vi.mocked(serversApi.listServers).mockResolvedValue([ftp]);
    await openEditor();
    const listsBefore = vi.mocked(serversApi.listServers).mock.calls.length;
    fireEvent.click(screen.getByTestId("publish-delete-server"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/being used/i);
    expect(vi.mocked(serversApi.listServers).mock.calls.length).toBe(listsBefore);
    expect(screen.getByTestId("publish-server-editor")).toBeTruthy();
  });
});

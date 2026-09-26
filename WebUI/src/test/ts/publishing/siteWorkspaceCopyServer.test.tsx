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

const ftpDetail = {
  ...ftp,
  isDefault: true,
  properties: [
    { key: "driver", value: "FTP" },
    { key: "folder", value: "/pub" },
    { key: "password", value: "s3cr3t" },
  ],
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
  getServer: vi.fn(),
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

describe("SiteWorkspace copy delivery server", () => {
  afterEach(() => {
    vi.mocked(serversApi.createServer).mockReset();
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.listServers).mockResolvedValue([ftp]);
    vi.unstubAllGlobals();
  });

  it("creates a same-site copy and shows the new name", async () => {
    vi.stubGlobal("prompt", vi.fn().mockReturnValue("FTP-Copy"));
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpDetail);
    vi.mocked(serversApi.createServer).mockImplementation(async () => {
      vi.mocked(serversApi.listServers).mockResolvedValue([
        ftp,
        { ...ftp, serverId: 8, serverName: "FTP-Copy", name: "FTP-Copy" },
      ]);
      return {};
    });
    renderWorkspace();
    fireEvent.click(await screen.findByTestId("publish-copy-server"));
    await waitFor(() => expect(serversApi.createServer).toHaveBeenCalled());
    const body = vi.mocked(serversApi.createServer).mock.calls[0][2] as {
      serverInfo: { properties: { key: string; value: string }[] };
    };
    expect(vi.mocked(serversApi.createServer).mock.calls[0][0]).toBe(42);
    expect(vi.mocked(serversApi.createServer).mock.calls[0][1]).toBe("FTP-Copy");
    const keys = body.serverInfo.properties.map((p) => p.key);
    expect(keys).toContain("driver");
    expect(keys).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("s3cr3t");
    expect(await screen.findByRole("button", { name: /FTP-Copy/ })).toBeTruthy();
  });

  it("blank name is not a successful copy", async () => {
    vi.stubGlobal("prompt", vi.fn().mockReturnValue("  "));
    renderWorkspace();
    fireEvent.click(await screen.findByTestId("publish-copy-server"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/required/i);
    expect(serversApi.getServer).not.toHaveBeenCalled();
    expect(serversApi.createServer).not.toHaveBeenCalled();
  });

  it("HTTP 409 stays an error and does not add the name", async () => {
    vi.stubGlobal("prompt", vi.fn().mockReturnValue("FTP-Prod"));
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpDetail);
    vi.mocked(serversApi.createServer).mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Publish server name already exists" },
    });
    renderWorkspace();
    fireEvent.click(await screen.findByTestId("publish-copy-server"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/already exists/i);
    expect(screen.queryByRole("button", { name: /^FTP-Copy/ })).toBeNull();
  });
});

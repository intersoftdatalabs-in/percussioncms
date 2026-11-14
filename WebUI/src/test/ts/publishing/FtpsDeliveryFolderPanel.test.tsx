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
import { FtpsDeliveryFolderPanel } from "@/publishing/components/FtpsDeliveryFolderPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { FTPS_FOLDER_MAX_LENGTH } from "@/publishing/deliveryServerFtpsFolder";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "secret-pass";

function ftpsServer(folder = "/kept/folder") {
  return {
    serverId: "9",
    serverName: "ftps-prod",
    properties: [
      { key: "driver", value: "FTPS" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "990" },
      { key: "userid", value: "ftpsuser" },
      { key: "folder", value: folder },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpsDeliveryFolderPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpsDeliveryFolderPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write an FTP folder and does not show the password", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "4",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "old.example" },
        { key: "port", value: "2121" },
        { key: "userid", value: "ftpuser" },
        { key: "folder", value: "/kept/folder" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-folder-open"));
    expect(await screen.findByTestId("ftps-folder-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(screen.queryByTestId("ftps-folder-save")).toBeNull();
    expect(screen.queryByTestId("ftps-folder-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/folder");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write an SFTP folder", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "5",
      serverName: "sftp-prod",
      properties: [
        { key: "driver", value: "SFTP" },
        { key: "serverip", value: "sftp.example" },
        { key: "port", value: "22" },
        { key: "userid", value: "sftpuser" },
        { key: "folder", value: "/kept/folder" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-folder-open"));
    expect(await screen.findByTestId("ftps-folder-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(screen.queryByTestId("ftps-folder-save")).toBeNull();
    expect(screen.queryByTestId("ftps-folder-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/folder");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the folder and keeps host, port, and user id", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpsServer())
      .mockResolvedValueOnce(ftpsServer("/remote/new"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-folder-open"));
    expect(await screen.findByTestId("ftps-folder-current")).toHaveTextContent("/kept/folder");
    expect(screen.getByTestId("ftps-folder-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftps-folder-port")).toHaveTextContent("990");
    expect(screen.getByTestId("ftps-folder-user")).toHaveTextContent("ftpsuser");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("ftps-folder-input"), {
      target: { value: "  /remote/new  " },
    });
    fireEvent.click(screen.getByTestId("ftps-folder-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "folder", value: "/remote/new" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftps-folder-current")).toHaveTextContent("/remote/new"),
    );
    expect(screen.getByTestId("ftps-folder-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftps-folder-port")).toHaveTextContent("990");
    expect(screen.getByTestId("ftps-folder-user")).toHaveTextContent("ftpsuser");
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/folder");
  });

  it("clears the folder when the field is blank and does not write a cancel or an overlong folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpsServer())
      .mockResolvedValueOnce(ftpsServer())
      .mockResolvedValueOnce(ftpsServer(""));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-folder-open"));
    await screen.findByTestId("ftps-folder-input");

    fireEvent.change(screen.getByTestId("ftps-folder-input"), {
      target: { value: "/".repeat(FTPS_FOLDER_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("ftps-folder-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/255 characters or fewer/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-folder-current")).toHaveTextContent("/kept/folder");

    fireEvent.change(screen.getByTestId("ftps-folder-input"), {
      target: { value: "/remote/new" },
    });
    fireEvent.click(screen.getByTestId("ftps-folder-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-folder-current")).toHaveTextContent("/kept/folder");
    expect(screen.queryByTestId("ftps-folder-form")).toBeNull();

    fireEvent.click(screen.getByTestId("ftps-folder-open"));
    await screen.findByTestId("ftps-folder-input");
    fireEvent.change(screen.getByTestId("ftps-folder-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftps-folder-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "folder", value: "" }] },
    });
    await waitFor(() => expect(screen.getByTestId("ftps-folder-current")).toHaveTextContent("—"));
    expect(screen.getByTestId("ftps-folder-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftps-folder-user")).toHaveTextContent("ftpsuser");
    expect(document.body.textContent).not.toContain(PASSWORD);
    vi.unstubAllGlobals();
  });

  it("keeps the previous folder on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-folder-open"));
    await screen.findByTestId("ftps-folder-input");

    const cases = [
      [400, "FTPS folder must be 255 characters or fewer.", /255 characters or fewer|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftps-folder-input"), {
        target: { value: `/remote/${status}` },
      });
      fireEvent.click(screen.getByTestId("ftps-folder-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftps-folder-current")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftps-folder-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("ftps-folder-port")).toHaveTextContent("990");
      expect(screen.getByTestId("ftps-folder-user")).toHaveTextContent("ftpsuser");
      expect(screen.getByTestId("ftps-folder-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

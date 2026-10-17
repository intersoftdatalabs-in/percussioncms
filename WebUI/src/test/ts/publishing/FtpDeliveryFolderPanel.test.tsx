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
import { FtpDeliveryFolderPanel } from "@/publishing/components/FtpDeliveryFolderPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { FTP_FOLDER_MAX_LENGTH } from "@/publishing/deliveryServerFtpFolder";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpServer(folder = "/kept/folder") {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: folder },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpDeliveryFolderPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpDeliveryFolderPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write a Local folder and does not show the password", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "local-files",
      properties: [
        { key: "driver", value: "Local" },
        { key: "folder", value: "/kept/local" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-folder-open"));
    expect(await screen.findByTestId("ftp-folder-not-ftp")).toHaveTextContent(
      /only on an FTP delivery server/i,
    );
    expect(screen.queryByTestId("ftp-folder-save")).toBeNull();
    expect(screen.queryByTestId("ftp-folder-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the folder and keeps host, port, and user", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(ftpServer("/remote/new"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-folder-open"));
    expect(await screen.findByTestId("ftp-folder-current")).toHaveTextContent("/kept/folder");
    expect(screen.getByTestId("ftp-folder-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-folder-port")).toHaveTextContent("2121");
    expect(screen.getByTestId("ftp-folder-user")).toHaveTextContent("ftpuser");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("ftp-folder-input"), {
      target: { value: "  /remote/new  " },
    });
    fireEvent.click(screen.getByTestId("ftp-folder-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "folder", value: "/remote/new" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftp-folder-current")).toHaveTextContent("/remote/new"),
    );
    expect(screen.getByTestId("ftp-folder-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-folder-port")).toHaveTextContent("2121");
    expect(screen.getByTestId("ftp-folder-user")).toHaveTextContent("ftpuser");
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/folder");
  });

  it("clears the folder when the field is blank and does not write a cancel or an overlong folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(ftpServer(""));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-folder-open"));
    await screen.findByTestId("ftp-folder-input");

    fireEvent.change(screen.getByTestId("ftp-folder-input"), {
      target: { value: "/".repeat(FTP_FOLDER_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("ftp-folder-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/255 characters or fewer/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-folder-current")).toHaveTextContent("/kept/folder");

    fireEvent.change(screen.getByTestId("ftp-folder-input"), {
      target: { value: "/remote/new" },
    });
    fireEvent.click(screen.getByTestId("ftp-folder-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-folder-current")).toHaveTextContent("/kept/folder");
    expect(screen.queryByTestId("ftp-folder-form")).toBeNull();

    fireEvent.click(screen.getByTestId("ftp-folder-open"));
    await screen.findByTestId("ftp-folder-input");
    fireEvent.change(screen.getByTestId("ftp-folder-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftp-folder-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "folder", value: "" }] },
    });
    await waitFor(() => expect(screen.getByTestId("ftp-folder-current")).toHaveTextContent("—"));
    expect(screen.getByTestId("ftp-folder-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-folder-user")).toHaveTextContent("ftpuser");
    expect(document.body.textContent).not.toContain(PASSWORD);
    vi.unstubAllGlobals();
  });

  it("keeps the previous folder on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-folder-open"));
    await screen.findByTestId("ftp-folder-input");

    const cases = [
      [400, "FTP folder must be 255 characters or fewer.", /255 characters or fewer|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftp-folder-input"), {
        target: { value: `/remote/${status}` },
      });
      fireEvent.click(screen.getByTestId("ftp-folder-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftp-folder-current")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftp-folder-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("ftp-folder-port")).toHaveTextContent("2121");
      expect(screen.getByTestId("ftp-folder-user")).toHaveTextContent("ftpuser");
      expect(screen.getByTestId("ftp-folder-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

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
import { SftpDeliveryPortPanel } from "@/publishing/components/SftpDeliveryPortPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSPHRASE = "s3cret-passphrase";
const PRIVATE_KEY = "stored-private-key";

function sftpServer(port = "22") {
  return {
    serverId: "9",
    serverName: "sftp-prod",
    properties: [
      { key: "driver", value: "SFTP" },
      { key: "serverip", value: "sftp.example" },
      { key: "port", value: port },
      { key: "userid", value: "sftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSPHRASE },
      { key: "privateKey", value: PRIVATE_KEY },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <SftpDeliveryPortPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("SftpDeliveryPortPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write a Local folder and does not show the secrets", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "local-files",
      properties: [
        { key: "driver", value: "Local" },
        { key: "folder", value: "/kept/local" },
        { key: "password", value: PASSPHRASE },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-port-open"));
    expect(await screen.findByTestId("sftp-port-not-sftp")).toHaveTextContent(
      /only on an SFTP delivery server/i,
    );
    expect(screen.queryByTestId("sftp-port-save")).toBeNull();
    expect(screen.queryByTestId("sftp-port-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write an FTP port", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "4",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "ftp.example" },
        { key: "port", value: "21" },
        { key: "userid", value: "ftpuser" },
        { key: "folder", value: "/ftp/folder" },
        { key: "password", value: PASSPHRASE },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-port-open"));
    expect(await screen.findByTestId("sftp-port-not-sftp")).toHaveTextContent(
      /only on an SFTP delivery server/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the port and keeps host, user, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(sftpServer())
      .mockResolvedValueOnce(sftpServer("2222"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-port-open"));
    expect(await screen.findByTestId("sftp-port-current")).toHaveTextContent("22");
    expect(screen.getByTestId("sftp-port-host")).toHaveTextContent("sftp.example");
    expect(screen.getByTestId("sftp-port-user")).toHaveTextContent("sftpuser");
    expect(screen.getByTestId("sftp-port-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);

    fireEvent.change(screen.getByTestId("sftp-port-input"), {
      target: { value: "2222" },
    });
    fireEvent.click(screen.getByTestId("sftp-port-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "port", value: "2222" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("sftp-port-current")).toHaveTextContent("2222"),
    );
    expect(screen.getByTestId("sftp-port-host")).toHaveTextContent("sftp.example");
    expect(screen.getByTestId("sftp-port-user")).toHaveTextContent("sftpuser");
    expect(screen.getByTestId("sftp-port-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
  });

  it("does not write a blank or non-numeric port or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(sftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-port-open"));
    await screen.findByTestId("sftp-port-input");

    fireEvent.change(screen.getByTestId("sftp-port-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("sftp-port-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/SFTP port is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("sftp-port-current")).toHaveTextContent("22");

    fireEvent.change(screen.getByTestId("sftp-port-input"), {
      target: { value: "21a" },
    });
    fireEvent.click(screen.getByTestId("sftp-port-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/only digits/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("sftp-port-current")).toHaveTextContent("22");

    fireEvent.change(screen.getByTestId("sftp-port-input"), {
      target: { value: "2222" },
    });
    fireEvent.click(screen.getByTestId("sftp-port-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("sftp-port-current")).toHaveTextContent("22");
    expect(screen.queryByTestId("sftp-port-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous port on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(sftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-port-open"));
    await screen.findByTestId("sftp-port-input");

    const cases = [
      [400, "SFTP port is required.", /SFTP port is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("sftp-port-input"), {
        target: { value: `222${status}` },
      });
      fireEvent.click(screen.getByTestId("sftp-port-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("sftp-port-current")).toHaveTextContent("22");
      expect(screen.getByTestId("sftp-port-host")).toHaveTextContent("sftp.example");
      expect(screen.getByTestId("sftp-port-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("sftp-port-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
  });
});

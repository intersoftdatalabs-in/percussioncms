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
import { FtpsDeliveryPortPanel } from "@/publishing/components/FtpsDeliveryPortPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpsServer(port = "21") {
  return {
    serverId: "9",
    serverName: "ftps-prod",
    properties: [
      { key: "driver", value: "FTPS" },
      { key: "serverip", value: "ftps.example" },
      { key: "port", value: port },
      { key: "userid", value: "ftpsuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpsDeliveryPortPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpsDeliveryPortPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write a Local folder and does not show the secret", async () => {
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
    fireEvent.click(screen.getByTestId("ftps-port-open"));
    expect(await screen.findByTestId("ftps-port-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(screen.queryByTestId("ftps-port-save")).toBeNull();
    expect(screen.queryByTestId("ftps-port-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
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
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-port-open"));
    expect(await screen.findByTestId("ftps-port-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write an SFTP port", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "5",
      serverName: "sftp-prod",
      properties: [
        { key: "driver", value: "SFTP" },
        { key: "serverip", value: "sftp.example" },
        { key: "port", value: "22" },
        { key: "userid", value: "sftpuser" },
        { key: "folder", value: "/sftp/folder" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-port-open"));
    expect(await screen.findByTestId("ftps-port-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the port and keeps host, user, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpsServer())
      .mockResolvedValueOnce(ftpsServer("990"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-port-open"));
    expect(await screen.findByTestId("ftps-port-current")).toHaveTextContent("21");
    expect(screen.getByTestId("ftps-port-host")).toHaveTextContent("ftps.example");
    expect(screen.getByTestId("ftps-port-user")).toHaveTextContent("ftpsuser");
    expect(screen.getByTestId("ftps-port-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("ftps-port-input"), {
      target: { value: "990" },
    });
    fireEvent.click(screen.getByTestId("ftps-port-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "port", value: "990" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftps-port-current")).toHaveTextContent("990"),
    );
    expect(screen.getByTestId("ftps-port-host")).toHaveTextContent("ftps.example");
    expect(screen.getByTestId("ftps-port-user")).toHaveTextContent("ftpsuser");
    expect(screen.getByTestId("ftps-port-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank or non-numeric port or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-port-open"));
    await screen.findByTestId("ftps-port-input");

    fireEvent.change(screen.getByTestId("ftps-port-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftps-port-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/FTPS port is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-port-current")).toHaveTextContent("21");

    fireEvent.change(screen.getByTestId("ftps-port-input"), {
      target: { value: "21a" },
    });
    fireEvent.click(screen.getByTestId("ftps-port-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/only digits/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-port-current")).toHaveTextContent("21");

    fireEvent.change(screen.getByTestId("ftps-port-input"), {
      target: { value: "990" },
    });
    fireEvent.click(screen.getByTestId("ftps-port-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-port-current")).toHaveTextContent("21");
    expect(screen.queryByTestId("ftps-port-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous port on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-port-open"));
    await screen.findByTestId("ftps-port-input");

    const cases = [
      [400, "FTPS port is required.", /FTPS port is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftps-port-input"), {
        target: { value: `22${status}` },
      });
      fireEvent.click(screen.getByTestId("ftps-port-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftps-port-current")).toHaveTextContent("21");
      expect(screen.getByTestId("ftps-port-host")).toHaveTextContent("ftps.example");
      expect(screen.getByTestId("ftps-port-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftps-port-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

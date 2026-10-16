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
import { FtpDeliveryPortPanel } from "@/publishing/components/FtpDeliveryPortPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpServer(port = "2121") {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: port },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpDeliveryPortPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpDeliveryPortPanel", () => {
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
    fireEvent.click(screen.getByTestId("ftp-port-open"));
    expect(await screen.findByTestId("ftp-port-not-ftp")).toHaveTextContent(
      /only on an FTP delivery server/i,
    );
    expect(screen.queryByTestId("ftp-port-save")).toBeNull();
    expect(screen.queryByTestId("ftp-port-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the port and keeps host, user, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(ftpServer("2222"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-port-open"));
    expect(await screen.findByTestId("ftp-port-current")).toHaveTextContent("2121");
    expect(screen.getByTestId("ftp-port-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-port-user")).toHaveTextContent("ftpuser");
    expect(screen.getByTestId("ftp-port-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("ftp-port-input"), {
      target: { value: "2222" },
    });
    fireEvent.click(screen.getByTestId("ftp-port-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "port", value: "2222" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftp-port-current")).toHaveTextContent("2222"),
    );
    expect(screen.getByTestId("ftp-port-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-port-user")).toHaveTextContent("ftpuser");
    expect(screen.getByTestId("ftp-port-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank or non-numeric port or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-port-open"));
    await screen.findByTestId("ftp-port-input");

    fireEvent.change(screen.getByTestId("ftp-port-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftp-port-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/FTP port is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-port-current")).toHaveTextContent("2121");

    fireEvent.change(screen.getByTestId("ftp-port-input"), {
      target: { value: "21a" },
    });
    fireEvent.click(screen.getByTestId("ftp-port-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/only digits/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-port-current")).toHaveTextContent("2121");

    fireEvent.change(screen.getByTestId("ftp-port-input"), {
      target: { value: "3333" },
    });
    fireEvent.click(screen.getByTestId("ftp-port-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-port-current")).toHaveTextContent("2121");
    expect(screen.queryByTestId("ftp-port-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous port on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-port-open"));
    await screen.findByTestId("ftp-port-input");

    const cases = [
      [400, "FTP port is required.", /FTP port is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftp-port-input"), {
        target: { value: String(2000 + status) },
      });
      fireEvent.click(screen.getByTestId("ftp-port-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftp-port-current")).toHaveTextContent("2121");
      expect(screen.getByTestId("ftp-port-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("ftp-port-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftp-port-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

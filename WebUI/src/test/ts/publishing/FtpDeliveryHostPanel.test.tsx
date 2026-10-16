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
import { FtpDeliveryHostPanel } from "@/publishing/components/FtpDeliveryHostPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpServer(host = "old.example") {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: host },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpDeliveryHostPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpDeliveryHostPanel", () => {
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
    fireEvent.click(screen.getByTestId("ftp-host-open"));
    expect(await screen.findByTestId("ftp-host-not-ftp")).toHaveTextContent(
      /only on an FTP delivery server/i,
    );
    expect(screen.queryByTestId("ftp-host-save")).toBeNull();
    expect(screen.queryByTestId("ftp-host-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the host and keeps port, user, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(ftpServer("ftp.new.example"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-host-open"));
    expect(await screen.findByTestId("ftp-host-current")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-host-port")).toHaveTextContent("2121");
    expect(screen.getByTestId("ftp-host-user")).toHaveTextContent("ftpuser");
    expect(screen.getByTestId("ftp-host-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("ftp-host-input"), {
      target: { value: "ftp.new.example" },
    });
    fireEvent.click(screen.getByTestId("ftp-host-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "serverip", value: "ftp.new.example" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftp-host-current")).toHaveTextContent("ftp.new.example"),
    );
    expect(screen.getByTestId("ftp-host-port")).toHaveTextContent("2121");
    expect(screen.getByTestId("ftp-host-user")).toHaveTextContent("ftpuser");
    expect(screen.getByTestId("ftp-host-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank host or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-host-open"));
    await screen.findByTestId("ftp-host-input");

    fireEvent.change(screen.getByTestId("ftp-host-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftp-host-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/FTP host is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-host-current")).toHaveTextContent("old.example");

    fireEvent.change(screen.getByTestId("ftp-host-input"), {
      target: { value: "ftp.will-not-save.example" },
    });
    fireEvent.click(screen.getByTestId("ftp-host-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftp-host-current")).toHaveTextContent("old.example");
    expect(screen.queryByTestId("ftp-host-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous host on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-host-open"));
    await screen.findByTestId("ftp-host-input");

    const cases = [
      [400, "FTP host is required.", /FTP host is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftp-host-input"), {
        target: { value: `ftp.will-not-save.${status}` },
      });
      fireEvent.click(screen.getByTestId("ftp-host-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftp-host-current")).toHaveTextContent("old.example");
      expect(screen.getByTestId("ftp-host-port")).toHaveTextContent("2121");
      expect(screen.getByTestId("ftp-host-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftp-host-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

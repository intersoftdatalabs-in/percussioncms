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
import { SftpDeliveryHostPanel } from "@/publishing/components/SftpDeliveryHostPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSPHRASE = "s3cret-passphrase";
const PRIVATE_KEY = "stored-private-key";

function sftpServer(host = "old.example") {
  return {
    serverId: "9",
    serverName: "sftp-prod",
    properties: [
      { key: "driver", value: "SFTP" },
      { key: "serverip", value: host },
      { key: "port", value: "22" },
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
      <SftpDeliveryHostPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("SftpDeliveryHostPanel", () => {
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
    fireEvent.click(screen.getByTestId("sftp-host-open"));
    expect(await screen.findByTestId("sftp-host-not-sftp")).toHaveTextContent(
      /only on an SFTP delivery server/i,
    );
    expect(screen.queryByTestId("sftp-host-save")).toBeNull();
    expect(screen.queryByTestId("sftp-host-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the host and keeps port, user, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(sftpServer())
      .mockResolvedValueOnce(sftpServer("sftp.new.example"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-host-open"));
    expect(await screen.findByTestId("sftp-host-current")).toHaveTextContent("old.example");
    expect(screen.getByTestId("sftp-host-port")).toHaveTextContent("22");
    expect(screen.getByTestId("sftp-host-user")).toHaveTextContent("sftpuser");
    expect(screen.getByTestId("sftp-host-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);

    fireEvent.change(screen.getByTestId("sftp-host-input"), {
      target: { value: "sftp.new.example" },
    });
    fireEvent.click(screen.getByTestId("sftp-host-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "serverip", value: "sftp.new.example" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("sftp-host-current")).toHaveTextContent("sftp.new.example"),
    );
    expect(screen.getByTestId("sftp-host-port")).toHaveTextContent("22");
    expect(screen.getByTestId("sftp-host-user")).toHaveTextContent("sftpuser");
    expect(screen.getByTestId("sftp-host-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
  });

  it("does not write a blank host or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(sftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-host-open"));
    await screen.findByTestId("sftp-host-input");

    fireEvent.change(screen.getByTestId("sftp-host-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("sftp-host-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/SFTP host is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("sftp-host-current")).toHaveTextContent("old.example");

    fireEvent.change(screen.getByTestId("sftp-host-input"), {
      target: { value: "sftp.will-not-save.example" },
    });
    fireEvent.click(screen.getByTestId("sftp-host-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("sftp-host-current")).toHaveTextContent("old.example");
    expect(screen.queryByTestId("sftp-host-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous host on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(sftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-host-open"));
    await screen.findByTestId("sftp-host-input");

    const cases = [
      [400, "SFTP host is required.", /SFTP host is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("sftp-host-input"), {
        target: { value: `sftp.will-not-save.${status}` },
      });
      fireEvent.click(screen.getByTestId("sftp-host-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("sftp-host-current")).toHaveTextContent("old.example");
      expect(screen.getByTestId("sftp-host-port")).toHaveTextContent("22");
      expect(screen.getByTestId("sftp-host-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("sftp-host-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
  });
});

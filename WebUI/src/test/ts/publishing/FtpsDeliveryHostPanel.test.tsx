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
import { FtpsDeliveryHostPanel } from "@/publishing/components/FtpsDeliveryHostPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSPHRASE = "s3cret-passphrase";
const PRIVATE_KEY = "stored-private-key";

function ftpsServer(host = "old.example") {
  return {
    serverId: "9",
    serverName: "ftps-prod",
    properties: [
      { key: "driver", value: "FTPS" },
      { key: "serverip", value: host },
      { key: "port", value: "990" },
      { key: "userid", value: "ftpsuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSPHRASE },
      { key: "privateKey", value: PRIVATE_KEY },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpsDeliveryHostPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpsDeliveryHostPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write an FTP host and does not show the secrets", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "ftp.example" },
        { key: "folder", value: "/kept/ftp" },
        { key: "password", value: PASSPHRASE },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-host-open"));
    expect(await screen.findByTestId("ftps-host-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(screen.queryByTestId("ftps-host-save")).toBeNull();
    expect(screen.queryByTestId("ftps-host-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain("/kept/ftp");
    expect(document.body.textContent).not.toContain("ftp.example");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the host and keeps port, user, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpsServer())
      .mockResolvedValueOnce(ftpsServer("ftps.new.example"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-host-open"));
    expect(await screen.findByTestId("ftps-host-current")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftps-host-port")).toHaveTextContent("990");
    expect(screen.getByTestId("ftps-host-user")).toHaveTextContent("ftpsuser");
    expect(screen.getByTestId("ftps-host-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);

    fireEvent.change(screen.getByTestId("ftps-host-input"), {
      target: { value: "ftps.new.example" },
    });
    fireEvent.click(screen.getByTestId("ftps-host-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "serverip", value: "ftps.new.example" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftps-host-current")).toHaveTextContent("ftps.new.example"),
    );
    expect(screen.getByTestId("ftps-host-port")).toHaveTextContent("990");
    expect(screen.getByTestId("ftps-host-user")).toHaveTextContent("ftpsuser");
    expect(screen.getByTestId("ftps-host-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
  });

  it("does not write a blank host or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-host-open"));
    await screen.findByTestId("ftps-host-input");

    fireEvent.change(screen.getByTestId("ftps-host-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftps-host-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/FTPS host is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-host-current")).toHaveTextContent("old.example");

    fireEvent.change(screen.getByTestId("ftps-host-input"), {
      target: { value: "ftps.will-not-save.example" },
    });
    fireEvent.click(screen.getByTestId("ftps-host-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-host-current")).toHaveTextContent("old.example");
    expect(screen.queryByTestId("ftps-host-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous host on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-host-open"));
    await screen.findByTestId("ftps-host-input");

    const cases = [
      [400, "FTPS host is required.", /FTPS host is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftps-host-input"), {
        target: { value: `ftps.will-not-save.${status}` },
      });
      fireEvent.click(screen.getByTestId("ftps-host-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftps-host-current")).toHaveTextContent("old.example");
      expect(screen.getByTestId("ftps-host-port")).toHaveTextContent("990");
      expect(screen.getByTestId("ftps-host-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftps-host-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
  });
});

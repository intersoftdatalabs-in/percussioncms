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
import { FtpsDeliveryUserPanel } from "@/publishing/components/FtpsDeliveryUserPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { FTPS_USER_MAX_LENGTH } from "@/publishing/deliveryServerFtpsUser";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpsServer(userId = "ftpsuser") {
  return {
    serverId: "9",
    serverName: "ftps-prod",
    properties: [
      { key: "driver", value: "FTPS" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "990" },
      { key: "userid", value: userId },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpsDeliveryUserPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpsDeliveryUserPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write an FTP port and does not show the password", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "ftp.example" },
        { key: "port", value: "2121" },
        { key: "userid", value: "ftpuser" },
        { key: "folder", value: "/kept/ftp" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-user-open"));
    expect(await screen.findByTestId("ftps-user-not-ftps")).toHaveTextContent(
      /only on an FTPS delivery server/i,
    );
    expect(screen.queryByTestId("ftps-user-save")).toBeNull();
    expect(screen.queryByTestId("ftps-user-folder")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/ftp");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the user id and keeps host, port, and folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpsServer())
      .mockResolvedValueOnce(ftpsServer("cmsftps"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-user-open"));
    expect(await screen.findByTestId("ftps-user-current")).toHaveTextContent("ftpsuser");
    expect(screen.getByTestId("ftps-user-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftps-user-port")).toHaveTextContent("990");
    expect(screen.getByTestId("ftps-user-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("ftps-user-input"), {
      target: { value: "cmsftps" },
    });
    fireEvent.click(screen.getByTestId("ftps-user-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "userid", value: "cmsftps" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("ftps-user-current")).toHaveTextContent("cmsftps"),
    );
    expect(screen.getByTestId("ftps-user-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftps-user-port")).toHaveTextContent("990");
    expect(screen.getByTestId("ftps-user-folder")).toHaveTextContent("/kept/folder");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank or overlong user id or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-user-open"));
    await screen.findByTestId("ftps-user-input");

    fireEvent.change(screen.getByTestId("ftps-user-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftps-user-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/FTPS user id is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-user-current")).toHaveTextContent("ftpsuser");

    fireEvent.change(screen.getByTestId("ftps-user-input"), {
      target: { value: "u".repeat(FTPS_USER_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("ftps-user-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/255 characters or fewer/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-user-current")).toHaveTextContent("ftpsuser");

    fireEvent.change(screen.getByTestId("ftps-user-input"), {
      target: { value: "cmsftps" },
    });
    fireEvent.click(screen.getByTestId("ftps-user-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("ftps-user-current")).toHaveTextContent("ftpsuser");
    expect(screen.queryByTestId("ftps-user-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous user id on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpsServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftps-user-open"));
    await screen.findByTestId("ftps-user-input");

    const cases = [
      [400, "FTPS user id is required.", /FTPS user id is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftps-user-input"), {
        target: { value: `user${status}` },
      });
      fireEvent.click(screen.getByTestId("ftps-user-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftps-user-current")).toHaveTextContent("ftpsuser");
      expect(screen.getByTestId("ftps-user-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("ftps-user-port")).toHaveTextContent("990");
      expect(screen.getByTestId("ftps-user-folder")).toHaveTextContent("/kept/folder");
      expect(screen.getByTestId("ftps-user-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

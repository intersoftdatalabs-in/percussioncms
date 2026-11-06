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
import { FtpDeliveryPasswordPanel } from "@/publishing/components/FtpDeliveryPasswordPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { FTP_PASSWORD_MAX_LENGTH } from "@/publishing/deliveryServerFtpPassword";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";
const MASK = "passwordEntry";

function ftpServer() {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function maskedServer() {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: MASK },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <FtpDeliveryPasswordPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("FtpDeliveryPasswordPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write on a Local server and never shows the stored secret", async () => {
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
    fireEvent.click(screen.getByTestId("ftp-password-open"));
    expect(await screen.findByTestId("ftp-password-not-ftp")).toHaveTextContent(
      /only on an FTP delivery server/i,
    );
    expect(screen.queryByTestId("ftp-password-save")).toBeNull();
    expect(screen.queryByTestId("ftp-password-summary")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("opens with an empty secret input and saves a password-only base64 body", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(maskedServer());
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-password-open"));
    expect(await screen.findByTestId("ftp-password-summary")).toBeTruthy();
    expect(screen.getByTestId("ftp-password-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-password-port")).toHaveTextContent("2121");
    expect(screen.getByTestId("ftp-password-user")).toHaveTextContent("ftpuser");
    expect(screen.getByTestId("ftp-password-folder")).toHaveTextContent("/kept/folder");
    expect(screen.getByTestId("ftp-password-status")).toHaveTextContent(/never shown/i);
    // The stored secret (and the masked placeholder) never reach the form.
    expect(document.body.textContent).not.toContain(PASSWORD);
    const input = screen.getByTestId("ftp-password-input") as HTMLInputElement;
    expect(input.value).toBe("");
    expect(input.type).toBe("password");

    fireEvent.change(input, { target: { value: "N3w-secret!" } });
    fireEvent.click(screen.getByTestId("ftp-password-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    const body = vi.mocked(serversApi.updateServer).mock.calls[0]?.[2] as {
      serverInfo: { properties: Array<{ key: string; value: string }> };
    };
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", body);
    expect(body.serverInfo.properties).toHaveLength(1);
    expect(body.serverInfo.properties[0]?.key).toBe("password");
    const sent = String(body.serverInfo.properties[0]?.value ?? "");
    expect(sent).not.toContain("N3w-secret!");
    expect(atob(sent)).toBe("N3w-secret!");

    // After success the input is cleared and the mask is never echoed into it.
    await waitFor(() => expect(screen.queryByTestId("ftp-password-form")).toBeNull());
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("N3w-secret!");
    expect(screen.getByTestId("ftp-password-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("ftp-password-user")).toHaveTextContent("ftpuser");
  });

  it("does not write a blank or overlong password or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-password-open"));
    await screen.findByTestId("ftp-password-input");

    fireEvent.change(screen.getByTestId("ftp-password-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("ftp-password-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/FTP password is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("ftp-password-input"), {
      target: { value: "p".repeat(FTP_PASSWORD_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("ftp-password-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/255 characters or fewer/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("ftp-password-input"), {
      target: { value: "N3w-secret!" },
    });
    fireEvent.click(screen.getByTestId("ftp-password-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.queryByTestId("ftp-password-form")).toBeNull();
    expect(document.body.textContent).not.toContain("N3w-secret!");
    expect(document.body.textContent).not.toContain(PASSWORD);
    vi.unstubAllGlobals();
  });

  it("keeps the previous secret untouched on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("ftp-password-open"));
    await screen.findByTestId("ftp-password-input");

    const cases = [
      [400, "FTP password is required.", /FTP password is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("ftp-password-input"), {
        target: { value: `pw-${status}-secret` },
      });
      fireEvent.click(screen.getByTestId("ftp-password-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("ftp-password-status")).toHaveTextContent(/never shown/i);
      expect(screen.getByTestId("ftp-password-form")).toBeTruthy();
      expect(document.body.textContent).not.toContain(`pw-${status}-secret`);
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

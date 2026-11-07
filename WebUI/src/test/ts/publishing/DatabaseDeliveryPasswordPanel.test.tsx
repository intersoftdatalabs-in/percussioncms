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
import { DatabaseDeliveryPasswordPanel } from "@/publishing/components/DatabaseDeliveryPasswordPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { DATABASE_PASSWORD_MAX_LENGTH } from "@/publishing/deliveryServerDatabasePassword";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";
const MASK = "passwordEntry";

function databaseServer() {
  return {
    serverId: "9",
    serverName: "mysql-prod",
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: "old.example" },
      { key: "port", value: "3306" },
      { key: "userid", value: "cmsdb" },
      { key: "database", value: "rxdb" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function maskedServer() {
  return {
    serverId: "9",
    serverName: "mysql-prod",
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: "old.example" },
      { key: "port", value: "3306" },
      { key: "userid", value: "cmsdb" },
      { key: "database", value: "rxdb" },
      { key: "password", value: MASK },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliveryPasswordPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("DatabaseDeliveryPasswordPanel", () => {
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
    fireEvent.click(screen.getByTestId("db-password-open"));
    expect(await screen.findByTestId("db-password-not-database")).toHaveTextContent(
      /only on a database delivery server/i,
    );
    expect(screen.queryByTestId("db-password-save")).toBeNull();
    expect(screen.queryByTestId("db-password-summary")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write on an FTP server and keeps the FTP secret path separate", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "4",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "ftp.example" },
        { key: "port", value: "2121" },
        { key: "userid", value: "ftpuser" },
        { key: "folder", value: "/kept/folder" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("db-password-open"));
    expect(await screen.findByTestId("db-password-not-database")).toHaveTextContent(
      /only on a database delivery server/i,
    );
    expect(screen.queryByTestId("db-password-save")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("opens with an empty secret input and saves a password-only base64 body", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(databaseServer())
      .mockResolvedValueOnce(maskedServer());
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-password-open"));
    expect(await screen.findByTestId("db-password-summary")).toBeTruthy();
    expect(screen.getByTestId("db-password-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("db-password-port")).toHaveTextContent("3306");
    expect(screen.getByTestId("db-password-user")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-password-database")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-password-status")).toHaveTextContent(/never shown/i);
    // The stored secret (and the masked placeholder) never reach the form.
    expect(document.body.textContent).not.toContain(PASSWORD);
    const input = screen.getByTestId("db-password-input") as HTMLInputElement;
    expect(input.value).toBe("");
    expect(input.type).toBe("password");

    fireEvent.change(input, { target: { value: "N3w-secret!" } });
    fireEvent.click(screen.getByTestId("db-password-save"));
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
    await waitFor(() => expect(screen.queryByTestId("db-password-form")).toBeNull());
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("N3w-secret!");
    expect(screen.getByTestId("db-password-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("db-password-user")).toHaveTextContent("cmsdb");
  });

  it("does not write a blank or overlong password or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(databaseServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-password-open"));
    await screen.findByTestId("db-password-input");

    fireEvent.change(screen.getByTestId("db-password-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-password-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /Database password is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("db-password-input"), {
      target: { value: "p".repeat(DATABASE_PASSWORD_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("db-password-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/255 characters or fewer/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("db-password-input"), {
      target: { value: "N3w-secret!" },
    });
    fireEvent.click(screen.getByTestId("db-password-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.queryByTestId("db-password-form")).toBeNull();
    expect(document.body.textContent).not.toContain("N3w-secret!");
    expect(document.body.textContent).not.toContain(PASSWORD);
    vi.unstubAllGlobals();
  });

  it("keeps the previous secret untouched on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(databaseServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-password-open"));
    await screen.findByTestId("db-password-input");

    const cases = [
      [400, "Database password is required.", /Database password is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-password-input"), {
        target: { value: `pw-${status}-secret` },
      });
      fireEvent.click(screen.getByTestId("db-password-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-password-status")).toHaveTextContent(/never shown/i);
      expect(screen.getByTestId("db-password-form")).toBeTruthy();
      expect(document.body.textContent).not.toContain(`pw-${status}-secret`);
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

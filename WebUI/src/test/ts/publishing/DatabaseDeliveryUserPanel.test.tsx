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
import { DatabaseDeliveryUserPanel } from "@/publishing/components/DatabaseDeliveryUserPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-db-password";

function mysqlServer(userId = "cmsdb") {
  return {
    serverId: "9",
    serverName: "mysql-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: "old.example" },
      { key: "port", value: "3306" },
      { key: "userid", value: userId },
      { key: "database", value: "rxdb" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliveryUserPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("DatabaseDeliveryUserPanel", () => {
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
    fireEvent.click(screen.getByTestId("db-user-open"));
    expect(await screen.findByTestId("db-user-not-database")).toHaveTextContent(
      /only on a database delivery server/i,
    );
    expect(screen.queryByTestId("db-user-save")).toBeNull();
    expect(screen.queryByTestId("db-user-database")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the user id and keeps host, port, database, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(mysqlServer())
      .mockResolvedValueOnce(mysqlServer("cmsnew"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-user-open"));
    expect(await screen.findByTestId("db-user-current")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-user-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("db-user-port")).toHaveTextContent("3306");
    expect(screen.getByTestId("db-user-database")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-user-name")).toHaveTextContent("mysql-prod");
    expect(screen.getByTestId("db-user-driver")).toHaveTextContent("MYSQL");
    expect(screen.getByTestId("db-user-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("db-user-input"), {
      target: { value: "cmsnew" },
    });
    fireEvent.click(screen.getByTestId("db-user-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "userid", value: "cmsnew" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("db-user-current")).toHaveTextContent("cmsnew"),
    );
    expect(screen.getByTestId("db-user-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("db-user-port")).toHaveTextContent("3306");
    expect(screen.getByTestId("db-user-database")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-user-name")).toHaveTextContent("mysql-prod");
    expect(screen.getByTestId("db-user-driver")).toHaveTextContent("MYSQL");
    expect(screen.getByTestId("db-user-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank user id or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-user-open"));
    await screen.findByTestId("db-user-input");

    fireEvent.change(screen.getByTestId("db-user-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-user-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/Database user id is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-user-current")).toHaveTextContent("cmsdb");

    fireEvent.change(screen.getByTestId("db-user-input"), {
      target: { value: "cmsnew" },
    });
    fireEvent.click(screen.getByTestId("db-user-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-user-current")).toHaveTextContent("cmsdb");
    expect(screen.queryByTestId("db-user-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous user id on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-user-open"));
    await screen.findByTestId("db-user-input");

    const cases = [
      [400, "Database user id is required.", /Database user id is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-user-input"), {
        target: { value: `user${status}` },
      });
      fireEvent.click(screen.getByTestId("db-user-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-user-current")).toHaveTextContent("cmsdb");
      expect(screen.getByTestId("db-user-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("db-user-database")).toHaveTextContent("rxdb");
      expect(screen.getByTestId("db-user-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

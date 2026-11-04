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
import { DatabaseDeliveryOwnerPanel } from "@/publishing/components/DatabaseDeliveryOwnerPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-mssql-password";

function mssqlServer(owner = "dbo") {
  return {
    serverId: "9",
    serverName: "sql-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "MSSQL" },
      { key: "server", value: "old-sql.example" },
      { key: "port", value: "1433" },
      { key: "userid", value: "cmsdb" },
      { key: "database", value: "rxdb" },
      { key: "owner", value: owner },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliveryOwnerPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("DatabaseDeliveryOwnerPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write a Local folder, Oracle SID, or the password", async () => {
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
    fireEvent.click(screen.getByTestId("db-owner-open"));
    expect(await screen.findByTestId("db-owner-not-mssql")).toHaveTextContent(
      /MSSQL delivery server/i,
    );
    expect(screen.queryByTestId("db-owner-save")).toBeNull();
    expect(screen.queryByTestId("db-owner-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write a MySQL database name", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "4",
      serverName: "mysql-prod",
      properties: [
        { key: "driver", value: "MYSQL" },
        { key: "server", value: "old.example" },
        { key: "database", value: "rxdb" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("db-owner-open"));
    expect(await screen.findByTestId("db-owner-not-mssql")).toHaveTextContent(
      /MSSQL delivery server/i,
    );
    expect(screen.queryByTestId("db-owner-save")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("rxdb");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the owner and keeps host, port, user, database, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(mssqlServer())
      .mockResolvedValueOnce(mssqlServer("sa"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-owner-open"));
    expect(await screen.findByTestId("db-owner-current")).toHaveTextContent("dbo");
    expect(screen.getByTestId("db-owner-host")).toHaveTextContent("old-sql.example");
    expect(screen.getByTestId("db-owner-port")).toHaveTextContent("1433");
    expect(screen.getByTestId("db-owner-user")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-owner-database")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-owner-name")).toHaveTextContent("sql-prod");
    expect(screen.getByTestId("db-owner-driver")).toHaveTextContent("MSSQL");
    expect(screen.getByTestId("db-owner-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("db-owner-input"), {
      target: { value: "sa" },
    });
    fireEvent.click(screen.getByTestId("db-owner-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "owner", value: "sa" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("db-owner-current")).toHaveTextContent("sa"),
    );
    expect(screen.getByTestId("db-owner-host")).toHaveTextContent("old-sql.example");
    expect(screen.getByTestId("db-owner-port")).toHaveTextContent("1433");
    expect(screen.getByTestId("db-owner-user")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-owner-database")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-owner-name")).toHaveTextContent("sql-prod");
    expect(screen.getByTestId("db-owner-driver")).toHaveTextContent("MSSQL");
    expect(screen.getByTestId("db-owner-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank owner or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mssqlServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-owner-open"));
    await screen.findByTestId("db-owner-input");

    fireEvent.change(screen.getByTestId("db-owner-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-owner-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/MSSQL owner is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-owner-current")).toHaveTextContent("dbo");

    fireEvent.change(screen.getByTestId("db-owner-input"), {
      target: { value: "sa" },
    });
    fireEvent.click(screen.getByTestId("db-owner-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-owner-current")).toHaveTextContent("dbo");
    expect(screen.queryByTestId("db-owner-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous owner on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mssqlServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-owner-open"));
    await screen.findByTestId("db-owner-input");

    const cases = [
      [400, "MSSQL owner is required.", /MSSQL owner is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-owner-input"), {
        target: { value: `own${status}` },
      });
      fireEvent.click(screen.getByTestId("db-owner-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-owner-current")).toHaveTextContent("dbo");
      expect(screen.getByTestId("db-owner-host")).toHaveTextContent("old-sql.example");
      expect(screen.getByTestId("db-owner-user")).toHaveTextContent("cmsdb");
      expect(screen.getByTestId("db-owner-database")).toHaveTextContent("rxdb");
      expect(screen.getByTestId("db-owner-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

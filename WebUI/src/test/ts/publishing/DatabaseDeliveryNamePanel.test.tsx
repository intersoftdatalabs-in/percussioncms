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
import { DatabaseDeliveryNamePanel } from "@/publishing/components/DatabaseDeliveryNamePanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-db-password";

function mysqlServer(databaseName = "rxdb") {
  return {
    serverId: "9",
    serverName: "mysql-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: "old.example" },
      { key: "port", value: "3306" },
      { key: "userid", value: "cmsdb" },
      { key: "database", value: databaseName },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliveryNamePanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("DatabaseDeliveryNamePanel", () => {
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
    fireEvent.click(screen.getByTestId("db-dbname-open"));
    expect(await screen.findByTestId("db-dbname-not-database")).toHaveTextContent(
      /MySQL or MSSQL delivery server/i,
    );
    expect(screen.queryByTestId("db-dbname-save")).toBeNull();
    expect(screen.queryByTestId("db-dbname-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write an Oracle SID", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "4",
      serverName: "ora-prod",
      properties: [
        { key: "driver", value: "Oracle" },
        { key: "server", value: "old-ora.example" },
        { key: "sid", value: "ORCL" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("db-dbname-open"));
    expect(await screen.findByTestId("db-dbname-not-database")).toHaveTextContent(
      /MySQL or MSSQL delivery server/i,
    );
    expect(screen.queryByTestId("db-dbname-save")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("ORCL");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the database name and keeps host, port, user, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(mysqlServer())
      .mockResolvedValueOnce(mysqlServer("rxnew"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-dbname-open"));
    expect(await screen.findByTestId("db-dbname-current")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-dbname-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("db-dbname-port")).toHaveTextContent("3306");
    expect(screen.getByTestId("db-dbname-user")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-dbname-name")).toHaveTextContent("mysql-prod");
    expect(screen.getByTestId("db-dbname-driver")).toHaveTextContent("MYSQL");
    expect(screen.getByTestId("db-dbname-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("db-dbname-input"), {
      target: { value: "rxnew" },
    });
    fireEvent.click(screen.getByTestId("db-dbname-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "database", value: "rxnew" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("db-dbname-current")).toHaveTextContent("rxnew"),
    );
    expect(screen.getByTestId("db-dbname-host")).toHaveTextContent("old.example");
    expect(screen.getByTestId("db-dbname-port")).toHaveTextContent("3306");
    expect(screen.getByTestId("db-dbname-user")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-dbname-name")).toHaveTextContent("mysql-prod");
    expect(screen.getByTestId("db-dbname-driver")).toHaveTextContent("MYSQL");
    expect(screen.getByTestId("db-dbname-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank database name or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-dbname-open"));
    await screen.findByTestId("db-dbname-input");

    fireEvent.change(screen.getByTestId("db-dbname-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-dbname-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/Database name is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-dbname-current")).toHaveTextContent("rxdb");

    fireEvent.change(screen.getByTestId("db-dbname-input"), {
      target: { value: "rxnew" },
    });
    fireEvent.click(screen.getByTestId("db-dbname-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-dbname-current")).toHaveTextContent("rxdb");
    expect(screen.queryByTestId("db-dbname-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous database name on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-dbname-open"));
    await screen.findByTestId("db-dbname-input");

    const cases = [
      [400, "Database name is required.", /Database name is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-dbname-input"), {
        target: { value: `db${status}` },
      });
      fireEvent.click(screen.getByTestId("db-dbname-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-dbname-current")).toHaveTextContent("rxdb");
      expect(screen.getByTestId("db-dbname-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("db-dbname-user")).toHaveTextContent("cmsdb");
      expect(screen.getByTestId("db-dbname-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

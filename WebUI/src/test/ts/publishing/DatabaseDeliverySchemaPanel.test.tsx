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
import { DatabaseDeliverySchemaPanel } from "@/publishing/components/DatabaseDeliverySchemaPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-ora-password";

function oracleServer(schema = "SYSTEM") {
  return {
    serverId: "9",
    serverName: "ora-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "Oracle" },
      { key: "server", value: "old-ora.example" },
      { key: "port", value: "1521" },
      { key: "userid", value: "cmsora" },
      { key: "sid", value: "ORCL" },
      { key: "schema", value: schema },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliverySchemaPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("DatabaseDeliverySchemaPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write a Local folder, MySQL database, or the password", async () => {
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
    fireEvent.click(screen.getByTestId("db-schema-open"));
    expect(await screen.findByTestId("db-schema-not-oracle")).toHaveTextContent(
      /Oracle delivery server/i,
    );
    expect(screen.queryByTestId("db-schema-save")).toBeNull();
    expect(screen.queryByTestId("db-schema-current")).toBeNull();
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
    fireEvent.click(screen.getByTestId("db-schema-open"));
    expect(await screen.findByTestId("db-schema-not-oracle")).toHaveTextContent(
      /Oracle delivery server/i,
    );
    expect(screen.queryByTestId("db-schema-save")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("rxdb");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the schema and keeps host, port, user, SID, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(oracleServer())
      .mockResolvedValueOnce(oracleServer("APP"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-schema-open"));
    expect(await screen.findByTestId("db-schema-current")).toHaveTextContent(
      "SYSTEM",
    );
    expect(screen.getByTestId("db-schema-host")).toHaveTextContent(
      "old-ora.example",
    );
    expect(screen.getByTestId("db-schema-port")).toHaveTextContent("1521");
    expect(screen.getByTestId("db-schema-user")).toHaveTextContent("cmsora");
    expect(screen.getByTestId("db-schema-sid")).toHaveTextContent("ORCL");
    expect(screen.getByTestId("db-schema-name")).toHaveTextContent("ora-prod");
    expect(screen.getByTestId("db-schema-driver")).toHaveTextContent("Oracle");
    expect(screen.getByTestId("db-schema-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("db-schema-input"), {
      target: { value: "APP" },
    });
    fireEvent.click(screen.getByTestId("db-schema-save"));
    await waitFor(() =>
      expect(serversApi.updateServer).toHaveBeenCalledTimes(1),
    );
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "schema", value: "APP" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("db-schema-current")).toHaveTextContent("APP"),
    );
    expect(screen.getByTestId("db-schema-host")).toHaveTextContent(
      "old-ora.example",
    );
    expect(screen.getByTestId("db-schema-port")).toHaveTextContent("1521");
    expect(screen.getByTestId("db-schema-user")).toHaveTextContent("cmsora");
    expect(screen.getByTestId("db-schema-sid")).toHaveTextContent("ORCL");
    expect(screen.getByTestId("db-schema-name")).toHaveTextContent("ora-prod");
    expect(screen.getByTestId("db-schema-driver")).toHaveTextContent("Oracle");
    expect(screen.getByTestId("db-schema-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank schema or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(oracleServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-schema-open"));
    await screen.findByTestId("db-schema-input");

    fireEvent.change(screen.getByTestId("db-schema-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-schema-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /Oracle schema is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-schema-current")).toHaveTextContent("SYSTEM");

    fireEvent.change(screen.getByTestId("db-schema-input"), {
      target: { value: "APP" },
    });
    fireEvent.click(screen.getByTestId("db-schema-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-schema-current")).toHaveTextContent("SYSTEM");
    expect(screen.queryByTestId("db-schema-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous schema on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(oracleServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-schema-open"));
    await screen.findByTestId("db-schema-input");

    const cases = [
      [400, "Oracle schema is required.", /Oracle schema is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-schema-input"), {
        target: { value: `SCH${status}` },
      });
      fireEvent.click(screen.getByTestId("db-schema-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-schema-current")).toHaveTextContent(
        "SYSTEM",
      );
      expect(screen.getByTestId("db-schema-host")).toHaveTextContent(
        "old-ora.example",
      );
      expect(screen.getByTestId("db-schema-user")).toHaveTextContent("cmsora");
      expect(screen.getByTestId("db-schema-sid")).toHaveTextContent("ORCL");
      expect(screen.getByTestId("db-schema-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

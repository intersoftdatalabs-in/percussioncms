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
import { DatabaseDeliverySidPanel } from "@/publishing/components/DatabaseDeliverySidPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-ora-password";

function oracleServer(sid = "ORCL") {
  return {
    serverId: "9",
    serverName: "ora-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "Oracle" },
      { key: "server", value: "old-ora.example" },
      { key: "port", value: "1521" },
      { key: "userid", value: "cmsora" },
      { key: "sid", value: sid },
      { key: "schema", value: "SYSTEM" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliverySidPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("DatabaseDeliverySidPanel", () => {
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
    fireEvent.click(screen.getByTestId("db-sid-open"));
    expect(await screen.findByTestId("db-sid-not-oracle")).toHaveTextContent(
      /Oracle delivery server/i,
    );
    expect(screen.queryByTestId("db-sid-save")).toBeNull();
    expect(screen.queryByTestId("db-sid-current")).toBeNull();
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
    fireEvent.click(screen.getByTestId("db-sid-open"));
    expect(await screen.findByTestId("db-sid-not-oracle")).toHaveTextContent(
      /Oracle delivery server/i,
    );
    expect(screen.queryByTestId("db-sid-save")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("rxdb");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the SID and keeps host, port, user, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(oracleServer())
      .mockResolvedValueOnce(oracleServer("ORCL2"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-sid-open"));
    expect(await screen.findByTestId("db-sid-current")).toHaveTextContent("ORCL");
    expect(screen.getByTestId("db-sid-host")).toHaveTextContent("old-ora.example");
    expect(screen.getByTestId("db-sid-port")).toHaveTextContent("1521");
    expect(screen.getByTestId("db-sid-user")).toHaveTextContent("cmsora");
    expect(screen.getByTestId("db-sid-name")).toHaveTextContent("ora-prod");
    expect(screen.getByTestId("db-sid-driver")).toHaveTextContent("Oracle");
    expect(screen.getByTestId("db-sid-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("db-sid-input"), {
      target: { value: "ORCL2" },
    });
    fireEvent.click(screen.getByTestId("db-sid-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { properties: [{ key: "sid", value: "ORCL2" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("db-sid-current")).toHaveTextContent("ORCL2"),
    );
    expect(screen.getByTestId("db-sid-host")).toHaveTextContent("old-ora.example");
    expect(screen.getByTestId("db-sid-port")).toHaveTextContent("1521");
    expect(screen.getByTestId("db-sid-user")).toHaveTextContent("cmsora");
    expect(screen.getByTestId("db-sid-name")).toHaveTextContent("ora-prod");
    expect(screen.getByTestId("db-sid-driver")).toHaveTextContent("Oracle");
    expect(screen.getByTestId("db-sid-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank SID or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(oracleServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-sid-open"));
    await screen.findByTestId("db-sid-input");

    fireEvent.change(screen.getByTestId("db-sid-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-sid-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/Oracle SID is required/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-sid-current")).toHaveTextContent("ORCL");

    fireEvent.change(screen.getByTestId("db-sid-input"), {
      target: { value: "ORCL2" },
    });
    fireEvent.click(screen.getByTestId("db-sid-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("db-sid-current")).toHaveTextContent("ORCL");
    expect(screen.queryByTestId("db-sid-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous SID on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(oracleServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-sid-open"));
    await screen.findByTestId("db-sid-input");

    const cases = [
      [400, "Oracle SID is required.", /Oracle SID is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-sid-input"), {
        target: { value: `SID${status}` },
      });
      fireEvent.click(screen.getByTestId("db-sid-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-sid-current")).toHaveTextContent("ORCL");
      expect(screen.getByTestId("db-sid-host")).toHaveTextContent("old-ora.example");
      expect(screen.getByTestId("db-sid-user")).toHaveTextContent("cmsora");
      expect(screen.getByTestId("db-sid-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});

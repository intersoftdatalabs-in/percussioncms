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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DatabaseDeliveryConnectionPanel } from "@/publishing/components/DatabaseDeliveryConnectionPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const STORED_SECRET = "s3cret-db-password";
const MASK = "passwordEntry";

function mysqlServer(host = "old.example", userId = "cmsdb") {
  return {
    serverId: "9",
    serverName: "mysql-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: host },
      { key: "port", value: "3306" },
      { key: "userid", value: userId },
      { key: "database", value: "rxdb" },
      { key: "password", value: MASK },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DatabaseDeliveryConnectionPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

function bodyOf(call: unknown): {
  serverInfo: { properties: { key: string; value: string }[] };
} {
  const args = call as unknown[];
  return args[2] as {
    serverInfo: { properties: { key: string; value: string }[] };
  };
}

describe("DatabaseDeliveryConnectionPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write on a Local server and does not show the secret", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "local-files",
      properties: [
        { key: "driver", value: "Local" },
        { key: "folder", value: "/kept/local" },
        { key: "password", value: STORED_SECRET },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("db-conn-open"));
    expect(await screen.findByTestId("db-conn-not-database")).toHaveTextContent(
      /only on a database delivery server/i,
    );
    expect(screen.queryByTestId("db-conn-save")).toBeNull();
    expect(document.body.textContent).not.toContain(STORED_SECRET);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("shows the masked status and opens host/user prefilled with an empty secret", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-conn-open"));
    expect(await screen.findByTestId("db-conn-host")).toHaveTextContent(
      "old.example",
    );
    expect(screen.getByTestId("db-conn-user")).toHaveTextContent("cmsdb");
    expect(screen.getByTestId("db-conn-port")).toHaveTextContent("3306");
    expect(screen.getByTestId("db-conn-database")).toHaveTextContent("rxdb");
    expect(screen.getByTestId("db-conn-name")).toHaveTextContent("mysql-prod");
    expect(screen.getByTestId("db-conn-driver")).toHaveTextContent("MYSQL");
    expect(screen.getByTestId("db-conn-password-status")).toHaveTextContent(
      /never shown/i,
    );
    expect(
      screen.getByTestId("db-conn-host-input") as HTMLInputElement,
    ).toHaveProperty("value", "old.example");
    expect(
      screen.getByTestId("db-conn-user-input") as HTMLInputElement,
    ).toHaveProperty("value", "cmsdb");
    expect(
      screen.getByTestId("db-conn-password-input") as HTMLInputElement,
    ).toHaveProperty("value", "");
    expect(document.body.textContent).not.toContain(STORED_SECRET);
    expect(document.body.textContent).not.toContain(MASK);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves host, user, and password in one body and keeps port, database, and driver", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(mysqlServer())
      .mockResolvedValueOnce(mysqlServer("db.new.example", "cmsnew"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("db-conn-open"));
    await screen.findByTestId("db-conn-host-input");

    fireEvent.change(screen.getByTestId("db-conn-host-input"), {
      target: { value: "db.new.example" },
    });
    fireEvent.change(screen.getByTestId("db-conn-user-input"), {
      target: { value: "cmsnew" },
    });
    fireEvent.change(screen.getByTestId("db-conn-password-input"), {
      target: { value: "N3wSecret!" },
    });
    fireEvent.click(screen.getByTestId("db-conn-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    const body = bodyOf(vi.mocked(serversApi.updateServer).mock.calls[0]);
    expect(body.serverInfo.properties).toHaveLength(3);
    const byKey = new Map(
      body.serverInfo.properties.map((p) => [p.key, p.value]),
    );
    expect(byKey.get("server")).toBe("db.new.example");
    expect(byKey.get("userid")).toBe("cmsnew");
    const sent = String(byKey.get("password") ?? "");
    expect(sent).not.toContain("N3wSecret!");
    expect(Buffer.from(sent, "base64").toString("utf8")).toBe("N3wSecret!");
    const json = JSON.stringify(body);
    expect(json).not.toContain("N3wSecret!");
    expect(json).not.toContain(STORED_SECRET);
    expect(json).not.toContain("3306");
    expect(json).not.toContain("rxdb");
    await waitFor(() =>
      expect(screen.getByTestId("db-conn-host")).toHaveTextContent(
        "db.new.example",
      ),
    );
    expect(screen.getByTestId("db-conn-user")).toHaveTextContent("cmsnew");
    expect(screen.getByTestId("db-conn-port")).toHaveTextContent("3306");
    expect(document.body.textContent).not.toContain("N3wSecret!");
    expect(document.body.textContent).not.toContain(STORED_SECRET);
  });

  it("does not write a blank host, user, password, or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("db-conn-open"));
    await screen.findByTestId("db-conn-host-input");

    fireEvent.change(screen.getByTestId("db-conn-host-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-conn-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /Database host is required/i,
    );
    fireEvent.change(screen.getByTestId("db-conn-host-input"), {
      target: { value: "db.new.example" },
    });
    fireEvent.change(screen.getByTestId("db-conn-user-input"), {
      target: { value: "   " },
    });
    fireEvent.change(screen.getByTestId("db-conn-password-input"), {
      target: { value: "N3wSecret!" },
    });
    fireEvent.click(screen.getByTestId("db-conn-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /Database user id is required/i,
    );
    fireEvent.change(screen.getByTestId("db-conn-user-input"), {
      target: { value: "cmsnew" },
    });
    fireEvent.change(screen.getByTestId("db-conn-password-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("db-conn-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /Database password is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    {
      fireEvent.change(screen.getByTestId("db-conn-password-input"), {
        target: { value: "typed-but-cancelled" },
      });
      fireEvent.click(screen.getByTestId("db-conn-cancel"));
      expect(confirm).toHaveBeenCalled();
      expect(screen.queryByTestId("db-conn-form")).toBeNull();
      expect(serversApi.updateServer).not.toHaveBeenCalled();
      expect(document.body.textContent).not.toContain("typed-but-cancelled");
    }
    vi.unstubAllGlobals();
  });

  it("keeps the form and the previous connection on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(mysqlServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("db-conn-open"));
    await screen.findByTestId("db-conn-host-input");

    const cases = [
      [400, "Database host is required.", /Database host is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("db-conn-host-input"), {
        target: { value: `db.new.example.${status}` },
      });
      fireEvent.change(screen.getByTestId("db-conn-user-input"), {
        target: { value: `cmsnew${status}` },
      });
      fireEvent.change(screen.getByTestId("db-conn-password-input"), {
        target: { value: `N3wSecret${status}!` },
      });
      fireEvent.click(screen.getByTestId("db-conn-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("db-conn-host")).toHaveTextContent("old.example");
      expect(screen.getByTestId("db-conn-user")).toHaveTextContent("cmsdb");
      expect(screen.getByTestId("db-conn-form")).toBeTruthy();
      expect(document.body.textContent).not.toContain(`N3wSecret${status}!`);
    }
    expect(document.body.textContent).not.toContain(STORED_SECRET);
  });
});

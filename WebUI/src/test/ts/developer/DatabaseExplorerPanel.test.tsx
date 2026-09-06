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
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as api from "../../../main/ts/api/developer/databaseExplorerApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { DatabaseExplorerPanel } from "../../../main/ts/developer/DatabaseExplorerPanel";

vi.mock("../../../main/ts/api/developer/databaseExplorerApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/databaseExplorerApi")>();
  return {
    ...actual,
    listDatabaseExplorerDatasources: vi.fn(),
    listDatabaseExplorerTables: vi.fn(),
  };
});

const listDatabaseExplorerDatasources = api.listDatabaseExplorerDatasources as ReturnType<
  typeof vi.fn
>;
const listDatabaseExplorerTables = api.listDatabaseExplorerTables as ReturnType<typeof vi.fn>;

describe("DatabaseExplorerPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listDatabaseExplorerDatasources.mockReset();
    listDatabaseExplorerTables.mockReset();
  });

  it("lists datasources and drills into tables then back", async () => {
    listDatabaseExplorerDatasources.mockResolvedValue([
      { id: "cms", displayName: "cms", repository: true, available: true },
    ]);
    listDatabaseExplorerTables.mockResolvedValue([
      { name: "CONTENTSTATUS", type: "TABLE", schema: "PUBLIC" },
      { name: "CMS_V", type: "VIEW", schema: "PUBLIC" },
    ]);
    render(<DatabaseExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-datasources-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-dbx-open-datasource"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-tables-table")).toBeTruthy();
    });
    expect(listDatabaseExplorerTables).toHaveBeenCalledWith("cms");
    expect(screen.getAllByTestId("developer-dbx-table-name")[0].textContent).toBe(
      "CONTENTSTATUS",
    );
    fireEvent.click(screen.getByTestId("developer-dbx-back-datasources"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-datasources-table")).toBeTruthy();
    });
  });

  it("shows empty state when no datasources are configured", async () => {
    listDatabaseExplorerDatasources.mockResolvedValue([]);
    render(<DatabaseExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-dbx-empty").textContent).toBe(DEV_MSG.DBX_EMPTY);
  });

  it("shows empty tables when a datasource has none", async () => {
    listDatabaseExplorerDatasources.mockResolvedValue([
      { id: "cms", displayName: "cms", available: true },
    ]);
    listDatabaseExplorerTables.mockResolvedValue([]);
    render(<DatabaseExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-open-datasource")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-dbx-open-datasource"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-tables-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listDatabaseExplorerDatasources.mockRejectedValue(new SessionRedirectError());
    render(<DatabaseExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-dbx-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
  });

  it("shows tables error without leaving browse chrome", async () => {
    listDatabaseExplorerDatasources.mockResolvedValue([
      { id: "cms", displayName: "cms", available: false },
    ]);
    listDatabaseExplorerTables.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Datasource is not allow-listed" },
    });
    render(<DatabaseExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-open-datasource")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-dbx-open-datasource"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-dbx-tables-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-dbx-browse")).toBeTruthy();
    expect(screen.getByTestId("developer-dbx-tables-error").textContent).toContain(
      DEV_MSG.DBX_TABLES_ERROR,
    );
  });
});

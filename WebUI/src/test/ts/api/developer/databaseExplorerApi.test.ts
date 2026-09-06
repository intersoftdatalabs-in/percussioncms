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

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  databaseExplorerTablesUrl,
  isSafeDatabaseExplorerId,
  isSafeDatabaseExplorerIdent,
  listDatabaseExplorerDatasources,
  listDatabaseExplorerTables,
  unwrapDatabaseExplorerDatasources,
  unwrapDatabaseExplorerTables,
} from "../../../../main/ts/api/developer/databaseExplorerApi";
import { PATHS } from "../../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("Database Explorer path-safety helpers", () => {
  it("accepts REST catalog ids", () => {
    expect(isSafeDatabaseExplorerId("cms")).toBe(true);
    expect(isSafeDatabaseExplorerId("rx_repo")).toBe(true);
    expect(isSafeDatabaseExplorerId("A1-b_2")).toBe(true);
  });

  it("rejects unsafe catalog ids", () => {
    expect(isSafeDatabaseExplorerId("")).toBe(false);
    expect(isSafeDatabaseExplorerId("1cms")).toBe(false);
    expect(isSafeDatabaseExplorerId("../etc")).toBe(false);
    expect(isSafeDatabaseExplorerId("C:Windows")).toBe(false);
    expect(isSafeDatabaseExplorerId("jdbc/RhythmyxData")).toBe(false);
  });

  it("accepts path-safe SQL idents", () => {
    expect(isSafeDatabaseExplorerIdent("CONTENTSTATUS")).toBe(true);
    expect(isSafeDatabaseExplorerIdent("CMS_V")).toBe(true);
  });

  it("rejects unsafe SQL idents", () => {
    expect(isSafeDatabaseExplorerIdent("")).toBe(false);
    expect(isSafeDatabaseExplorerIdent("../x")).toBe(false);
    expect(isSafeDatabaseExplorerIdent("a b")).toBe(false);
    expect(isSafeDatabaseExplorerIdent("foo;drop")).toBe(false);
  });
});

describe("unwrap Database Explorer payloads", () => {
  it("unwraps a bare datasource array and Jackson wrap", () => {
    expect(
      unwrapDatabaseExplorerDatasources([
        { id: "cms", displayName: "cms", repository: true },
      ]),
    ).toEqual([{ id: "cms", displayName: "cms", repository: true, available: undefined }]);
    expect(
      unwrapDatabaseExplorerDatasources({
        DatabaseExplorerDatasource: { id: "cms", displayName: "CMS" },
      }),
    ).toEqual([
      { id: "cms", displayName: "CMS", repository: undefined, available: undefined },
    ]);
    expect(unwrapDatabaseExplorerDatasources("not-json")).toEqual([]);
    expect(unwrapDatabaseExplorerTables(42)).toEqual([]);
  });

  it("skips unsafe datasource ids", () => {
    expect(
      unwrapDatabaseExplorerDatasources([{ id: "../etc", displayName: "nope" }, { id: "ok" }]),
    ).toEqual([
      { id: "ok", displayName: "ok", repository: undefined, available: undefined },
    ]);
  });

  it("unwraps tables and skips unsafe names", () => {
    const tables = unwrapDatabaseExplorerTables({
      DatabaseExplorerTable: [
        { name: "CONTENTSTATUS", type: "TABLE", schema: "PUBLIC" },
        { name: "foo;drop", type: "TABLE" },
        { name: "CMS_V", type: "view" },
      ],
    });
    expect(tables).toEqual([
      { name: "CONTENTSTATUS", type: "TABLE", schema: "PUBLIC" },
      { name: "CMS_V", type: "VIEW", schema: undefined },
    ]);
  });
});

describe("databaseExplorerTablesUrl", () => {
  it("encodes the catalog id", () => {
    expect(databaseExplorerTablesUrl("cms")).toBe(`${PATHS.DATABASE_EXPLORER}/cms/tables`);
  });
});

describe("listDatabaseExplorer REST calls", () => {
  it("listDatabaseExplorerDatasources GETs PATHS.DATABASE_EXPLORER", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue([{ id: "cms" }]);
    const list = await listDatabaseExplorerDatasources();
    expect(spy).toHaveBeenCalledWith(PATHS.DATABASE_EXPLORER);
    expect(list[0].id).toBe("cms");
  });

  it("listDatabaseExplorerTables rejects unsafe ids before GET", async () => {
    const spy = vi.spyOn(client, "get");
    await expect(listDatabaseExplorerTables("../etc")).rejects.toThrow(
      /Invalid Database Explorer datasource/,
    );
    expect(spy).not.toHaveBeenCalled();
  });

  it("listDatabaseExplorerTables GETs encoded tables URL", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue([]);
    await listDatabaseExplorerTables("cms");
    expect(spy).toHaveBeenCalledWith(`${PATHS.DATABASE_EXPLORER}/cms/tables`);
  });
});

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

import { describe, expect, it } from "vitest";
import {
  listColumnsHttpStatus,
  unwrapExplorerListColumns,
} from "../../../main/ts/api/contentExplorer/listColumnsApi";

describe("explorer list columns (#4722)", () => {
  it("unwraps the Jackson root envelope", () => {
    const out = unwrapExplorerListColumns({
      ExplorerListColumns: {
        folderPath: "//Sites/Demo",
        columns: ["sys_title", "sys_workflow"],
      },
    });
    expect(out.folderPath).toBe("//Sites/Demo");
    expect(out.columns).toEqual(["sys_title", "sys_workflow"]);
  });

  it("reads a bare body and drops blank sources", () => {
    const out = unwrapExplorerListColumns({
      folderPath: "/",
      columns: ["sys_title", "", 3],
    });
    expect(out.columns).toEqual(["sys_title"]);
  });

  it("exposes HTTP status from an API error object", () => {
    expect(listColumnsHttpStatus({ status: 403, statusText: "Forbidden", body: "" })).toBe(
      403,
    );
    expect(listColumnsHttpStatus(new Error("no"))).toBeNull();
  });
});

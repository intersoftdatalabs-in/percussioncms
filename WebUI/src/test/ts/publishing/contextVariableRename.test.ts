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
import type { SitePropertyDto } from "@/api/publishing/designApi";
import {
  CONTEXT_VARIABLE_EXISTS,
  CONTEXT_VARIABLE_NAME_MAX_LENGTH,
} from "@/publishing/contextVariable";
import {
  CONTEXT_VARIABLE_NOT_LISTED,
  buildContextVariableRenameBody,
  contextVariablesAfterSuccessfulRename,
  mapContextVariableRenameError,
  validateContextVariableRename,
} from "@/publishing/contextVariableRename";

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };
const target: SitePropertyDto = { name: "nightVar", contextId: "3", value: "before" };
const rows = [kept, target];

describe("validateContextVariableRename", () => {
  it("rejects a blank new name", () => {
    expect(validateContextVariableRename("nightVar", "   ", rows)).toEqual({
      ok: false,
      error: "Context variable name is required",
    });
  });

  it("rejects an overlong new name", () => {
    expect(
      validateContextVariableRename(
        "nightVar",
        "n".repeat(CONTEXT_VARIABLE_NAME_MAX_LENGTH + 1),
        rows,
      ),
    ).toEqual({
      ok: false,
      error: "Context variable name must be 50 characters or fewer",
    });
  });

  it("rejects a duplicate of another variable", () => {
    expect(validateContextVariableRename("nightVar", "kept", rows)).toEqual({
      ok: false,
      error: CONTEXT_VARIABLE_EXISTS,
    });
  });

  it("rejects a name that is not listed", () => {
    expect(validateContextVariableRename("missing", "nextName", rows)).toEqual({
      ok: false,
      error: CONTEXT_VARIABLE_NOT_LISTED,
    });
  });

  it("trims and keeps the stored value; the same name is not a duplicate", () => {
    expect(validateContextVariableRename("  nightVar  ", "  nextName  ", rows)).toEqual({
      ok: true,
      name: "nightVar",
      newName: "nextName",
      value: "before",
    });
    expect(validateContextVariableRename("nightVar", "nightVar", rows)).toEqual({
      ok: true,
      name: "nightVar",
      newName: "nightVar",
      value: "before",
    });
  });
});

describe("buildContextVariableRenameBody", () => {
  it("sends the stored name and new name without a value", () => {
    expect(buildContextVariableRenameBody("nightVar", "3", "nextName")).toEqual({
      name: "nightVar",
      contextId: "3",
      newName: "nextName",
      renameName: true,
    });
  });
});

describe("contextVariablesAfterSuccessfulRename", () => {
  it("shows the new name from a refresh and leaves the other variable", () => {
    const renamed = { name: "nightVar", newName: "nextName", value: "before", contextId: "3" };
    const refreshed = [kept, { name: "nextName", contextId: "3", value: "before" }];
    expect(contextVariablesAfterSuccessfulRename(refreshed, renamed, rows)).toEqual(refreshed);
    expect(kept.value).toBe("old");
  });

  it("renames only that row when refresh fails", () => {
    const renamed = { name: "nightVar", newName: "nextName", value: "before", contextId: "3" };
    expect(contextVariablesAfterSuccessfulRename(null, renamed, rows)).toEqual([
      kept,
      { name: "nextName", contextId: "3", value: "before" },
    ]);
  });

  it("replaces a stale refreshed name and keeps the value", () => {
    const renamed = { name: "nightVar", newName: "nextName", value: "before", contextId: "3" };
    expect(contextVariablesAfterSuccessfulRename(rows, renamed, rows)).toEqual([
      kept,
      { name: "nextName", contextId: "3", value: "before" },
    ]);
  });
});

describe("mapContextVariableRenameError", () => {
  it("uses the body message for 400, 403, and 409", () => {
    expect(
      mapContextVariableRenameError({
        status: 400,
        body: { message: "Context variable name is required" },
      }),
    ).toBe("Context variable name is required");
    expect(
      mapContextVariableRenameError({
        status: 403,
        body: { message: "Admin or Designer role required" },
      }),
    ).toBe("Admin or Designer role required");
    expect(
      mapContextVariableRenameError({
        status: 409,
        body: { message: CONTEXT_VARIABLE_EXISTS },
      }),
    ).toBe(CONTEXT_VARIABLE_EXISTS);
    expect(
      mapContextVariableRenameError({
        status: 409,
        body: { message: CONTEXT_VARIABLE_NOT_LISTED },
      }),
    ).toBe(CONTEXT_VARIABLE_NOT_LISTED);
  });

  it("falls back when a 409 body has no message", () => {
    expect(mapContextVariableRenameError({ status: 409, body: {} })).toContain("already exists");
  });
});

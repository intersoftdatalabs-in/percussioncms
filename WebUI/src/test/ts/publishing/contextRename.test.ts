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
import type { ContextSummary } from "@/api/publishing/designApi";
import {
  CONTEXT_NAME_MAX_LENGTH,
  buildContextRenameBody,
  contextsAfterSuccessfulRename,
  validateContextRenameName,
} from "@/publishing/contextRename";

const source: ContextSummary = {
  contextId: "3",
  name: "Publish",
  description: "Public site",
  defaultSchemeId: "11",
};

describe("validateContextRenameName", () => {
  it("rejects a blank name", () => {
    expect(validateContextRenameName("   ")).toEqual({
      ok: false,
      error: "Name is required",
    });
  });

  it("rejects a name longer than the column", () => {
    expect(validateContextRenameName("n".repeat(CONTEXT_NAME_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: "Publishing context name must be 50 characters or fewer",
    });
  });

  it("trims a usable name", () => {
    expect(validateContextRenameName("  Night  ")).toEqual({
      ok: true,
      name: "Night",
    });
  });
});

describe("buildContextRenameBody", () => {
  it("sends the name only", () => {
    expect(buildContextRenameBody("Renamed")).toEqual({ name: "Renamed" });
    expect(buildContextRenameBody("Renamed")).not.toHaveProperty("description");
    expect(buildContextRenameBody("Renamed")).not.toHaveProperty("defaultSchemeId");
    expect(buildContextRenameBody("Renamed")).not.toHaveProperty("contextId");
  });
});

describe("contextsAfterSuccessfulRename", () => {
  const other: ContextSummary = {
    contextId: "4",
    name: "Preview",
    description: "preview",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, name: "Renamed" }, other];
    expect(contextsAfterSuccessfulRename(refreshed, "3", "Renamed", [source, other])).toBe(
      refreshed,
    );
  });

  it("patches only the name when the reload failed", () => {
    expect(contextsAfterSuccessfulRename(null, "3", "Renamed", [source, other])).toEqual([
      { ...source, name: "Renamed" },
      other,
    ]);
  });
});

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
  CONTEXT_NAME_REQUIRED,
  CONTEXT_NAME_TOO_LONG,
  buildContextCopyBody,
  contextsAfterSuccessfulCopy,
  suggestedContextCopyName,
  validateContextCopyName,
} from "@/publishing/contextCopy";

const source: ContextSummary = {
  contextId: "3",
  name: "Publish",
  description: "Public site",
  defaultSchemeId: "11",
};

describe("validateContextCopyName", () => {
  it("rejects a blank name", () => {
    expect(validateContextCopyName("   ")).toEqual({
      ok: false,
      error: CONTEXT_NAME_REQUIRED,
    });
  });

  it("rejects a name longer than the CONTEXTNAME column", () => {
    const raw = "n".repeat(CONTEXT_NAME_MAX_LENGTH + 1);
    expect(validateContextCopyName(raw)).toEqual({
      ok: false,
      error: CONTEXT_NAME_TOO_LONG,
    });
  });

  it("accepts a trimmed name at the column limit", () => {
    const raw = `  ${"n".repeat(CONTEXT_NAME_MAX_LENGTH)}  `;
    expect(validateContextCopyName(raw)).toEqual({
      ok: true,
      name: "n".repeat(CONTEXT_NAME_MAX_LENGTH),
    });
  });
});

describe("suggestedContextCopyName", () => {
  it("suggests the source name plus copy", () => {
    expect(suggestedContextCopyName("Publish")).toBe("Publish copy");
  });

  it("does not suggest a name that would be overlong", () => {
    expect(suggestedContextCopyName("n".repeat(CONTEXT_NAME_MAX_LENGTH))).toBe("");
  });
});

describe("buildContextCopyBody", () => {
  it("keeps the description and drops the source id and default scheme", () => {
    const body = buildContextCopyBody(source, "Publish copy");
    expect(body).toEqual({
      name: "Publish copy",
      description: "Public site",
    });
    expect(body).not.toHaveProperty("contextId");
    expect(body).not.toHaveProperty("defaultSchemeId");
  });

  it("omits a missing description", () => {
    const body = buildContextCopyBody({ contextId: "4", name: "Preview" }, "Preview copy");
    expect(body).toEqual({ name: "Preview copy" });
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("contextId");
    expect(body).not.toHaveProperty("defaultSchemeId");
  });

  it("keeps an empty description", () => {
    expect(buildContextCopyBody({ contextId: "4", name: "Preview", description: "" }, "Preview copy")).toEqual({
      name: "Preview copy",
      description: "",
    });
  });
});

describe("contextsAfterSuccessfulCopy", () => {
  const created: ContextSummary = {
    contextId: "9",
    name: "Publish copy",
    description: "Public site",
  };

  it("uses the refreshed list when the new row is already there", () => {
    const refreshed = [source, created];
    expect(contextsAfterSuccessfulCopy(refreshed, created, [source])).toEqual(refreshed);
  });

  it("appends the created row and keeps the source when refresh fails", () => {
    expect(contextsAfterSuccessfulCopy(null, created, [source])).toEqual([source, created]);
  });

  it("restores a source row a partial refresh dropped", () => {
    expect(contextsAfterSuccessfulCopy([created], created, [source])).toEqual([
      created,
      source,
    ]);
  });
});

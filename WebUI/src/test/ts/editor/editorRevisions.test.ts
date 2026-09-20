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

import { describe, expect, it } from "vitest";
import {
  canRestoreFromEditor,
  editorRevisionErrorReason,
  parseRevisionId,
  restoreRevisionConfirmBody,
  summarizeRevisionRow,
} from "../../../main/ts/editor/editorRevisions";

describe("editorRevisions", () => {
  it("allows restore actions only in edit mode", () => {
    expect(canRestoreFromEditor("edit")).toBe(true);
    expect(canRestoreFromEditor("view")).toBe(false);
    expect(canRestoreFromEditor("promote")).toBe(false);
  });

  it("summarizes a revision row with rev id, status, modifier, and date", () => {
    expect(
      summarizeRevisionRow({
        revId: 5,
        status: "Live",
        lastModifier: "admin",
        lastModifiedDate: "2026-04-12",
      }),
    ).toBe("#5 — Live — admin — 2026-04-12");
  });

  it("falls back to a partial label when only some fields are present", () => {
    expect(
      summarizeRevisionRow({
        revId: 5,
        status: "",
        lastModifier: "admin",
        lastModifiedDate: "",
      }),
    ).toBe("#5 — admin");
    expect(
      summarizeRevisionRow(
        { revId: 0, status: "", lastModifier: "", lastModifiedDate: "" },
        "#5",
      ),
    ).toBe("#5");
    expect(
      summarizeRevisionRow({
        revId: 5,
        status: "Live",
        lastModifier: "",
        lastModifiedDate: "",
      }),
    ).toBe("#5 — Live");
  });

  it("builds a confirm body with the item name when present", () => {
    expect(
      restoreRevisionConfirmBody("#3 — Live — admin", "Home"),
    ).toBe("#3 — Live — admin (Home)");
    expect(restoreRevisionConfirmBody("#3", "")).toBe("#3");
    expect(restoreRevisionConfirmBody("#3", "  ")).toBe("#3");
  });

  it("parses revision ids from numbers and strings", () => {
    expect(parseRevisionId(7)).toBe(7);
    expect(parseRevisionId("7")).toBe(7);
    expect(parseRevisionId("")).toBeNull();
    expect(parseRevisionId("abc")).toBeNull();
    expect(parseRevisionId(0)).toBeNull();
    expect(parseRevisionId(-3)).toBeNull();
    expect(parseRevisionId(null)).toBeNull();
    expect(parseRevisionId(undefined)).toBeNull();
  });

  it("maps 403 and 404 from restoreRevision without treating them as success", () => {
    expect(
      editorRevisionErrorReason({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    ).toBe("forbidden");
    expect(
      editorRevisionErrorReason({
        status: 404,
        statusText: "Not Found",
        body: { message: "no such revision" },
      }),
    ).toBe("not_found");
    expect(
      editorRevisionErrorReason({
        status: 500,
        statusText: "Server Error",
        body: {},
      }),
    ).toBe("failed");
    expect(editorRevisionErrorReason(new Error("network down"))).toBe("failed");
  });
});

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
import { collectInvalidLinkFieldErrors, linkFieldProblem } from "../../../main/ts/editor/linkField";

describe("linkFieldProblem", () => {
  it("allows a clear, a content id, a GUID, and a site path", () => {
    expect(linkFieldProblem("")).toBeNull();
    expect(linkFieldProblem("  ")).toBeNull();
    expect(linkFieldProblem("594")).toBeNull();
    expect(linkFieldProblem("0-101-594")).toBeNull();
    expect(linkFieldProblem("//Sites/Example/index")).toBeNull();
    expect(linkFieldProblem("/Sites/Example/index")).toBeNull();
  });

  it("rejects schemes, traversal, and free text", () => {
    expect(linkFieldProblem("javascript:alert(1)")).toBe("invalid");
    expect(linkFieldProblem("https://example.test/a")).toBe("invalid");
    expect(linkFieldProblem("//Sites/../secret")).toBe("invalid");
    expect(linkFieldProblem("not a path")).toBe("invalid");
  });
});

describe("collectInvalidLinkFieldErrors", () => {
  it("flags only link rows", () => {
    const errors = collectInvalidLinkFieldErrors(
      [
        { name: "page", kind: "link", value: "nope" },
        { name: "title", kind: "text", value: "nope" },
      ],
      "bad",
    );
    expect(errors).toEqual({ page: "bad" });
  });
});

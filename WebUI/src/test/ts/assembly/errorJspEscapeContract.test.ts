/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

/**
 * Contract: ui/assembly/error.jsp must compile on commons-lang3 + commons-text.
 * lang3 StringEscapeUtils.escapeHtml was removed; use commons-text escapeHtml4
 * (#3719).
 */
describe("ui/assembly/error.jsp escape contract (#3719)", () => {
  const jspPath = resolve(
    __dirname,
    "../../../main/webapp/ui/assembly/error.jsp",
  );
  const text = readFileSync(jspPath, "utf8");

  it("uses commons-text escapeHtml4, not lang3 escapeHtml", () => {
    expect(text).toContain("org.apache.commons.text.StringEscapeUtils");
    expect(text).toContain("escapeHtml4");
    expect(text).not.toContain("org.apache.commons.lang3.StringEscapeUtils");
    expect(text).not.toMatch(/StringEscapeUtils\.escapeHtml\s*\(/);
  });
});

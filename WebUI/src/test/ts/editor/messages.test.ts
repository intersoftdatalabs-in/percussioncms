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
import { EDITOR_MSG } from "../../../main/ts/editor/messages";

/**
 * FR-014 convention guard: every editor string resolves through the
 * {@code perc.ui.editor@} TMX bundle with a non-blank default, and no
 * invisible control characters (cf. #6172 Telugu {@code U+200C} follow-up).
 */
describe("EDITOR_MSG", () => {
  const entries = Object.entries(EDITOR_MSG);

  it("defines at least one message", () => {
    expect(entries.length).toBeGreaterThan(0);
  });

  it("prefixes every value with the editor TMX bundle", () => {
    for (const [key, value] of entries) {
      expect(value.startsWith("perc.ui.editor@"), key).toBe(true);
    }
  });

  it("gives every key a non-blank default text", () => {
    for (const [key, value] of entries) {
      const text = value.slice("perc.ui.editor@".length);
      expect(text.trim().length, key).toBeGreaterThan(0);
    }
  });

  it("contains no invisible control or zero-width characters", () => {
    // U+200B/U+200C/U+200D zero-width, U+FEFF BOM, U+00AD soft hyphen,
    // plus other Cc controls.
    const invisible = /[\u200B\u200C\u200D\uFEFF\u00AD\u0000-\u001F\u007F]/;
    for (const [key, value] of entries) {
      expect(invisible.test(value), key).toBe(false);
    }
  });

  it("uses UPPER_SNAKE keys", () => {
    for (const key of Object.keys(EDITOR_MSG)) {
      expect(key, key).toMatch(/^[A-Z][A-Z0-9_]*$/);
    }
  });
});

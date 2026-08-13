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

/**
 * Static-resource contract for editor toolbar icons (#3332 / parent #3329).
 *
 * The files live under {@code /cm/images/icons/editor}. Toolbar JSPs must
 * request that canonical tree — not {@code /cm/pages/app/images/...} which
 * 404s under the SPA mount.
 */

import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const webappRoot = resolve(__dirname, "../../../main/webapp");

const TOOLBAR_JSPS = [
  "cm/app/includes/content_toolbar.jsp",
  "cm/pages/app/includes/content_toolbar.jsp",
] as const;

const EDITOR_ICONS = [
  "cm/images/icons/editor/delete.png",
  "cm/images/icons/editor/edit.png",
] as const;

function read(rel: string): string {
  return readFileSync(resolve(webappRoot, rel), "utf8").replace(/\r\n/g, "\n");
}

describe("editor toolbar icon assets (#3332)", () => {
  it("ships delete.png and edit.png under /cm/images/icons/editor", () => {
    for (const rel of EDITOR_ICONS) {
      expect(existsSync(resolve(webappRoot, rel)), rel).toBe(true);
    }
  });

  it("toolbar JSPs request canonical /cm/images paths, not SPA-mount images", () => {
    for (const rel of TOOLBAR_JSPS) {
      const text = read(rel);
      expect(text, rel).toContain('src="/cm/images/icons/editor/delete.png"');
      expect(text, rel).toContain('src="/cm/images/icons/editor/edit.png"');
      expect(text, rel).not.toContain("/cm/pages/app/images/");
      expect(text, rel).not.toContain("/cm/app/images/icons/editor/");
      expect(text, rel).toContain("onerror=");
    }
  });
});

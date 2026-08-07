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

/**
 * Contract tests for Admin portrait / narrow layout (GH-945).
 * Asserts modern CSS module + classic JSP dual-ship no longer hard-clip chrome.
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const tsRoot = resolve(__dirname, "../../../main/ts");
const webappRoot = resolve(__dirname, "../../../main/webapp");
const warRoot = resolve(__dirname, "../../../../war");

describe("Admin portrait layout contract (GH-945)", () => {
  it("AdminChrome.module.css wraps tabs and avoids fixed min-width clip", () => {
    const path = resolve(tsRoot, "admin/AdminChrome.module.css");
    const text = readFileSync(path, "utf8");
    expect(text).toMatch(/flex-wrap:\s*wrap/);
    expect(text).toMatch(/overflow-x:\s*auto/);
    expect(text).toMatch(/min-width:\s*0/);
    expect(text).toMatch(/@media\s*\(max-width:\s*640px\)/);
    expect(text).toMatch(/orientation:\s*portrait/);
  });

  it("classic adminWorkflow.jsp drops fixed min-width:500px (dual-ship)", () => {
    const rels = [
      resolve(webappRoot, "cm/app/adminWorkflow.jsp"),
      resolve(webappRoot, "cm/pages/app/adminWorkflow.jsp"),
      resolve(warRoot, "app/adminWorkflow.jsp"),
    ];
    for (const path of rels) {
      const text = readFileSync(path, "utf8");
      expect(text, path).not.toMatch(/min-width\s*:\s*500px/);
      expect(text, path).toContain('class="perc-admin-tabs"');
      expect(text, path).toContain("perc-admin-workflow-page");
      expect(text, path).toMatch(/overflow-y:\s*auto/);
    }
  });

  it("percWorkflow.css ships portrait media-query rules (dual-ship)", () => {
    const rels = [
      resolve(webappRoot, "cm/css/percWorkflow.css"),
      resolve(webappRoot, "cm/app/css/legacy/percWorkflow.css"),
      resolve(warRoot, "css/percWorkflow.css"),
    ];
    for (const path of rels) {
      const text = readFileSync(path, "utf8");
      expect(text, path).toContain("GH-945");
      expect(text, path).toMatch(/flex-wrap:\s*wrap/);
      expect(text, path).toContain(".perc-admin-tabs");
      expect(text, path).toMatch(
        /#perc-users-details[\s\S]*min-width:\s*0/,
      );
    }
  });
});

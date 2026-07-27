/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const appIndex = resolve(__dirname, "../../../main/webapp/cm/app/index.jsp");
const pagesIndex = resolve(
  __dirname,
  "../../../main/webapp/cm/pages/app/index.jsp",
);

function read(path: string): string {
  return readFileSync(path, "utf8");
}

describe("PR-5 aggressive index.jsp SPA cutover", () => {
  it("maps modern views to spa.jsp?entry= (not *Modern.jsp product hosts)", () => {
    for (const indexPath of [appIndex, pagesIndex]) {
      const text = read(indexPath);
      expect(text).toContain("buildSpaEntryRedirect");
      expect(text).toContain("/cm/app/spa.jsp?");
      // Intent: SPA Location query starts with entry= (avoid coupling to JSP format)
      expect(text).toMatch(/"entry="\s*\)|entry=\s*"\s*\+/);
      expect(text).toMatch(/URLEncoder\.encode\(\s*entry\b/);
      // Must not forward product modern views to *Modern.jsp
      expect(text).not.toMatch(
        /legacyViews\.put\("home",\s*"homeModern\.jsp"\)/,
      );
      expect(text).not.toMatch(
        /legacyViews\.put\("publish",\s*"publishModern\.jsp"\)/,
      );
      expect(text).not.toMatch(
        /views\.put\("publish",\s*"publishModern\.jsp"\)/,
      );
      expect(text).not.toMatch(
        /views\.put\("home",\s*"homeModern\.jsp"\)/,
      );
      // Legacy exits preserved
      expect(text).toMatch(/legacyViews\.put\("dash",\s*"dashboard\.jsp"\)/);
      expect(text).toMatch(/legacyViews\.put\("editor",\s*"webmgt\.jsp"\)/);
      expect(text).toMatch(/legacyViews\.put\("design",\s*"admin\.jsp"\)/);
      expect(text).toMatch(
        /legacyViews\.put\("arch",\s*"siteArchitecture\.jsp"\)/,
      );
    }
  });

  it("uses proxyURL prefix on SPA redirects", () => {
    const text = read(appIndex);
    expect(text).toContain("proxyURL");
    expect(text).toMatch(
      /\(proxyURL == null \? "" : proxyURL\) \+ "\/cm\/app\/spa\.jsp\?"/,
    );
    // Never emit hash fragments on server redirects
    expect(text).not.toMatch(/spa\.jsp\?[^"']*#/);
    expect(text).not.toContain("spa.jsp#");
  });

  it("allowlists home/publish/workflow/admin deep-link tokens", () => {
    const text = read(appIndex);
    expect(text).toContain("HOME_SECTIONS");
    expect(text).toContain("PUBLISH_SECTIONS");
    expect(text).toContain("WORKFLOW_TABS");
    expect(text).toContain("ADMIN_TABS");
    expect(text).toContain('"widget-builder"');
  });

  it("retired_modern_redirect only re-forwards params buildSpaEntryRedirect consumes", () => {
    const redirect = read(
      resolve(
        __dirname,
        "../../../main/webapp/cm/app/includes/retired_modern_redirect.jsp",
      ),
    );
    expect(redirect).toContain('"initialScreen"');
    expect(redirect).toContain('"section"');
    expect(redirect).toContain('"tab"');
    expect(redirect).toContain('"siteId"');
    expect(redirect).toContain('"serverId"');
    // Intentionally not re-forwarded (Kilo #1531): not mapped for these SPA entries
    expect(redirect).not.toMatch(/"path"\s*,/);
    expect(redirect).not.toMatch(/"site"\s*\]/);
  });

  it("dual-tree index.jsp files stay aligned for SPA cutover", () => {
    expect(read(appIndex)).toBe(read(pagesIndex));
  });

  it("retired *Modern.jsp product hosts re-enter dispatcher", () => {
    const hosts = [
      "homeModern.jsp",
      "publishModern.jsp",
      "adminModern.jsp",
      "adminWorkflowModern.jsp",
      "widgetBuilderModern.jsp",
      "unavailableModern.jsp",
    ];
    for (const name of hosts) {
      const path = resolve(
        __dirname,
        `../../../main/webapp/cm/app/${name}`,
      );
      const text = read(path);
      expect(text).toContain("retired_modern_redirect.jsp");
      expect(text).toContain("retiredModernView");
      expect(text).not.toContain("PercModernUI.mount");
    }
  });
});

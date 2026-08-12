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

import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const webappRoot = resolve(__dirname, "../../../main/webapp");
const appIndex = resolve(webappRoot, "cm/app/index.jsp");
const pagesIndex = resolve(webappRoot, "cm/pages/app/index.jsp");

function read(path: string): string {
  // Normalize CRLF/LF so dual-tree equality is portable on Windows checkouts.
  return readFileSync(path, "utf8").replace(/\r\n/g, "\n");
}

/** Product host JSPs deleted in PR-8 (SPA owns these surfaces). */
const DELETED_PRODUCT_HOSTS = [
  "cm/app/homeModern.jsp",
  "cm/app/publishModern.jsp",
  "cm/app/adminModern.jsp",
  "cm/app/adminWorkflowModern.jsp",
  "cm/app/widgetBuilderModern.jsp",
  "cm/app/unavailableModern.jsp",
  "cm/app/explorerModern.jsp",
  "cm/pages/app/homeModern.jsp",
  "cm/pages/app/publishModern.jsp",
  "cm/pages/app/widgetBuilderModern.jsp",
  "cm/pages/app/unavailableModern.jsp",
  "cm/pages/app/explorerModern.jsp",
  "cm/app/includes/retired_modern_redirect.jsp",
  "cm/pages/app/includes/retired_modern_redirect.jsp",
  "cm/app/includes/modern_shell_head.jsp",
  "rxlogin-classic.jsp",
] as const;

/** Residual bridge dialog hosts (must remain). assetPicker is app-tree only. */
const RESIDUAL_BRIDGE_HOSTS = [
  "cm/app/assetPickerModern.jsp",
  "cm/app/pagePickerModern.jsp",
  "cm/app/folderPickerModern.jsp",
  "cm/app/folderSecurityModern.jsp",
  "cm/app/searchModern.jsp",
  "cm/app/actionMenuModern.jsp",
  "cm/app/us7AdvancedModern.jsp",
  "cm/pages/app/pagePickerModern.jsp",
  "cm/pages/app/folderPickerModern.jsp",
  "cm/pages/app/folderSecurityModern.jsp",
  "cm/pages/app/searchModern.jsp",
  "cm/pages/app/actionMenuModern.jsp",
  "cm/pages/app/us7AdvancedModern.jsp",
] as const;

describe("PR-5 aggressive index.jsp SPA cutover (retained)", () => {
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
      // Legacy exits preserved (dash moved to SPA Home gadgets in PR-7)
      expect(text).toMatch(/legacyViews\.put\("editor",\s*"webmgt\.jsp"\)/);
      expect(text).toMatch(/legacyViews\.put\("design",\s*"admin\.jsp"\)/);
      // #3094 / #3099: Architecture is SPA entry, not legacyViews arch → siteArchitecture.jsp
      expect(text).not.toMatch(
        /legacyViews\.put\("arch",\s*"siteArchitecture\.jsp"\)/,
      );
      expect(text).toMatch(/"arch"/);
      expect(text).toMatch(/"architecture"/);
      expect(text).toMatch(/entry\s*=\s*"architecture"|entry = "architecture"/);
    }
  });

  it("siteArchitecture.jsp hard-redirects to SPA Architecture (#3099)", () => {
    const hosts = [
      resolve(webappRoot, "cm/app/siteArchitecture.jsp"),
      resolve(webappRoot, "cm/pages/app/siteArchitecture.jsp"),
    ];
    for (const jsp of hosts) {
      expect(existsSync(jsp), jsp).toBe(true);
      const text = read(jsp);
      // Retired host: redirect stub only (no site map widget / packed assets)
      expect(text).toMatch(/setStatus\s*\(\s*301\s*\)/);
      expect(text).toContain('view=arch');
      expect(text).toContain("Location");
      expect(text).not.toContain("perc_architecture.packed");
      expect(text).not.toContain("perc_site_map");
      expect(text).not.toMatch(/perc_site_map\s*\(/);
    }
  });

  it("does not pack retired perc_architecture bundles (#3099)", () => {
    const bundles = resolve(
      __dirname,
      "../../../main/resources/minify/static-bundles.json",
    );
    const text = read(bundles);
    expect(text).not.toContain("perc_architecture.packed");
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
    expect(text).toContain('"developer"');
    expect(text).toContain("DEVELOPER_SECTIONS");
    // PR-7: dash maps to Home gadgets, not legacy dashboard.jsp
    expect(text).toContain('"gadgets"');
    expect(text).toMatch(/"dash"/);
    expect(text).not.toMatch(/legacyViews\.put\("dash",\s*"dashboard\.jsp"\)/);
  });

  it("dual-tree index.jsp files stay aligned for SPA cutover", () => {
    expect(read(appIndex)).toBe(read(pagesIndex));
  });
});

describe("PR-8 delete obsolete product host JSPs", () => {
  it("retired product *Modern.jsp shells and classic login are gone", () => {
    for (const rel of DELETED_PRODUCT_HOSTS) {
      expect(existsSync(resolve(webappRoot, rel)), rel).toBe(false);
    }
  });

  it("residual bridge dialog/legacy hosts remain", () => {
    for (const rel of RESIDUAL_BRIDGE_HOSTS) {
      expect(existsSync(resolve(webappRoot, rel)), rel).toBe(true);
    }
  });

  it("spa.jsp remains the authenticated SPA document (dual-tree aligned)", () => {
    const appSpa = resolve(webappRoot, "cm/app/spa.jsp");
    const pagesSpa = resolve(webappRoot, "cm/pages/app/spa.jsp");
    expect(existsSync(appSpa)).toBe(true);
    expect(existsSync(pagesSpa)).toBe(true);
    expect(read(appSpa)).toBe(read(pagesSpa));
    // SPA boot root (index.ts accepts perc-spa-root / root / perc-app-root)
    expect(read(appSpa)).toContain("perc-spa-root");
    // Home and all SPA chrome need TMX before the modern bundle
    expect(read(appSpa)).toContain("tmx/tmx.jsp");
  });


  it("rxlogin.jsp remains the React login host", () => {
    const login = resolve(webappRoot, "rxlogin.jsp");
    expect(existsSync(login)).toBe(true);
    const text = read(login);
    expect(text).toContain("perc-login-root");
    // Classic host file is gone; comment may still mention the name historically
    expect(existsSync(resolve(webappRoot, "rxlogin-classic.jsp"))).toBe(false);
  });

  it("security conf no longer grants anonymous to deleted classic login", () => {
    const conf = resolve(
      webappRoot,
      "WEB-INF/config/security/system-security-conf.xml",
    );
    const text = read(conf);
    expect(text).toContain("/rxlogin.jsp");
    expect(text).not.toContain("rxlogin-classic.jsp");
  });

  it("actionMenu residual host opens SPA explorer (not deleted explorerModern)", () => {
    const appMenu = resolve(webappRoot, "cm/app/actionMenuModern.jsp");
    const pagesMenu = resolve(webappRoot, "cm/pages/app/actionMenuModern.jsp");
    for (const path of [appMenu, pagesMenu]) {
      const text = read(path);
      expect(text).toContain("/cm/app/spa.jsp?entry=explorer");
      expect(text).not.toContain("explorerModern.jsp");
    }
    expect(read(appMenu)).toBe(read(pagesMenu));
  });
});

describe("PR-9 path-based SPA URLs + fallback filter", () => {
  it("web.xml registers PSWebUiSpaFallbackFilter for app and pages trees", () => {
    const webXml = resolve(webappRoot, "WEB-INF/web.xml");
    const text = read(webXml);
    expect(text).toContain("PSWebUiSpaFallbackFilter");
    expect(text).toContain("com.percussion.webui.filter.PSWebUiSpaFallbackFilter");
    expect(text).toContain("/cm/app/*");
    expect(text).toContain("/cm/pages/app/*");
  });

  it("filter source allowlists SPA entries and spa.jsp forward", () => {
    const filter = resolve(
      __dirname,
      "../../../main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java",
    );
    const text = read(filter);
    expect(text).toContain("spa.jsp?entry=");
    expect(text).toContain("widget-builder");
    expect(text).toContain("explorer");
    expect(text).toContain("profile");
    expect(text).not.toMatch(/sendRedirect/);
  });
});


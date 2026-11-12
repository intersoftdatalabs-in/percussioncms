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

import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, resolve } from "node:path";
import { describe, expect, it } from "vitest";

const webappRoot = resolve(__dirname, "../../../main/webapp");
const appIndex = resolve(webappRoot, "cm/app/index.jsp");
const pagesIndex = resolve(webappRoot, "cm/pages/app/index.jsp");

function read(path: string): string {
  // Normalize CRLF/LF so dual-tree equality is portable on Windows checkouts.
  return readFileSync(path, "utf8").replace(/\r\n/g, "\n");
}

function walkFiles(dir: string, acc: string[] = []): string[] {
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) {
      walkFiles(full, acc);
    } else {
      acc.push(full);
    }
  }
  return acc;
}

/** Pull quoted entries from a JSP `String[] name = new String[]{ ... };` block. */
function extractStringArray(text: string, name: string): string[] {
  const re = new RegExp(
    `String\\[\\]\\s+${name}\\s*=\\s*new String\\[\\]\\{([\\s\\S]*?)\\};`,
  );
  const match = text.match(re);
  expect(match, `${name} array`).toBeTruthy();
  return [...match![1].matchAll(/"([^"]+)"/g)].map((entry) => entry[1]);
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
  // #3587: leftover Architecture JSP host retired; filter 301 keeps bookmarks
  "cm/app/siteArchitecture.jsp",
  "cm/pages/app/siteArchitecture.jsp",
  // #3473: leftover Data Flow / CM1 asset editor JSP
  "cm/app/editAsset.jsp",
  "cm/pages/app/editAsset.jsp",
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
      // #3473: leftover editor / editAsset are SPA, not webmgt.jsp / editAsset.jsp
      expect(text).not.toMatch(/legacyViews\.put\("editor",\s*"webmgt\.jsp"\)/);
      expect(text).not.toMatch(/legacyViews\.put\("editAsset",\s*"editAsset\.jsp"\)/);
      expect(extractStringArray(text, "spaViews")).toContain("editor");
      expect(text).toContain("PSEditorHostRedirect");
      expect(text).toMatch(/editasset/i);
      expect(text).not.toContain('"/cm/app/?view=editor"');
      // #3306: Design template list is SPA entry, not legacyViews design → admin.jsp
      expect(text).not.toMatch(/legacyViews\.put\("design",\s*"admin\.jsp"\)/);
      expect(text).toMatch(/"design"/);
      expect(text).toMatch(/entry\s*=\s*"design"|entry = "design"/);
      // #3094 / #3099: Architecture is SPA entry, not legacyViews arch → siteArchitecture.jsp
      expect(text).not.toMatch(
        /legacyViews\.put\("arch",\s*"siteArchitecture\.jsp"\)/,
      );
      expect(text).toMatch(/"arch"/);
      expect(text).toMatch(/"architecture"/);
      expect(text).toMatch(/entry\s*=\s*"architecture"|entry = "architecture"/);
    }
  });

  it("admin.jsp hard-redirects to SPA Design (#3306)", () => {
    const hosts = [
      resolve(webappRoot, "cm/app/admin.jsp"),
      resolve(webappRoot, "cm/pages/app/admin.jsp"),
    ];
    for (const jsp of hosts) {
      expect(existsSync(jsp), jsp).toBe(true);
      const text = read(jsp);
      expect(text).toMatch(/setStatus\s*\(\s*301\s*\)/);
      expect(text).toContain("PSLegacyViewRedirect");
      expect(text).toContain('buildLocation("design"');
      expect(text).toContain("escapeHtmlAttribute");
      expect(text).toContain("htmlTarget");
      expect(text).toContain("Location");
      expect(text).not.toMatch(/target\s*=\s*"\/cm\/app\/\?"\s*\+\s*qs/);
      expect(text).not.toContain("PercTemplateLibraryWidget");
      expect(text).not.toContain("perc-assigned-templates");
    }
  });

  it("siteArchitecture.jsp host is retired; filter keeps bookmark 301 (#3587)", () => {
    const hosts = [
      resolve(webappRoot, "cm/app/siteArchitecture.jsp"),
      resolve(webappRoot, "cm/pages/app/siteArchitecture.jsp"),
    ];
    for (const jsp of hosts) {
      expect(existsSync(jsp), jsp).toBe(false);
    }
    expect(
      existsSync(resolve(__dirname, "../../../../war/app/siteArchitecture.jsp")),
    ).toBe(false);
    const filter = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java",
      ),
    );
    expect(filter).toContain("buildRetiredJspRedirectLocation");
    expect(filter).toContain("isRetiredArchitectureJsp");
    expect(filter).toContain("SC_MOVED_PERMANENTLY");
    expect(filter).toContain("PSLegacyViewRedirect");
    const redirect = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/util/PSEditorHostRedirect.java",
      ),
    );
    expect(redirect).toContain("sitearchitecture.jsp");
    const webXml = read(resolve(webappRoot, "WEB-INF/web.xml"));
    expect(webXml).toContain("PSRetiredJspRedirectServlet");
    expect(webXml).toContain("/cm/app/siteArchitecture.jsp");
    expect(webXml).toContain("<param-value>arch</param-value>");
    expect(webXml).toContain("/cm/app/editAsset.jsp");
    expect(webXml).toContain("/cm/pages/app/editAsset.jsp");
    expect(webXml).toContain("<param-value>editor</param-value>");
  });

  it("editAsset.jsp host is retired; filter keeps bookmark 301 (#3473)", () => {
    const hosts = [
      resolve(webappRoot, "cm/app/editAsset.jsp"),
      resolve(webappRoot, "cm/pages/app/editAsset.jsp"),
    ];
    for (const jsp of hosts) {
      expect(existsSync(jsp), jsp).toBe(false);
    }
    expect(
      existsSync(resolve(__dirname, "../../../../war/app/editAsset.jsp")),
    ).toBe(false);
    const filter = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java",
      ),
    );
    expect(filter).toContain("isRetiredEditAssetJsp");
  });

  it("legacy workflow/admin JSP hosts are retired; filter+servlet keep bookmark 301 (#6095)", () => {
    // FR-002 React-only mandate: five legacy hosts removed from the distribution.
    const retired = [
      "adminWorkflow.jsp",
      "percWorkflow.jsp",
      "percRoles.jsp",
      "percUsers.jsp",
      "percCategories.jsp",
    ];
    for (const file of retired) {
      const hosts = [
        resolve(webappRoot, `cm/app/${file}`),
        resolve(webappRoot, `cm/pages/app/${file}`),
      ];
      for (const jsp of hosts) {
        expect(existsSync(jsp), jsp).toBe(false);
      }
      expect(
        existsSync(resolve(__dirname, `../../../../war/app/${file}`)),
        `war/app/${file}`,
      ).toBe(false);
    }
    const filter = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java",
      ),
    );
    expect(filter).toContain("isRetiredWorkflowAdminJsp");
    const redirect = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/util/PSEditorHostRedirect.java",
      ),
    );
    expect(redirect).toContain("isRetiredWorkflowAdminJsp");
    const webXml = read(resolve(webappRoot, "WEB-INF/web.xml"));
    expect(webXml).toContain("PSRetiredWorkflowAdminRedirect");
    for (const file of retired) {
      expect(webXml).toContain(`/cm/app/${file}`);
      expect(webXml).toContain(`/cm/pages/app/${file}`);
    }
    expect(webXml).toContain("<param-value>admin</param-value>");
  });

  it("scheduler-console JSPs are retired; filter+servlet keep bookmark 301 (#6140)", () => {
    // FR-002 React-only mandate: TasksSection/TaskLogsSection/TaskNotifications/
    // ToolsSection own these surfaces. Kept (no React equivalent yet): TaskDetailLog,
    // console, index, AdminAuthentication, tools ConvertVariant/RxFix.
    const retiredTop = [
      "ScheduledTask.jsp",
      "ScheduledTaskList.jsp",
      "RemoveConfirmation.jsp",
      "TaskLogs.jsp",
      "DeleteAllTaskLogWarning.jsp",
      "NoTaskLogSelectionWarning.jsp",
      "TaskNotificationEditor.jsp",
      "TaskNotificationList.jsp",
    ];
    const retiredTools = [
      "ConsistencyChecker.jsp",
      "ConsistencyCheckerReport.jsp",
      "ConsistencyCheckerReview.jsp",
    ];
    for (const file of retiredTop) {
      expect(existsSync(resolve(webappRoot, `ui/admin/${file}`)), file).toBe(false);
    }
    for (const file of retiredTools) {
      expect(existsSync(resolve(webappRoot, `ui/admin/tools/${file}`)), file).toBe(false);
    }
    // Kept scheduler-console pages remain in the distribution.
    const kept = [
      "ui/admin/TaskDetailLog.jsp",
      "ui/admin/console.jsp",
      "ui/admin/index.jsp",
      "ui/admin/AdminAuthentication.jsp",
      "ui/admin/tools/ConvertVariant.jsp",
      "ui/admin/tools/ConvertVariantResult.jsp",
      "ui/admin/tools/RxFix.jsp",
      "ui/admin/tools/RxFixPreview.jsp",
    ];
    for (const rel of kept) {
      expect(existsSync(resolve(webappRoot, rel)), rel).toBe(true);
    }
    const filter = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java",
      ),
    );
    expect(filter).toContain("isRetiredSchedulerAdminJsp");
    const redirect = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/util/PSEditorHostRedirect.java",
      ),
    );
    expect(redirect).toContain("isRetiredSchedulerAdminJsp");
    const webXml = read(resolve(webappRoot, "WEB-INF/web.xml"));
    expect(webXml).toContain("PSRetiredSchedulerAdminRedirect");
    for (const file of retiredTop) {
      expect(webXml).toContain(`/ui/admin/${file}`);
    }
    for (const file of retiredTools) {
      expect(webXml).toContain(`/ui/admin/tools/${file}`);
    }
    expect(webXml).toContain("/ui/admin/ScheduledTaskList.faces");
    expect(webXml).toContain("/ui/admin/tools/ConsistencyChecker.faces");
    // /Rhythmyx-prefixed spellings: PSDispatcherFilter only strips the prefix for
    // its resourcePaths (/cm/ but not /ui/), so prefixed bookmarks need exact mappings.
    expect(webXml).toContain("/Rhythmyx/ui/admin/ScheduledTaskList.faces");
    expect(webXml).toContain("/Rhythmyx/ui/admin/TaskLogs.jsp");
    expect(webXml).toContain("/Rhythmyx/ui/admin/tools/ConsistencyChecker.faces");
    expect(webXml).toContain("<param-value>admin</param-value>");
    // Filter covers the /ui/admin tree so bypassed bookmarks still 301.
    expect(webXml).toContain("<url-pattern>/ui/admin/*</url-pattern>");
  });

  it("workflow dialog-pane JSPs are retired; filter+servlet keep editor 301 (#6139)", () => {
    // FR-002 React-only mandate: EditorWorkflowPanel (EditorHost edit mode) owns
    // in-context transitions, comment enforcement, and ad-hoc assignees via REST
    // (us4-item-transitions asserts zero legacy-CE requests). The Dojo
    // WorkflowActions.js dialog panes are deleted; stragglers 301 to the SPA
    // editor shell. Sibling UserStatus.jsp / help pages keep shipping.
    const panes = [
      "workflowactions.jsp",
      "adhocsearch.jsp",
      "adhocresults.jsp",
    ];
    for (const file of panes) {
      expect(
        existsSync(resolve(webappRoot, `ui/activeassembly/workflow/${file}`)),
        file,
      ).toBe(false);
      expect(
        existsSync(resolve(__dirname, `../../../../war/app/${file}`)),
        `war/app/${file}`,
      ).toBe(false);
    }
    for (const rel of [
      "ui/activeassembly/UserStatus.jsp",
      "ui/activeassembly/help/aboutaahelp.jsp",
    ]) {
      expect(existsSync(resolve(webappRoot, rel)), rel).toBe(true);
    }
    const filter = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java",
      ),
    );
    expect(filter).toContain("isRetiredWorkflowDialogJsp");
    const redirect = read(
      resolve(
        __dirname,
        "../../../main/java/com/percussion/webui/util/PSEditorHostRedirect.java",
      ),
    );
    expect(redirect).toContain("isRetiredWorkflowDialogJsp");
    const webXml = read(resolve(webappRoot, "WEB-INF/web.xml"));
    expect(webXml).toContain("PSRetiredWorkflowDialogsRedirect");
    for (const file of panes) {
      expect(webXml).toContain(`/ui/activeassembly/workflow/${file}`);
      expect(webXml).toContain(`/Rhythmyx/ui/activeassembly/workflow/${file}`);
    }
    expect(webXml).toContain("<param-value>editor</param-value>");
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
    expect(text).not.toMatch(/spa\.jsp\?[^"'\n]*#/);
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
    expect(text).toContain("DESIGN_SECTIONS");
    // PR-7: dash maps to Home gadgets, not legacy dashboard.jsp
    expect(text).toContain('"gadgets"');
    expect(text).toMatch(/"dash"/);
    expect(text).not.toMatch(/legacyViews\.put\("dash",\s*"dashboard\.jsp"\)/);
  });

  it("dual-tree index.jsp files stay aligned for SPA cutover", () => {
    expect(read(appIndex)).toBe(read(pagesIndex));
  });

  it("explorer is an ungated spaView (not adminViews) so Designer landings are not reset", () => {
    for (const indexPath of [appIndex, pagesIndex]) {
      const text = read(indexPath);
      const spa = extractStringArray(text, "spaViews");
      const admin = extractStringArray(text, "adminViews");
      const designer = extractStringArray(text, "designerViews");
      expect(spa).toContain("explorer");
      // Gate at isAdminView && !admin: explorer must stay off adminViews.
      // designerViews is only consulted inside that gate — listing explorer
      // there would be a no-op and would not authorize Contributors.
      expect(admin).not.toContain("explorer");
      expect(designer).not.toContain("explorer");
      // #3473: leftover editor landing is SPA, still ungated like Home
      expect(spa).toContain("editor");
      expect(admin).not.toContain("editor");
    }
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

  it("assetPickerModern.jsp loads a single perc-modern-ui.js module URL (#3438)", () => {
    const text = read(resolve(webappRoot, "cm/app/assetPickerModern.jsp"));
    expect(text).toContain('s.src = "/cm/modern/assets/perc-modern-ui.js"');
    expect(text).not.toMatch(/perc-modern-ui\.js\?cb=/);
    expect(text).not.toMatch(/Date\.now\s*\(/);
    expect(text).toContain('initialPath: "//Sites"');
    expect(text).toContain("enableSearch: true");
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
    // #3219: default sys_redirect is the dispatcher (homepage resolve), not Home
    expect(text.replace(/\s+/g, " ")).toMatch(
      /String defaultRedirect\s*=\s*"\/cm\/app\/"\s*;/,
    );
    expect(text).not.toMatch(
      /String defaultRedirect\s*=\s*"\/cm\/app\/spa\.jsp\?entry=home"/,
    );
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

describe("leftover Data Flow CE HTML retirement (#3473)", () => {
  it("product SPA TypeScript does not open leftover CE requestors", () => {
    const root = resolve(__dirname, "../../../main/ts");
    const leftover =
      /['"`][^'"`\n]*(editAsset\.jsp|[?&]view=editor|checkoutedit\.xml|contenteditorurls\.html|sys_ceSupport\/)/i;
    const offenders: string[] = [];
    for (const file of walkFiles(root)) {
      if (!/\.(ts|tsx)$/.test(file)) {
        continue;
      }
      const text = read(file);
      const hits = text.split("\n").filter((line) => {
        const t = line.trim();
        if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) {
          return false;
        }
        return leftover.test(line);
      });
      if (hits.length) {
        offenders.push(file);
      }
    }
    expect(offenders, offenders.join("\n")).toEqual([]);
  });
});


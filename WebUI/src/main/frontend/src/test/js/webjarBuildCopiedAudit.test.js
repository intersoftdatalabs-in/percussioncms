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
 * Slice-3 audit: WebJar/Maven-unpacked + build-copied JS assets (issue #7167,
 * parent audit #5627).
 *
 * Scope of this slice is everything that arrives in the product WITHOUT going
 * through the npm graph and that is NOT owned by sibling slices:
 *
 *   A. WebJar / Maven-unpacked JS.
 *      The repo carries NO unpacked-WebJar JS anymore: the TinyMCE WebJar
 *      retired with modules/perc-tinymce in #5734 (owned by epic #5622),
 *      CodeMirror is npm-managed via modules/perc-common-ui-bundle
 *      (owned by #5626), and the last `org.webjars` dependencyManagement pin
 *      (swagger-ui) was dead — removed by this slice. The only
 *      Maven-resolved swagger UI bits are the CXF
 *      `cxf-rt-rs-service-description-swagger-ui` runtime jar (Dependabot-
 *      visible, no vendoring) and the unpkg CDN pins in the openapi webapp
 *      (follow-on #7281: self-hosted via npm; CXF dep removed as unreachable).
 *   B. Build-copied / stale-mirror JS.
 *      `WebUI/war/` is a checked-in stale mirror that the WAR build never
 *      packages (maven-war-plugin webResources are `src/main/webapp` +
 *      `target/generated-webui` + config overlays — no `war/` directory).
 *      Its `war/api/` subtree was the retired Swagger-UI 1.x/2.x-era viewer
 *      (#5672 retired the live viewer to a redirect stub): the referenced
 *      jquery/backbone/underscore copies were already deleted by #5664, and
 *      the residual below (notably Handlebars 1.0.0, CVE-exposed) was dead
 *      weight with zero live references. Follow-on #7280 deleted the whole
 *      `war/api/` tree (16 files); this audit now carries a stays-deleted
 *      guard (RETIRED_7280) instead of version-banner pins.
 *   C. Explicitly OUT of this slice (cross-check, no double-count):
 *      - WebUI jslib/cm trees -> slice 1, #7165 (pinned in
 *        jslibVendoredAudit.test.js; migrations #7175-#7181).
 *      - DTS web resources + installer payloads -> slice 2, #7166.
 *      - perc-packages widget payloads (moment/fullcalendar/lodash/slick/
 *        superfish/...) -> #6319/#6320/#6321/#6686/#6919 cluster.
 *      - TinyMCE bundle -> epic #5622. CodeMirror WebJar -> #5626.
 *
 * Inventory (this slice; war/api rows deleted by #7280 — kept as record):
 *
 * | path (under WebUI/) | version | license | consumer | npm | migration |
 * |------|---------|---------|----------|-----|-----------|
 * | war/api/lib/handlebars-1.0.0.js | 1.0.0 (2011 Katz) | MIT | NONE (dead) | handlebars (modern) | DELETED by #7280 |
 * | war/api/lib/highlight.7.3.pack.js | 7.3 pack | UNSTATED | NONE (dead) | highlight.js | DELETED by #7280 |
 * | war/api/lib/shred.bundle.js + lib/shred/content.js | shred-era | UNSTATED | NONE (dead) | none | DELETED by #7280 |
 * | war/api/lib/swagger.js | 2.0.37 (wordnik) | UNSTATED | NONE (dead) | n/a (retired viewer) | DELETED by #7280 |
 * | war/api/swagger-ui.js | 2.0.22 (wordnik) | UNSTATED | NONE (dead) | n/a (retired viewer) | DELETED by #7280 |
 * | war/api/lib/swagger-oauth.js + o2c.html | 2.x-era | UNSTATED | NONE (dead) | n/a | DELETED by #7280 |
 * | war/api/css/* + war/api/images/* | n/a (theme) | UNSTATED | NONE (dead) | n/a | DELETED by #7280 |
 * | war/api/index.html | stale viewer | UNSTATED | NONE (dead; live cm/api is a redirect stub) | n/a | DELETED by #7280 |
 * | modules/perc-openapi-webapp swagger-ui | swagger-ui-dist 5.36.0 (npm-tracked) | Apache-2.0 (upstream) | /openapi viewer (self-hosted ./swagger-ui/) | swagger-ui-dist | #7281 (LANDED this slice) |
 * | root pom org.webjars:swagger-ui pin | 5.32.15 (dead pin) | n/a | NONE (no module declares it) | n/a | REMOVED this slice |
 * | WebUI cxf-rt-rs-service-description-swagger-ui | cxf.version (Maven-tracked) | Apache-2.0 | NONE active (Swagger2Feature commented out) | n/a | REMOVED by #7281 (unreachable) |
 */

import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const WEBUI_DIR = path.join(FRONTEND_ROOT, "..", "..", "..");
const REPO_ROOT = path.join(WEBUI_DIR, "..");
const WEBAPP_CM = path.join(WEBUI_DIR, "src", "main", "webapp", "cm");
const WAR_API = path.join(WEBUI_DIR, "war", "api");

/**
 * Issue #7280 deleted the dead war/api swagger-viewer tree (16 files, the
 * full inventory above). Stays-deleted guard: none of these paths may
 * reappear under WebUI/war/api/. Mirrors the RETIRED_* guards in
 * jslibVendoredAudit.test.js (slice 1).
 */
const RETIRED_7280 = [
  "lib/handlebars-1.0.0.js",
  "lib/highlight.7.3.pack.js",
  "lib/shred.bundle.js",
  "lib/shred/content.js",
  "lib/swagger.js",
  "swagger-ui.js",
  "lib/swagger-oauth.js",
  "o2c.html",
  "index.html",
  "css/screen.css",
  "css/reset.css",
  "images/explorer_icons.png",
  "images/logo_small.png",
  "images/pet_store_api.png",
  "images/throbber.gif",
  "images/wordnik_api.png",
];

describe("webjar/build-copied slice-3 audit (issue #7167)", () => {
  it("deleted war/api swagger-viewer tree stays deleted (#7280)", () => {
    expect(RETIRED_7280.length).toBe(16);
    expect(
      fs.existsSync(WAR_API),
      "dead war/api tree resurrected; see #7280 (zero live references, never packaged)",
    ).toBe(false);
    for (const rel of RETIRED_7280) {
      expect(
        fs.existsSync(path.join(WAR_API, rel)),
        `deleted file resurrected: war/api/${rel}`,
      ).toBe(false);
    }
  });

  it("dead war/api tree is not shipped and not referenced by live sources", () => {
    // The WAR build never packages WebUI/war/ (webResources are
    // src/main/webapp + target/generated-webui + config overlays).
    const webuiPom = fs.readFileSync(path.join(WEBUI_DIR, "pom.xml"), "utf8");
    expect(webuiPom, "WebUI build must not package WebUI/war/").not.toMatch(
      /<directory>war<\/directory>/,
    );
    // None of the deleted files may exist under the shipped tree.
    for (const rel of RETIRED_7280) {
      const base = path.basename(rel);
      const shipped = path.join(WEBAPP_CM, "api", "lib", base);
      expect(fs.existsSync(shipped), `dead file resurrected in shipped tree: ${shipped}`).toBe(false);
    }
    expect(
      fs.existsSync(path.join(WEBAPP_CM, "api", "swagger-ui.js")),
      "dead swagger-ui.js resurrected in shipped tree",
    ).toBe(false);
    // The live viewer is a redirect stub with no vendored script loads.
    const liveIndex = path.join(WEBAPP_CM, "api", "index.html");
    expect(fs.existsSync(liveIndex), "live cm/api redirect stub missing").toBe(true);
    const live = fs.readFileSync(liveIndex, "utf8");
    expect(live, "live cm/api viewer must redirect to /openapi/").toMatch(/meta[^>]*refresh/i);
    expect(live, "live cm/api viewer must load no vendored scripts").not.toMatch(/<script\s+src=/i);
  });

  it("no live source references the dead war/api tree", () => {
    // webapp sources, minify inputs, and the openapi webapp must not load
    // war/api assets; only audit tests may mention the path.
    const roots = [
      path.join(WEBUI_DIR, "src", "main", "webapp"),
      path.join(REPO_ROOT, "modules", "perc-openapi-webapp", "src"),
    ];
    const hits = [];
    const walk = (dir) => {
      for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const abs = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          if (entry.name === "node_modules" || entry.name === "target") continue;
          walk(abs);
        } else if (/\.(jsp|jspx|html|js|ts|tsx|xml)$/i.test(entry.name)) {
          const text = fs.readFileSync(abs, "utf8");
          if (/war\/api\//.test(text)) hits.push(abs);
        }
      }
    };
    for (const root of roots) {
      if (fs.existsSync(root)) walk(root);
    }
    expect(hits, `live references to dead war/api tree: ${hits.join(", ")}`).toEqual([]);
  });

  it("no org.webjars dependency remains in the build (dead pin removed)", () => {
    // CodeMirror (#5626) and TinyMCE (#5622) WebJars are long retired; the
    // last org.webjars dependencyManagement pin (swagger-ui) had no
    // declaring module and was removed by this slice. Any re-added WebJar
    // dependency must come with a migration issue, not silent vendoring.
    const rootPom = fs.readFileSync(path.join(REPO_ROOT, "pom.xml"), "utf8");
    expect(rootPom, "org.webjars dependency reintroduced").not.toMatch(
      /<groupId>org\.webjars/,
    );
    expect(rootPom, "swagger.ui.version property reintroduced").not.toMatch(/swagger\.ui\.version/);
    // CodeMirror stays npm-managed (owned by #5626): the bundle module
    // declares it in package.json, not via Maven.
    const bundlePkg = JSON.parse(
      fs.readFileSync(
        path.join(REPO_ROOT, "modules", "perc-common-ui-bundle", "package.json"),
        "utf8",
      ),
    );
    expect(bundlePkg.dependencies?.codemirror ?? bundlePkg.devDependencies?.codemirror).toBeTruthy();
  });

  it("swagger-ui serving is npm self-hosted, CXF viewer dep removed", () => {
    // #7281 (implementing #5627): the /openapi viewer loads
    // swagger-ui-dist from the npm graph (self-hosted under ./swagger-ui/,
    // air-gap friendly, Dependabot-visible) — no CDN pins. The CXF
    // cxf-rt-rs-service-description-swagger-ui runtime jar was dropped from
    // WebUI: its Swagger2Feature beans are commented out and no code
    // references org.apache.cxf.jaxrs.swagger types, so the bundled
    // swagger-ui endpoint was unreachable. Any reintroduced CDN load or
    // checked-in swagger bundle must come with a migration issue, not
    // silent vendoring.
    const webuiPom = fs.readFileSync(path.join(WEBUI_DIR, "pom.xml"), "utf8");
    expect(webuiPom, "CXF swagger-ui dep must stay removed (see #7281)").not.toMatch(
      /cxf-rt-rs-service-description-swagger-ui/,
    );
    const openapiIndex = path.join(
      REPO_ROOT,
      "modules",
      "perc-openapi-webapp",
      "src",
      "main",
      "webapp",
      "index.html",
    );
    expect(fs.existsSync(openapiIndex)).toBe(true);
    const html = fs.readFileSync(openapiIndex, "utf8");
    expect(html, "openapi viewer must load the self-hosted swagger bundle").toMatch(
      /\.\/swagger-ui\/swagger-ui-bundle\.js/,
    );
    expect(html, "openapi viewer must load the self-hosted swagger CSS").toMatch(
      /\.\/swagger-ui\/swagger-ui\.css/,
    );
    expect(html, "openapi viewer must not load third-party CDN scripts").not.toMatch(
      /src=["']https?:\/\//i,
    );
    expect(html, "openapi viewer must not load third-party CDN styles").not.toMatch(
      /href=["']https?:\/\//i,
    );
    expect(html, "openapi viewer must not load a checked-in swagger bundle").not.toMatch(
      /src=["'](lib|js|vendor)\//i,
    );
    // The self-hosted assets stay npm-tracked (Dependabot-visible): the
    // module package.json pins swagger-ui-dist and the build syncs it.
    const openapiPkg = JSON.parse(
      fs.readFileSync(
        path.join(REPO_ROOT, "modules", "perc-openapi-webapp", "package.json"),
        "utf8",
      ),
    );
    expect(openapiPkg.dependencies?.["swagger-ui-dist"]).toBeTruthy();
  });

  it("sibling slice scopes stay owned elsewhere (no double-count)", () => {
    // Slice 1 (#7165) owns the jslib/cm trees: its audit test must exist.
    expect(
      fs.existsSync(path.join(__dirname, "jslibVendoredAudit.test.js")),
      "slice-1 audit test missing; slice scopes may have drifted",
    ).toBe(true);
    // Slice 2 (#7166) owns DTS web resources + installer payloads: those
    // trees exist and this test asserts nothing about their contents.
    for (const dir of [
      path.join(REPO_ROOT, "deliverytiersuite"),
      path.join(REPO_ROOT, "modules", "perc-toolkit", "src", "main", "resources", "InstallDir"),
    ]) {
      expect(fs.existsSync(dir), `slice-2 scope tree missing: ${dir}`).toBe(true);
    }
  });
});

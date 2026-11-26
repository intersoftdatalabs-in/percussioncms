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
 * Apache frontend smoke for the `--with-apache` QA cell (issue #7323,
 * unblocks #7233 / #7164).
 *
 * <p>Tags: {@code @apache} {@code @apache-frontend}</p>
 *
 * <p>Run (needs the Apache sidecar from {@code perc-devctl qa-up
 * --with-apache}, which prints the origins in its endpoint banner):
 * {@code APACHE_HTTPS_URL=https://127.0.0.1:<port>
 * APACHE_HTTP_URL=http://127.0.0.1:<port>
 * npm run test:surface -- --path tests/apache-frontend-smoke.spec.js}</p>
 *
 * <p>What it proves: the vhost serves the published fixture static
 * tree on :80/:443 (home, category, web_resources bytes, no retired
 * DynaTree copies), the backend-owned probe path answers through the
 * vhost proxy (Tomcat {@code Coyote} Server header behind the Apache
 * frontend), and the home page loads with zero console errors.</p>
 *
 * <p>Without {@code APACHE_HTTPS_URL} both tests skip cleanly — the
 * default CMS-only and DTS-only cells have no Apache frontend. The
 * cell TLS identity is a throwaway mkcert test CA, so the browser
 * contexts allow the untrusted chain (same precedent as the shipped
 * DTS HTTPS flow, #6375); the genuine chain is verified separately by
 * {@code qa-health}'s {@code curl --cacert} probes.</p>
 */

const { test, expect } = require("@playwright/test");

const PUBLISHED_WEB_RESOURCES = [
  "/web_resources/cm/jslib/jquery.fancytree.js",
  "/web_resources/cm/css/fancytree/skin/ui.fancytree.css",
];

const REMOVED_DYNATREE_PUBLISHED = ["/web_resources/cm/jslib/jquery.dynatree.js"];

// Backend-owned marker (installer-deployed DTS ROOT error document):
// static, unauthenticated, always present on a fresh DTS install. Apache
// holds no local copy, so a 200 through the vhost can only come from DTS.
const PROXY_PROBE_PATH = "/error.html";

// Service-map probe (exercises the per-context ProxyPass entries, not
// just the `/` fallback): the spec asserts vhost-vs-direct status parity
// (absolute codes shift with seeds — parity never lies).
const SERVICE_PROBE_PATH = "/feeds/rss";

const SKIP_NO_APACHE =
  "needs APACHE_HTTPS_URL=(qa-up --with-apache banner HTTPS origin); "
  + "default CMS-only / DTS-only cells have no Apache frontend (#7323)";

function apacheOrigins() {
  const https = process.env.APACHE_HTTPS_URL || "";
  const http = process.env.APACHE_HTTP_URL || "";
  if (!https && !http) {
    return null;
  }
  return { https: https || null, http: http || https };
}

test.describe("Apache frontend smoke for the --with-apache cell (issue #7323)", () => {
  // Page browsers honor this; API request contexts do not, so the
  // https request tests below open their own ignoring context (same
  // precedent as the shipped DTS HTTPS flow, #6375).
  test.use({ ignoreHTTPSErrors: true });

  test(
    "vhost serves static tree and proxies the DTS backend @apache",
    { tag: ["@apache", "@apache-frontend"] },
    async ({ playwright }) => {
      const origins = apacheOrigins();
      test.skip(!origins || !origins.https, SKIP_NO_APACHE);
      const https = origins.https;
      const request = await playwright.request.newContext({
        ignoreHTTPSErrors: true,
      });
      try {
      const home = await request.get(`${https}/`);
      expect(home.status(), "expected 200 for vhost home").toBe(200);
      expect(
        home.headers()["server"] || "",
        "home must be Apache-direct (static offload)",
      ).toMatch(/Apache\//);

      const category = await request.get(`${https}/category.html`);
      expect(category.status(), "expected 200 for fixture category").toBe(200);

      for (const asset of PUBLISHED_WEB_RESOURCES) {
        const resp = await request.get(`${https}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of REMOVED_DYNATREE_PUBLISHED) {
        const resp = await request.get(`${https}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed dead copy ${asset}`,
        ).toBe(404);
      }

      const proxied = await request.get(`${https}${PROXY_PROBE_PATH}`);
      expect(
        proxied.status(),
        "expected 200 for the backend-owned probe path through the vhost",
      ).toBe(200);
      } finally {
        await request.dispose();
      }
    },
  );

  test(
    "service contexts transit the vhost proxy with backend parity @apache",
    { tag: ["@apache", "@apache-frontend"] },
    async ({ playwright }) => {
      const origins = apacheOrigins();
      test.skip(!origins || !origins.https, SKIP_NO_APACHE);
      const request = await playwright.request.newContext({
        ignoreHTTPSErrors: true,
      });
      try {
      // Absolute service codes shift with seeds/config, so the gate
      // asserts parity: the vhost status must equal the direct-DTS
      // status (PUBLISHED_SITE_URL is the standard direct origin printed
      // by every --with-dts boot).
      const direct =
        process.env.PUBLISHED_SITE_URL || process.env.DTS_URL || "";
      test.skip(
        !direct,
        "needs PUBLISHED_SITE_URL=(direct DTS origin for parity) — "
          + "run qa-up --with-dts (implied by --with-apache, #7323)",
      );
      const https = origins.https;
      // maxRedirects: 0 keeps the first hop as a plain response: the
      // unseeded feeds path 302s (error document), and following that
      // hop would chase the backend's own redirect chain instead of
      // asserting parity.
      const vhost = await request.get(`${https}${SERVICE_PROBE_PATH}`, {
        maxRedirects: 0,
      });
      const backend = await request.get(
        `${direct.replace(/\/$/, "")}${SERVICE_PROBE_PATH}`,
        { maxRedirects: 0 },
      );
      expect(
        vhost.status(),
        `vhost service status must match direct DTS (${backend.status()})`,
      ).toBe(backend.status());
      expect(
        vhost.status(),
        "unexpected service failure through the vhost",
      ).toBeLessThan(500);
      } finally {
        await request.dispose();
      }
    },
  );

  test(
    "vhost home page loads with zero console errors @apache",
    { tag: ["@apache", "@apache-frontend"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const origins = apacheOrigins();
      test.skip(!origins || !origins.https, SKIP_NO_APACHE);

      const failedRequests = [];
      page.on("requestfailed", (req) => {
        if (/web_resources/i.test(req.url())) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && /web_resources/i.test(resp.url())) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });
      const consoleErrors = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String((err && err.stack) || err));
      });

      await page.goto(origins.https, { waitUntil: "networkidle" });

      expect(
        failedRequests,
        `failed web_resources requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        consoleErrors,
        `console errors:\n${consoleErrors.join("\n")}`,
      ).toEqual([]);
      expect(
        pageErrors,
        `page errors:\n${pageErrors.join("\n")}`,
      ).toEqual([]);
    },
  );
});

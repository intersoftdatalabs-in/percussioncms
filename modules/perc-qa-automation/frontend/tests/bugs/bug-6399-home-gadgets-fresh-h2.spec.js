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
 * Regression: Home gadgets REST on a fresh CMS-only H2 cell (GH-6399 residual
 * of #6302 / QA #6329).
 *
 * <p>On a fresh cell the delivery tier is unreachable and Google Analytics is
 * never configured, so the Form Tracker gadget call
 * {@code GET .../assetmanagement/asset/forms/<site>} and the Traffic gadget
 * call {@code POST .../activitymanagement/activity/contenttraffic} (dashboard
 * default path {@code /Sites/<site-name>}) both returned HTTP 500. The server
 * now returns HTTP 200 empty payloads instead:
 * forms degrade to an empty list when the delivery server is unreachable, and
 * traffic zero-fills the visits series when analytics is not configured while
 * resolving page lookups through the site's repository folder.</p>
 *
 * <p>Requires live CMS (QA mode via {@code perc-devctl qa-up} + {@code TEST_CMS_URL}
 * preferred). Surface-filterable path — do not require full suite.</p>
 *
 * <pre>
 *   # From repo root
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/bugs/bug-6399-home-gadgets-fresh-h2.spec.js
 * </pre>
 *
 * <p>Tags: {@code @home} {@code @dashboard} {@code @gadgets}</p>
 */

const { test, expect } = require("@playwright/test");
const { BASE_URL } = require("../helpers/auth");

function basicHeaders() {
  return {
    RX_USEBASICAUTH: "true",
    Authorization: `Basic ${Buffer.from(`Admin:${process.env.ADMIN_PASSWORD}`, "utf8").toString("base64")}`,
  };
}

/** First site summary from {@code GET .../sitemanage/site/} (SiteSummary envelope or bare array). */
async function fetchFirstSite(request) {
  const resp = await request.get(`${BASE_URL}/Rhythmyx/services/sitemanage/site/`, {
    headers: basicHeaders(),
  });
  expect(resp.ok(), `GET sites ${resp.status()}`).toBeTruthy();
  const data = await resp.json();
  const list = Array.isArray(data) ? data : data.SiteSummary;
  const sites = (Array.isArray(list) ? list : [list]).filter((s) => s && s.name);
  expect(sites.length, "fresh cell exposes at least one site").toBeGreaterThan(0);
  return sites[0];
}

function trafficBody(path) {
  const fmt = (d) =>
    `${String(d.getMonth() + 1).padStart(2, "0")}/${String(d.getDate()).padStart(2, "0")}/${d.getFullYear()}`;
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 30);
  return {
    ContentTrafficRequest: {
      path,
      startDate: fmt(start),
      endDate: fmt(end),
      granularity: "DAY",
      usage: "uniquepageviews",
      trafficRequested: ["LIVE_PAGES", "NEW_PAGES", "UPDATED_PAGES", "TAKE_DOWNS", "VISITS"],
    },
  };
}

test("forms gadget REST returns 200 empty (not 500) with unreachable DTS", async ({ request }) => {
  const site = await fetchFirstSite(request);
  const resp = await request.get(
    `${BASE_URL}/Rhythmyx/services/assetmanagement/asset/forms/${encodeURIComponent(site.name)}`,
    { headers: basicHeaders() },
  );
  expect(resp.status(), "GET forms/{site}").toBe(200);
  const data = await resp.json();
  const list = Array.isArray(data) ? data : (data.ArrayList ?? data.FormSummary ?? []);
  expect(Array.isArray(list), "forms payload is a list envelope").toBeTruthy();
});

test("traffic gadget REST returns 200 (not 500) for dashboard default site path", async ({
  request,
}) => {
  const site = await fetchFirstSite(request);
  const resp = await request.post(
    `${BASE_URL}/Rhythmyx/services/activitymanagement/activity/contenttraffic`,
    {
      headers: { ...basicHeaders(), "Content-Type": "application/json" },
      data: trafficBody(`/Sites/${site.name}`),
    },
  );
  expect(resp.status(), "POST contenttraffic").toBe(200);
  const data = await resp.json();
  const traffic = data.ContentTraffic || data;
  expect(Array.isArray(traffic.dates), "dates series present").toBeTruthy();
  expect(traffic.dates.length, "dates non-empty").toBeGreaterThan(0);
  for (const key of ["visits", "livePages", "newPages", "pageUpdates", "takeDowns"]) {
    expect(Array.isArray(traffic[key]), `${key} series present`).toBeTruthy();
    expect(traffic[key].length, `${key} aligned with dates`).toBe(traffic.dates.length);
  }
});

test("traffic gadget REST stays 200 for unknown paths", async ({ request }) => {
  const resp = await request.post(
    `${BASE_URL}/Rhythmyx/services/activitymanagement/activity/contenttraffic`,
    {
      headers: { ...basicHeaders(), "Content-Type": "application/json" },
      data: trafficBody("/Sites/NoSuchSite"),
    },
  );
  expect(resp.status(), "POST contenttraffic unknown path").toBe(200);
});

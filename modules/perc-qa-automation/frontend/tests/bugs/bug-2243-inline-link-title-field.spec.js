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
 * Residual automation: custom inline-link title field (#2243 / parent #946 slice 4).
 *
 * Product slices 1–3 landed (#2254 / #2256 / #2258). Server unit coverage is
 * already on main. This residual exercises the live CMS path:
 *
 *   1) REST renderlink/preview without titleField (type default BC)
 *   2) REST with titleField=page_title or resource_link_title (custom / fallback peers)
 *   3) Optional UI smoke: content editor shell reachable when fixtures exist
 *
 * Full configure-control → TinyMCE insert → DOM title is heavy and fixture-
 * dependent; stock H2 often has empty Sites. Soft-skip with BUG + durable URL
 * when no page/asset id is discoverable (not a silent flake).
 *
 * Recipe:
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \\
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=... \\
 *     npm test -- tests/bugs/bug-2243-inline-link-title-field.spec.js
 * </pre>
 *
 * Tags: @webui @tinymce @inline-link @title-field
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const {
  buildRenderLinkPreviewUrl,
  extractTitleFromPreviewBody,
  pathItemsWithIds,
  inlineLinkTitleFixturesSkipReason,
  PAGE_DEFAULT_TITLE_FIELD,
  DISPLAYTITLE_FIELD,
} = require("../helpers/inline-link-title");

const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;

/**
 * Walk Sites (shallow) for a content item with an id usable for render preview.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<{ id: string, name: string }|null>}
 */
async function findPreviewableItem(request) {
  const headers = adminBasicAuthHeaders();
  const sitesRes = await request.get(`${PATH_FOLDER}/Sites`, { headers });
  if (sitesRes.status() !== 200) {
    return null;
  }
  const sitesBody = await sitesRes.json();
  const sites = pathItemsWithIds(sitesBody);

  // Prefer a direct child that looks like a page; else descend one level.
  for (const site of sites) {
    // Some installs expose pages at site root; ids may be folder-ish.
    if (site.id && !String(site.name).toLowerCase().includes("folder")) {
      // Probe preview — folders fail; pages/assets succeed with title.
      const probe = await request.get(
        buildRenderLinkPreviewUrl(BASE_URL, site.id),
        { headers },
      );
      if (probe.status() === 200) {
        const title = extractTitleFromPreviewBody(await probe.json());
        if (title != null) {
          return site;
        }
      }
    }
  }

  for (const site of sites) {
    const folderRes = await request.get(
      `${PATH_FOLDER}/Sites/${encodeURIComponent(site.name)}`,
      { headers },
    );
    if (folderRes.status() !== 200) {
      continue;
    }
    const children = pathItemsWithIds(await folderRes.json());
    for (const child of children) {
      const probe = await request.get(
        buildRenderLinkPreviewUrl(BASE_URL, child.id),
        { headers },
      );
      if (probe.status() !== 200) {
        continue;
      }
      const title = extractTitleFromPreviewBody(await probe.json());
      if (title != null) {
        return child;
      }
    }
  }

  // Assets tree as last resort (displaytitle default)
  const assetsRes = await request.get(`${PATH_FOLDER}/Assets`, { headers });
  if (assetsRes.status() === 200) {
    const assets = pathItemsWithIds(await assetsRes.json());
    for (const a of assets.slice(0, 15)) {
      const probe = await request.get(
        buildRenderLinkPreviewUrl(BASE_URL, a.id),
        { headers },
      );
      if (probe.status() !== 200) {
        continue;
      }
      const title = extractTitleFromPreviewBody(await probe.json());
      if (title != null) {
        return a;
      }
    }
  }

  return null;
}

test.describe("Inline link title field residual (#2243 / #946 slice 4)", () => {
  test("REST: preview default title + titleField query (custom / fallback peers)", async ({
    request,
  }) => {
    test.setTimeout(60_000);
    const headers = adminBasicAuthHeaders();

    const item = await findPreviewableItem(request);
    if (!item) {
      test.skip(inlineLinkTitleFixturesSkipReason());
      return;
    }

    // 1) Type default (no titleField) — BC for empty control setting
    const defaultUrl = buildRenderLinkPreviewUrl(BASE_URL, item.id);
    const defaultRes = await request.get(defaultUrl, { headers });
    expect(
      defaultRes.status(),
      `GET default preview for ${item.id} (${item.name})`,
    ).toBe(200);
    const defaultBody = await defaultRes.json();
    const defaultTitle = extractTitleFromPreviewBody(defaultBody);
    expect(
      defaultTitle,
      "default preview must yield a non-empty title (type default)",
    ).toBeTruthy();

    // 2) Explicit page default field name (resource_link_title) — should not 500
    const rltUrl = buildRenderLinkPreviewUrl(
      BASE_URL,
      item.id,
      PAGE_DEFAULT_TITLE_FIELD,
    );
    const rltRes = await request.get(rltUrl, { headers });
    expect(
      rltRes.status(),
      `GET preview?titleField=${PAGE_DEFAULT_TITLE_FIELD}`,
    ).toBe(200);
    const rltTitle = extractTitleFromPreviewBody(await rltRes.json());
    // May equal default for pages; assets may still return a title via fallback
    expect(
      rltTitle == null || typeof rltTitle === "string",
      "titleField resource_link_title response has title string or null",
    ).toBe(true);

    // 3) displaytitle peer (asset default / page fallback)
    const dtUrl = buildRenderLinkPreviewUrl(
      BASE_URL,
      item.id,
      DISPLAYTITLE_FIELD,
    );
    const dtRes = await request.get(dtUrl, { headers });
    expect(dtRes.status(), `GET preview?titleField=${DISPLAYTITLE_FIELD}`).toBe(
      200,
    );
    const dtTitle = extractTitleFromPreviewBody(await dtRes.json());
    expect(
      dtTitle == null || typeof dtTitle === "string",
      "titleField displaytitle response has title string or null",
    ).toBe(true);

    // 4) Custom field name that is often present on pages (page_title)
    const customUrl = buildRenderLinkPreviewUrl(BASE_URL, item.id, "page_title");
    const customRes = await request.get(customUrl, { headers });
    expect(customRes.status(), "GET preview?titleField=page_title").toBe(200);
    const customTitle = extractTitleFromPreviewBody(await customRes.json());
    // Fallback chain: if page_title missing, server returns displaytitle or type default
    expect(
      customTitle,
      "custom titleField must resolve to some title via fallback chain",
    ).toBeTruthy();

    // At least one of the titled responses should be a non-empty string
    const anyTitle = customTitle || rltTitle || dtTitle || defaultTitle;
    expect(String(anyTitle).trim().length).toBeGreaterThan(0);
  });

  test("UI: modern shell login works when CMS is up (fixture gate for full insert path)", async ({
    page,
  }) => {
    test.setTimeout(45_000);
    // Soft fixture check via REST first so empty H2 does not flake UI hard asserts
    // on missing content-type / TinyMCE control UI.
    const item = await findPreviewableItem(page.request);
    if (!item) {
      test.skip(inlineLinkTitleFixturesSkipReason());
      return;
    }

    await loginAsAdmin(page);
    // Landing shell — proves CMS UI session for a follow-up full insert E2E
    // when control-setting fixtures are seeded (content type editor + TinyMCE).
    await page.goto(`${BASE_URL}/cm/app/index.jsp`);
    await expect(page.locator("body")).toBeVisible({ timeout: 30000 });

    // Document intentional gap: full configure→insert→DOM title remains
    // fixture-heavy; REST path above is the durable residual gate.
    expect(item.id).toBeTruthy();
  });
});

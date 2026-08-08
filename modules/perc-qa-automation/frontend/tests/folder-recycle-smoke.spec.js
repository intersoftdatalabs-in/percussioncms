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
 * Folder + recycle REST smoke after folderHelper cycle break
 * (#2464 / parent #2423 residual; related #2437 login/health).
 *
 * <p>Proves pathmanagement + recycleService still work end-to-end on a healthy
 * H2 QA stack: create folder under Assets → recycle (soft-delete) →
 * restore when guid available else purge via empty Recycling.</p>
 *
 * <p><strong>Hard fail</strong> when Rhythmyx context / pathmanagement is down
 * (the pre-fix class of bug: Jetty connector up, Spring cycle dead). Do not
 * soft-skip that failure.</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/folder-recycle-smoke.spec.js
 *   # tags:
 *   npm run test:surface -- --tag folder-recycle
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p>List only (no live CMS):
 * {@code npm run test:surface:list -- --path tests/folder-recycle-smoke.spec.js}
 * or {@code --tag folder-recycle}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  probePathmanagementContext,
  createNamedFolder,
  recycleFolder,
  restoreFolderByGuid,
  findInRecycling,
  findNamedPathItem,
  listFolderChildren,
  emptyRecyclingViaApi,
  emptyApiFailureMessage,
  extractPathItemGuid,
  contextDownFailureMessage,
} = require("./helpers/folder-recycle-smoke");

test.describe("folder + recycle REST smoke @folder-recycle @smoke", () => {
  test("pathmanagement context is up (hard fail if Rhythmyx dead) @folder-recycle @smoke", async ({
    request,
  }) => {
    test.setTimeout(45_000);
    const headers = adminBasicAuthHeaders();
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);
    expect(probe.status).toBeGreaterThanOrEqual(200);
  });

  test("REST: create folder, recycle, restore or purge @folder-recycle @smoke", async ({
    request,
  }) => {
    test.setTimeout(120_000);
    const headers = adminBasicAuthHeaders();

    // Hard fail first — never soft-skip a dead context as "folder missing".
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);

    const created = await createNamedFolder(request, BASE_URL, headers, {
      parentPath: "Assets",
    });
    expect(created.name).toBeTruthy();
    expect(created.path).toMatch(/Assets/i);

    const underAssets = await listFolderChildren(
      request,
      BASE_URL,
      headers,
      "Assets",
    );
    expect(
      findNamedPathItem(underAssets, created.name),
      `expected live folder ${created.name} under Assets after create; got ${JSON.stringify(underAssets).slice(0, 400)}`,
    ).toBeTruthy();

    await recycleFolder(request, BASE_URL, headers, {
      path: created.path,
      guid: created.guid,
    });

    // Live tree should no longer list the folder (soft-deleted into Recycling).
    const assetsAfter = await listFolderChildren(
      request,
      BASE_URL,
      headers,
      "Assets",
    );
    expect(
      findNamedPathItem(assetsAfter, created.name),
      `folder ${created.name} should leave Assets after recycle`,
    ).toBeNull();

    const recycled = await findInRecycling(
      request,
      BASE_URL,
      headers,
      created.name,
    );
    expect(
      recycled.found,
      `expected ${created.name} under Recycling after soft-delete; location scan failed`,
    ).toBe(true);

    // Prefer restore when we have a guid (recycleService.restoreFolder path).
    const restoreGuid =
      extractPathItemGuid(recycled.item) || created.guid || "";
    if (restoreGuid) {
      const restored = await restoreFolderByGuid(
        request,
        BASE_URL,
        headers,
        restoreGuid,
      );
      expect(
        restored.status >= 200 && restored.status < 300,
        `restoreFolder failed HTTP ${restored.status}: ${(restored.text || "").slice(0, 300)}`,
      ).toBe(true);

      // After restore, folder should leave Recycling (may reappear under Assets).
      const stillInBin = await findInRecycling(
        request,
        BASE_URL,
        headers,
        created.name,
      );
      // Best-effort: if restore succeeded, recycle listing should not keep it.
      // Some installs rename on restore conflict — accept either not-in-bin
      // or present-under-Assets under original or restored name.
      const assetsRestored = await listFolderChildren(
        request,
        BASE_URL,
        headers,
        "Assets",
      );
      const liveAgain =
        findNamedPathItem(assetsRestored, created.name) ||
        stillInBin.found === false;
      expect(
        liveAgain,
        `after restore guid=${restoreGuid}: expected not-in-Recycling or under Assets; stillInBin=${stillInBin.found}`,
      ).toBeTruthy();

      // Cleanup: soft-delete again then empty so we do not leave fixtures.
      const liveItem = findNamedPathItem(assetsRestored, created.name);
      if (liveItem) {
        await recycleFolder(request, BASE_URL, headers, {
          path: String(liveItem.path || created.path),
          guid: extractPathItemGuid(liveItem),
        }).catch(() => {});
      }
      const emptied = await emptyRecyclingViaApi(request, BASE_URL, headers);
      expect(
        emptied.status >= 200 && emptied.status < 300,
        emptyApiFailureMessage(emptied),
      ).toBe(true);
    } else {
      // No guid available — product still allows purge via empty Recycling.
      const emptied = await emptyRecyclingViaApi(request, BASE_URL, headers);
      expect(
        emptied.status >= 200 && emptied.status < 300,
        emptyApiFailureMessage(emptied),
      ).toBe(true);
      const afterPurge = await findInRecycling(
        request,
        BASE_URL,
        headers,
        created.name,
      );
      expect(
        afterPurge.found,
        `seed ${created.name} should be gone after empty purge`,
      ).toBe(false);
    }
  });

  test("Admin login still works when context is healthy @folder-recycle @smoke", async ({
    page,
    request,
  }) => {
    test.setTimeout(90_000);
    const headers = adminBasicAuthHeaders();
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);

    await loginAsAdmin(page);
    const url = page.url();
    expect(url).not.toMatch(/\/Rhythmyx\/login(\?|$)/);
    expect(url).toMatch(/\/Rhythmyx\/|\/cm\//);
  });
});

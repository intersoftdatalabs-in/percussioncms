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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression: Bulk Upload HTTP 405 (GH-1812).
 *
 * <p>Client {@code resolveAssetUploadUrl()} POSTs the exact path
 * {@code /cm/uploadAssetFile} (no pathInfo). WebUI {@code web.xml} must
 * declare that exact {@code url-pattern} (prefix {@code /*} alone does not
 * match on servlet containers). Selecting a file in the Bulk Upload gadget
 * must not yield HTTP 405 Method Not Allowed.</p>
 *
 * <p>Requires a live CMS with the modern Home gadgets section. Playwright
 * could not be executed in the agent session if no install is configured —
 * the spec is still required companion coverage per WebUI AGENTS.md.</p>
 */

const path = require("path");
const fs = require("fs");
const os = require("os");
const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

/**
 * Ensure the Bulk Upload gadget is on the dashboard (not in default layout).
 * @param {import('@playwright/test').Page} page
 */
async function ensureBulkUploadGadget(page) {
  const widget = page.getByTestId("bulk-upload-widget");
  if (await widget.isVisible().catch(() => false)) {
    return;
  }

  await page.getByTestId("dashboard-add-gadget").click();
  // Search filter in Add Gadget modal (no dedicated testid on the input).
  const search = page.locator("input[placeholder]").first();
  await search.fill("Bulk Upload");
  // Click the Add button next to the Bulk Upload catalog entry.
  const row = page
    .locator("div")
    .filter({ hasText: /^Bulk Upload/ })
    .first();
  await expect(row).toBeVisible({ timeout: 10_000 });
  await row.getByRole("button", { name: /add/i }).click();
  await expect(widget).toBeVisible({ timeout: 15_000 });
}

test.describe("Bulk Upload exact mapping (GH-1812)", () => {
  test("file select POSTs exact /uploadAssetFile and is not HTTP 405", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    // Modern Home hosts the gadgets section (PR-7).
    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/home`);
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("home-gadgets-section")).toBeVisible({
      timeout: 30_000,
    });

    await ensureBulkUploadGadget(page);

    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "perc-bulk-upload-"));
    const samplePath = path.join(tmpDir, "gh-1812-sample.txt");
    fs.writeFileSync(
      samplePath,
      "GH-1812 bulk upload regression sample\n",
      "utf8",
    );

    const uploadResponsePromise = page.waitForResponse(
      (res) => {
        try {
          const u = new URL(res.url());
          return (
            u.pathname.endsWith("/uploadAssetFile") &&
            res.request().method() === "POST"
          );
        } catch {
          return false;
        }
      },
      { timeout: 45_000 },
    );

    await page.getByTestId("bulk-upload-files").setInputFiles(samplePath);

    const response = await uploadResponsePromise;
    // Primary regression assertion: exact-path mapping must not 405.
    expect(
      response.status(),
      `POST ${response.url()} returned ${response.status()} (body excerpt)`,
    ).not.toBe(405);

    // Success path shows results; failure path must not be "HTTP 405".
    const results = page.getByTestId("bulk-upload-results");
    const errorBox = page.getByTestId("bulk-upload-error");
    await expect
      .poll(
        async () => {
          if (await results.isVisible().catch(() => false)) return "results";
          if (await errorBox.isVisible().catch(() => false)) return "error";
          return "pending";
        },
        { timeout: 30_000 },
      )
      .not.toBe("pending");

    if (await results.isVisible().catch(() => false)) {
      await expect(results).toContainText(/succeeded|✓|✕/i);
      await expect(results).not.toContainText("HTTP 405");
    } else {
      const errText = await errorBox.innerText();
      expect(errText).not.toMatch(/HTTP\s*405/i);
    }

    try {
      fs.unlinkSync(samplePath);
      fs.rmdirSync(tmpDir);
    } catch {
      /* best-effort temp cleanup */
    }
  });
});

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
 * PublishingShell set the open site default workflow (#5061 / parent #4531).
 * GET /services/sites omits folderRoot even for QA-image sites (#5075).
 * The spec opens Corporate_Investments or Enterprise_Investments when the
 * image seeded them, and otherwise creates a site plus a //Sites folder.
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/siteDefaultWorkflow.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("../helpers/auth");
const {
  createdFolderPath,
  seededSiteName,
} = require("../helpers/site-default-workflow-fixture");

/**
 * Use a QA-image site when one is present. Otherwise create a site and a real
 * //Sites folder (content-explorer). Finder /folders/create rejects that root.
 * GET /services/sites omits folderRoot, so that field is not a signal (#5075).
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {string} siteName
 * @returns {Promise<string>}
 */
async function ensureSiteWithFolder(request, baseUrl, headers, siteName) {
  const listed = await request.get(`${baseUrl}/Rhythmyx/services/sites`, { headers });
  expect(listed.ok(), `sites HTTP ${listed.status()}`).toBeTruthy();
  const seeded = seededSiteName(await listed.json());
  if (seeded) {
    return seeded;
  }

  const created = await request.post(`${baseUrl}/Rhythmyx/services/sites`, {
    headers,
    data: { Site: { name: siteName } },
  });
  expect(created.status(), await created.text()).toBe(200);

  const folder = await request.post(
    `${baseUrl}/Rhythmyx/services/content-explorer/folders`,
    {
      headers,
      data: { AddFolderRequest: { name: siteName, parentPath: "//Sites" } },
    },
  );
  const folderBody = await folder.text();
  expect(folder.status(), folderBody.slice(0, 300)).toBe(200);
  const folderPath = createdFolderPath(JSON.parse(folderBody)) || `//Sites/${siteName}`;

  const rooted = await request.put(
    `${baseUrl}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}`,
    {
      headers,
      data: { Site: { name: siteName, folderRoot: folderPath } },
    },
  );
  expect(rooted.status(), await rooted.text()).toBe(200);
  return siteName;
}

test.describe("PublishingShell set the open site default workflow", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("save shows the catalog workflow after refresh; cancel, empty, and 409 do not", async ({
    page,
  }) => {
    test.setTimeout(90000);
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (
        text.includes("status of 400") ||
        text.includes("status of 403") ||
        text.includes("status of 409")
      ) {
        return;
      }
      jsErrors.push(text);
    });

    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
      "Content-Type": "application/json",
    };
    const stamp = Date.now().toString().slice(-6);
    const next = `NightWf${stamp}`;
    let siteName = `WfSite${stamp}`;
    const createdWf = await page.request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name: next, description: "Publishing shell slice 5061" } },
    });
    expect(createdWf.status(), await createdWf.text()).toBe(200);
    siteName = await ensureSiteWithFolder(page.request, BASE_URL, headers, siteName);

    let putCalls = 0;
    page.on("request", (req) => {
      const path = new URL(req.url()).pathname;
      if (req.method() === "PUT" && /\/sites\/[^/]+$/.test(path)) {
        putCalls += 1;
      }
    });

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`);
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId(`publish-site-card-${siteName}`).click();
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-site-default-workflow")).toBeVisible();

    const putsBeforeCancel = putCalls;
    await page.getByTestId("publish-site-default-workflow-edit").click();
    await page.getByTestId("publish-site-default-workflow-choice").selectOption(next);
    await page.getByTestId("publish-site-default-workflow-cancel").click();
    await expect(page.getByTestId("publish-site-default-workflow-form")).toHaveCount(0);
    expect(putCalls).toBe(putsBeforeCancel);

    await page.getByTestId("publish-site-default-workflow-edit").click();
    await page.getByTestId("publish-site-default-workflow-choice").selectOption("");
    await page.getByTestId("publish-site-default-workflow-save").click();
    await expect(page.getByTestId("publish-site-default-workflow-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-default-workflow-saved")).toHaveCount(0);
    expect(putCalls).toBe(putsBeforeCancel);

    await page.route("**/services/sites/**", async (route) => {
      if (route.request().method() !== "PUT") {
        return route.continue();
      }
      return route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "conflict" }),
      });
    });
    await page.getByTestId("publish-site-default-workflow-choice").selectOption(next);
    await page.getByTestId("publish-site-default-workflow-save").click();
    await expect(page.getByTestId("publish-site-default-workflow-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-default-workflow-form")).toBeVisible();
    await expect(page.getByTestId("publish-site-default-workflow-saved")).toHaveCount(0);
    await expect(page.getByTestId("publish-site-default-workflow")).not.toHaveText(next);
    await page.unrouteAll({ behavior: "ignoreErrors" });

    await page.getByTestId("publish-site-default-workflow-choice").selectOption(next);
    const putResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "PUT" &&
        /\/sites\/[^/]+$/.test(new URL(res.url()).pathname),
      { timeout: 20000 },
    );
    await page.getByTestId("publish-site-default-workflow-save").click();
    const saved = await putResponse;
    if (saved.status() !== 200) {
      throw new Error(
        `update HTTP ${saved.status()} ${saved.url()} ${(await saved.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByTestId("publish-site-default-workflow")).toHaveText(next);
    await expect(page.getByTestId("publish-site-default-workflow-saved")).toBeVisible();

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 20000,
    });
    await page.getByTestId(`publish-site-card-${siteName}`).click();
    await expect(page.getByTestId("publish-site-default-workflow")).toHaveText(next, {
      timeout: 20000,
    });

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});

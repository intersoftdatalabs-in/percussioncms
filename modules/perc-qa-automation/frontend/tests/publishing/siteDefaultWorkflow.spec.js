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
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/siteDefaultWorkflow.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function workflowNames(payload) {
  const rows = [];
  const visit = (node) => {
    if (!node) {
      return;
    }
    if (Array.isArray(node)) {
      node.forEach(visit);
      return;
    }
    if (typeof node !== "object") {
      return;
    }
    if (typeof node.workflowName === "string" && node.workflowName.trim()) {
      rows.push(node.workflowName.trim());
    }
    Object.values(node).forEach(visit);
  };
  visit(payload);
  return [...new Set(rows)];
}

function siteRows(payload) {
  const rows = [];
  const visit = (node) => {
    if (!node) {
      return;
    }
    if (Array.isArray(node)) {
      node.forEach(visit);
      return;
    }
    if (typeof node !== "object") {
      return;
    }
    if (typeof node.name === "string" && (node.folderRoot || node.workflowName || node.baseUrl)) {
      rows.push(node);
      return;
    }
    Object.values(node).forEach(visit);
  };
  visit(payload);
  return rows;
}

test.describe("PublishingShell set the open site default workflow", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("save shows the catalog workflow after refresh; cancel, empty, and 409 do not", async ({
    page,
  }) => {
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

    const wfRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/workflowmanagement/workflows/metadata`,
    );
    expect(wfRes.ok(), `workflow catalog HTTP ${wfRes.status()}`).toBeTruthy();
    const catalog = workflowNames(await wfRes.json());
    expect(catalog.length, "H2 workflow catalog").toBeGreaterThan(0);

    const sitesRes = await page.request.get(`${BASE_URL}/Rhythmyx/services/sites`);
    expect(sitesRes.ok(), `sites HTTP ${sitesRes.status()}`).toBeTruthy();
    const sites = siteRows(await sitesRes.json()).filter(
      (site) => typeof site.folderRoot === "string" && site.folderRoot.trim(),
    );
    const target = sites.find((site) =>
      catalog.some(
        (name) => name.toLowerCase() !== String(site.workflowName || "").trim().toLowerCase(),
      ),
    );
    expect(target, "sample site with a folder and an alternate workflow").toBeTruthy();
    const siteName = target.name;
    const next = catalog.find(
      (name) => name.toLowerCase() !== String(target.workflowName || "").trim().toLowerCase(),
    );

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

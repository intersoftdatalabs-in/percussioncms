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
 * PublishingShell Runtime — open a running edition job on Status (#4838 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/runtimeOpenJob.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell Runtime open running job", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("Open job switches to Status detail; idle editions have no control", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });

    await page.route(
      "**/services/sitemanage/publishingdesign/runtime/editions?**",
      async (route) => {
        if (route.request().method() !== "GET") {
          return route.continue();
        }
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([
            {
              editionId: "10",
              name: "IdleEdition",
              runningJobId: 0,
              pubServerId: "7",
            },
            {
              editionId: "11",
              name: "RunningEdition",
              runningJobId: 88,
              jobStatus: "Running",
              pubServerId: "7",
            },
          ]),
        });
      },
    );
    await page.route("**/services/publishmanagement/servers/**", async (route) => {
      if (route.request().method() !== "GET") {
        return route.continue();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([{ serverId: "7", serverName: "LocalFS" }]),
      });
    });
    await page.route("**/sitemanage/pubstatus/details**", async (route) => {
      if (route.request().method() === "POST") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ SitePublishItem: [] }),
        });
        return;
      }
      return route.continue();
    });
    await page.route("**/services/sitemanage/pubstatus/current**", async (route) => {
      if (route.request().method() !== "GET") {
        return route.continue();
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            jobId: 88,
            siteName: "H2 Site",
            editionName: "RunningEdition",
            status: "Running",
            completedItems: 1,
            totalItems: 4,
          },
        ]),
      });
    });

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=runtime`,
    );
    await expect(page.getByTestId("publish-section-runtime")).toBeVisible({
      timeout: 30000,
    });
    const openKey = "perc.ui.publish.sections.runtime@Open job";
    const resolved = await page.evaluate(
      (key) => window.I18N && window.I18N.message(key),
      openKey,
    );
    expect(resolved, "en-us catalog must resolve Open job").toBe("Open job");
    await expect(page.getByTestId("runtime-start-10")).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByTestId("runtime-open-job-10")).toHaveCount(0);
    await expect(page.getByTestId("runtime-open-job-11")).toBeVisible();
    await page.getByTestId("runtime-open-job-11").click();
    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByTestId("publish-status-job-detail")).toBeVisible();
    await expect(page.getByTestId("publish-status-detail-job-id")).toHaveText(
      "88",
    );
    await expect(page.getByTestId("publish-status-detail-status")).toHaveText(
      "Running",
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});

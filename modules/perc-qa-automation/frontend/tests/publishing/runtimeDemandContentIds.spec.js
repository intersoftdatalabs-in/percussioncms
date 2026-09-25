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
 * PublishingShell Runtime — demand-publish content ids (#4858 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/runtimeDemandContentIds.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function trackJsErrors(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      jsErrors.push(msg.text());
    }
  });
  return jsErrors;
}

async function stubRuntimeLists(page) {
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
            name: "H2Full",
            runningJobId: 0,
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
}

test.describe("PublishingShell Runtime demand content ids", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("posts parsed content ids for the selected edition", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await stubRuntimeLists(page);
    let demandBody = null;
    let demandUrl = "";
    await page.route(
      "**/services/sitemanage/publishingdesign/runtime/editions/*/demand",
      async (route) => {
        demandUrl = route.request().url();
        demandBody = route.request().postDataJSON();
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            editionId: "10",
            requestId: 44,
            status: "queued",
          }),
        });
      },
    );

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=runtime`,
    );
    await expect(page.getByTestId("publish-section-runtime")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("runtime-demand-submit")).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByTestId("publish-section-runtime")).toContainText(
      "Selected edition: 10",
    );
    await page.getByTestId("runtime-demand-ids").fill("101, 102");
    await page.getByTestId("runtime-demand-submit").click();
    await expect(page.getByTestId("runtime-job-status")).toContainText(/queued/i);
    await expect(page.getByTestId("runtime-job-status")).toContainText(/44/);
    expect(demandUrl).toMatch(/\/editions\/10\/demand$/);
    expect(demandBody).toEqual({ contentIds: ["101", "102"] });
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("empty content ids do not call demand and HTTP errors stay on Runtime", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await stubRuntimeLists(page);
    let demandCalls = 0;
    await page.route(
      "**/services/sitemanage/publishingdesign/runtime/editions/*/demand",
      async (route) => {
        demandCalls += 1;
        return route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({
            message: "folderId required for contentId 101",
          }),
        });
      },
    );

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=runtime`,
    );
    await expect(page.getByTestId("publish-section-runtime")).toContainText(
      "Selected edition: 10",
      { timeout: 30000 },
    );
    await page.getByTestId("runtime-demand-submit").click();
    await expect(page.getByRole("alert")).toContainText(/at least one content id/i);
    expect(demandCalls).toBe(0);

    await page.getByTestId("runtime-demand-ids").fill("101");
    await page.getByTestId("runtime-demand-submit").click();
    const alert = page.getByRole("alert");
    await expect(alert).toContainText(/folderId required for contentId 101/);
    await expect(page.getByTestId("publish-section-runtime")).toContainText(
      /folderId required for contentId 101/,
    );
    await expect(page.getByTestId("publish-section-status")).toHaveCount(0);
    expect(demandCalls).toBe(1);
    const unexpected = jsErrors.filter(
      (line) => !/Failed to load resource: the server responded with a status of 400/.test(line),
    );
    expect(
      unexpected,
      `console/page errors: ${unexpected.join("\n")}`,
    ).toEqual([]);
  });
});

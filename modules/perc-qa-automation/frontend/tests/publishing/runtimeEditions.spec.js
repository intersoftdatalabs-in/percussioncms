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
 * PublishingShell Runtime — start/stop selected pub-server edition (#4648 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/runtimeEditions.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell Runtime start/stop", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("opens Runtime with pub-server selector and start control", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=runtime`,
    );
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-section-runtime")).toBeVisible();
    await expect(page.getByTestId("runtime-pub-server")).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("start then stop shows status feedback", async ({ page }) => {
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
    await page.route(
      "**/services/sitemanage/publishingdesign/runtime/editions/10/start",
      async (route) => {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ jobId: 88, editionId: "10", status: "started" }),
        });
      },
    );
    await page.route(
      "**/services/sitemanage/publishingdesign/runtime/jobs/88/stop",
      async (route) => {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ jobId: 88, status: "cancelled" }),
        });
      },
    );

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=runtime`,
    );
    await expect(page.getByTestId("publish-section-runtime")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("runtime-start-10")).toBeVisible({
      timeout: 20000,
    });
    await page.getByTestId("runtime-start-10").click();
    await expect(page.getByTestId("runtime-job-status")).toContainText(
      /started/i,
    );

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
              runningJobId: 88,
              jobStatus: "Running",
              pubServerId: "7",
            },
          ]),
        });
      },
    );
    await page.getByRole("button", { name: "Refresh" }).click();
    await expect(page.getByTestId("runtime-stop-10")).toBeVisible({
      timeout: 20000,
    });
    await page.getByTestId("runtime-stop-10").click();
    await expect(page.getByTestId("runtime-job-status")).toContainText(
      /cancelled/i,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});

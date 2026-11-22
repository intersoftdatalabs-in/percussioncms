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
 * Playwright surface: #6929 / parent #4531 — site-workspace Schedule panel
 * clear confirm keyboard operation (ITM-05 UX closure).
 *
 * The clear confirm is an inline `alertdialog`: initial focus lands on
 * Cancel, Tab/Shift+Tab cycle inside, Escape cancels without writing and
 * returns focus to the Clear scheduled dates button. Confirming posts empty
 * dates through the existing setitemdates route and reloads the empty
 * schedule.
 *
 * Tags: @publish-site-schedule-dates @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-site-schedule-dates.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { publishingSitesUrl } = require("./helpers/publish-sites-staged-status");
const {
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-deep-links");
const {
  expectNoSeriousA11yViolations,
} = require("./helpers/a11y");
const { proofScreenshot } = require("./helpers/proof-shot");

const TAGS = ["@publish-site-schedule-dates", "@publish", "@smoke"];

function collectConsoleErrors(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (text.startsWith("Failed to load resource:")) {
      return;
    }
    if (!isKnownPublishConsoleNoise(text)) {
      consoleErrors.push(text);
    }
  });
  return consoleErrors;
}

async function stubScheduleApis(page, store) {
  await page.route(
    "**/services/itemmanagement/item/getitemdates/**",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ItemDates: store.dates }),
      });
    },
  );
  await page.route(
    "**/services/itemmanagement/item/setitemdates**",
    async (route) => {
      store.posts.push(route.request().postData() || "");
      if (store.setStatus && store.setStatus !== 200) {
        await route.fulfill({
          status: store.setStatus,
          contentType: "application/json",
          body: JSON.stringify({ message: store.setMessage || "rejected" }),
        });
        return;
      }
      const raw = route.request().postData() || "{}";
      const parsed = JSON.parse(raw);
      const next = parsed.ItemDates || parsed;
      store.dates = {
        itemId: String(next.itemId || "42"),
        startDate: next.startDate || "",
        endDate: next.endDate || "",
        comments: next.comments || "",
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ operation: "Success" }),
      });
    },
  );
}

async function openSchedulePanel(page) {
  await page.goto(publishingSitesUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  const siteCard = page.locator('[data-testid^="publish-site-card-"]').first();
  await expect(siteCard).toBeVisible({ timeout: 30000 });
  await siteCard.click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
  const panel = page.getByTestId("item-schedule-dates");
  await expect(panel).toBeVisible({ timeout: 15000 });
  await panel.scrollIntoViewIfNeeded();
  return panel;
}

async function loadDates(panel, id) {
  await panel.getByTestId("item-schedule-id").fill(id);
  await panel.getByTestId("item-schedule-load").click();
}

test.describe("PublishingShell site-workspace schedule clear confirm (#6929)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`Escape cancels the clear confirm and returns focus ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    const store = {
      dates: {
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
        comments: "",
      },
      posts: [],
    };
    await stubScheduleApis(page, store);
    const panel = await openSchedulePanel(page);
    await loadDates(panel, "42");
    await expect(panel.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );

    await panel.getByTestId("item-schedule-clear-submit").click();
    const confirm = panel.getByTestId("item-schedule-clear-confirm");
    await expect(confirm).toBeVisible();
    await expect(confirm).toHaveAttribute("role", "alertdialog");
    // Destructive confirm: initial focus lands on Cancel, not Clear.
    await expect(panel.getByTestId("item-schedule-clear-cancel")).toBeFocused();
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="item-schedule-clear-confirm"]',
    });

    await page.keyboard.press("Escape");
    await expect(confirm).toHaveCount(0);
    // Focus returns to the opener; nothing was written.
    await expect(
      panel.getByTestId("item-schedule-clear-submit"),
    ).toBeFocused();
    expect(store.posts).toEqual([]);
    await expect(panel.getByTestId("item-schedule-error")).toHaveCount(0);
    await expect(panel.getByTestId("item-schedule-success")).toHaveCount(0);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`Tab cycles inside the confirm and Yes clears the schedule ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    const store = {
      dates: {
        itemId: "42",
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
        comments: "",
      },
      posts: [],
    };
    await stubScheduleApis(page, store);
    const panel = await openSchedulePanel(page);
    await loadDates(panel, "42");
    await expect(panel.getByTestId("item-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );

    await panel.getByTestId("item-schedule-clear-submit").click();
    const confirm = panel.getByTestId("item-schedule-clear-confirm");
    await expect(confirm).toBeVisible();
    const yes = panel.getByTestId("item-schedule-clear-yes");
    const cancel = panel.getByTestId("item-schedule-clear-cancel");
    await expect(cancel).toBeFocused();
    // Tab on the last control wraps to the first.
    await page.keyboard.press("Tab");
    await expect(yes).toBeFocused();
    // Shift+Tab on the first control wraps back to the last.
    await page.keyboard.press("Shift+Tab");
    await expect(cancel).toBeFocused();

    await proofScreenshot(confirm, "issue-6929-schedule-clear-confirm.png");
    await yes.click();
    await expect(panel.getByTestId("item-schedule-success")).toContainText(
      /cleared/i,
      { timeout: 15000 },
    );
    expect(store.posts.length).toBe(1);
    const posted = JSON.parse(store.posts[0]);
    expect(posted.ItemDates.startDate).toBe("");
    expect(posted.ItemDates.endDate).toBe("");
    await expect(confirm).toHaveCount(0);
    await expect(panel.getByTestId("item-schedule-start")).toHaveValue("");
    await expect(panel.getByTestId("item-schedule-end")).toHaveValue("");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});

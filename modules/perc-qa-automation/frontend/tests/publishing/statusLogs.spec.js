const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("Unified Publishing UI - Status and Logs (US2)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("verify Status and Logs tabs and purge button visibility", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=publish`);

    // Click Logs tab
    const logsTab = page.locator("a:has-text('Logs')");
    await expect(logsTab).toBeVisible();
    await logsTab.click();

    // Verify logs list table
    const logsTable = page.locator(".perc-publishing-logs-table");
    await expect(logsTable).toBeVisible();

    // Verify purge button
    const purgeButton = page.locator("button:has-text('Purge Logs')");
    await expect(purgeButton).toBeVisible();
  });
});

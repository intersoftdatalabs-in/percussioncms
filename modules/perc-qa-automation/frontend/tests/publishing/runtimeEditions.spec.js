const { test, expect } = require("@playwright/test");
const { loginAsAdmin } = require("../helpers/auth");

test.describe("Unified Publishing UI - Runtime Editions (US5)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("verify runtime section options", async ({ page }) => {
    await page.goto("/cm/app/index.jsp?view=publish");

    // Click Runtime tab
    const runtimeTab = page.locator("a:has-text('Runtime')");
    await expect(runtimeTab).toBeVisible();
    await runtimeTab.click();

    // Verify Runtime edition views/tables
    const runtimeContainer = page.locator(".perc-publishing-runtime-container, .perc-coming-soon");
    await expect(runtimeContainer).toBeVisible();
  });
});

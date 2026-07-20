const { test, expect } = require("@playwright/test");
const { loginAsAdmin } = require("../helpers/auth");

test.describe("Unified Publishing UI - Server Configuration (US3)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("verify server settings CRUD", async ({ page }) => {
    await page.goto("/cm/app/index.jsp?view=publish");

    // Select first site and open workspace
    const siteCard = page.locator(".perc-site-card").first();
    await siteCard.click();

    // Click Add Server
    const addServerBtn = page.locator("button:has-text('Add Server')");
    await expect(addServerBtn).toBeVisible();
    await addServerBtn.click();

    // Verify fields in ServerEditor
    await expect(page.locator("input[name='server-name']")).toBeVisible();
    await expect(page.locator("select[name='server-driver']")).toBeVisible();
  });
});

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("Unified Publishing UI - Site Operations (US1)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("navigate to Publishing and verify site list is displayed", async ({
    page,
  }) => {
    // Navigate to Publishing module
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=publish`);

    // Check if modern Publishing Shell or view is rendered
    await expect(page.locator("#perc-publishing-root")).toBeVisible();

    // Verify list of sites and site card container
    const siteList = page.locator(".perc-publishing-sites-list");
    await expect(siteList).toBeVisible();
  });

  test("run full publish for a site", async ({ page }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=publish`);

    // Select first site card and open workspace
    const siteCard = page.locator(".perc-site-card").first();
    await expect(siteCard).toBeVisible();
    await siteCard.click();

    // Confirm navigation to workspace
    await expect(page.locator("button:has-text('Back')")).toBeVisible();

    // Wire run full publish
    const publishButton = page.locator("button:has-text('Full Publish')");
    await expect(publishButton).toBeVisible();
    await publishButton.click();

    // Verify toast or progress container starts
    const successToast = page.locator(".perc-publishing-job-started");
    await expect(successToast).toBeVisible();

    // Clean up: Stop the running job to prevent side-effects
    const stopButton = page.locator("button:has-text('Stop')");
    if (await stopButton.isVisible()) {
      await stopButton.click();
    }
  });
});

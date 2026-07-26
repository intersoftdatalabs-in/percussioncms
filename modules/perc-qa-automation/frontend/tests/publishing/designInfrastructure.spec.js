const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("Unified Publishing UI - Design Infrastructure (US4)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("verify design navigation panel is present", async ({ page }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=publish`);

    // Click Design tab
    const designTab = page.locator("a:has-text('Design')");
    await expect(designTab).toBeVisible();
    await designTab.click();

    // Verify Design tree / explorer container is visible
    const designContainer = page.locator(
      ".perc-publishing-design-container, .perc-coming-soon"
    );
    await expect(designContainer).toBeVisible();
  });
});

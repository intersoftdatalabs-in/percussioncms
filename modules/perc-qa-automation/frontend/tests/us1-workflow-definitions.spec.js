const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Workflow Administration - Workflow Definitions (US1)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(
    "navigate to workflow admin and verify shell and list render",
    { tag: ["@workflow-admin", "@administration", "@smoke"] },
    async ({ page }) => {
      await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

      // Verify WorkflowAdminShell container is present
      const shell = page.locator("[data-testid='perc-workflow-admin-shell']");
      await expect(shell).toBeVisible({ timeout: 30_000 });

      // #2959: wrapper payload must not trip RouteErrorBoundary (e.map is not a function)
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
      await expect(
        page.getByText(/Unable to load Administration/i),
      ).toHaveCount(0);

      // Verify Workflow section table is visible
      const wfSection = page.locator("[data-testid='perc-workflow-section']");
      await expect(wfSection).toBeVisible({ timeout: 30_000 });

      // Verify tabs are present
      await expect(page.locator("[data-testid='tab-workflow']")).toBeVisible();
      await expect(page.locator("[data-testid='tab-roles']")).toBeVisible();
      await expect(page.locator("[data-testid='tab-users']")).toBeVisible();
      await expect(page.locator("[data-testid='tab-categories']")).toBeVisible();
    },
  );

  test("open create workflow form", async ({ page }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

    const createBtn = page.locator("[data-testid='create-workflow-button']");
    await expect(createBtn).toBeVisible();
    await createBtn.click();

    // Verify editor is displayed
    const editor = page.locator("[data-testid='perc-workflow-editor']");
    await expect(editor).toBeVisible();

    // Verify inputs
    await expect(
      page.locator("[data-testid='workflow-name-input']"),
    ).toBeVisible();
    await expect(page.locator("[data-testid='add-step-button']")).toBeVisible();
  });
});

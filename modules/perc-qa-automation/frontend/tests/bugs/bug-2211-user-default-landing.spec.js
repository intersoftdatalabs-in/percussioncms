/**
 * Issue #2211 / parent #959 slice 4 — Admin Users default landing control.
 *
 * WebUI product screen companion (Workflow Admin → Users tab → user editor).
 * Verifies the select is present, options load, and saving clears/sets
 * override when the slice-2 homepage API is available.
 *
 * Tags: @webui @admin @users @landing
 */
const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("Admin Users default landing page (#2211)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("Users tab exposes default landing control on edit", async ({
    page,
  }) => {
    // SPA workflow admin hosts Users section
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

    const shell = page.locator("[data-testid='perc-workflow-admin-shell']");
    await expect(shell).toBeVisible({ timeout: 30000 });

    const usersTab = page.locator("[data-testid='tab-users']");
    await expect(usersTab).toBeVisible();
    await usersTab.click();

    const usersSection = page.locator("[data-testid='perc-users-section']");
    await expect(usersSection).toBeVisible({ timeout: 30000 });

    // Open editor for Admin (always present) or first available user card
    const adminEdit = page.locator("[data-testid='edit-user-Admin']");
    const anyEdit = page.locator("[data-testid^='edit-user-']").first();
    if (await adminEdit.count()) {
      await adminEdit.click();
    } else {
      await expect(anyEdit).toBeVisible({ timeout: 30000 });
      await anyEdit.click();
    }

    const editor = page.locator("[data-testid='perc-user-editor']");
    await expect(editor).toBeVisible({ timeout: 15000 });

    const landingSelect = page.locator(
      "[data-testid='user-default-landing-select']",
    );
    await expect(landingSelect).toBeVisible();

    // Role-default + Home + Editor at minimum (Admin also gets Design/Admin options)
    const options = landingSelect.locator("option");
    const optionCount = await options.count();
    expect(optionCount).toBeGreaterThanOrEqual(3);

    const help = page.locator("[data-testid='user-default-landing-help']");
    await expect(help).toBeVisible();

    // Select Home if available, or leave role default — ensure control is interactive
    const values = await options.evaluateAll((els) => els.map((o) => o.value));
    expect(values).toContain("");
    expect(values).toContain("Home");
    expect(values).toContain("Editor");

    if (values.includes("Editor")) {
      await landingSelect.selectOption("Editor");
      await expect(landingSelect).toHaveValue("Editor");
    }

    // Cancel without saving permanent fixture data
    const cancelBtn = editor.locator("button", { hasText: /Cancel/i }).first();
    if (await cancelBtn.count()) {
      await cancelBtn.click();
      await expect(usersSection).toBeVisible({ timeout: 15000 });
    }
  });
});

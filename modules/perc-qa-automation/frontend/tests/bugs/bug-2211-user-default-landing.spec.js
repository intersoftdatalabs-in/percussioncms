/**
 * Issue #2211 / parent #959 slice 4 — Admin Users default landing control.
 *
 * WebUI product screen companion (Admin → Users tab → user editor).
 * Verifies the select is present, options load, and saving clears/sets
 * override when the slice-2 homepage API is available.
 *
 * #3088: Users live under unified AdminShell (not sibling Workflow shell).
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
    // SPA Admin hosts Users section (legacy view=workflow redirects here)
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

    const shell = page.locator("[data-testid='perc-admin-shell']");
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

    // Remaining-app list (#3537) + role gates (#3538): role-default + Home +
    // Explorer; Editor/Design not new. Admin role checkbox proves picker filter.
    const options = landingSelect.locator("option");
    const optionCount = await options.count();
    expect(optionCount).toBeGreaterThanOrEqual(3);

    const help = page.locator("[data-testid='user-default-landing-help']");
    await expect(help).toBeVisible();

    const values = await options.evaluateAll((els) =>
      els.map((o) => o.value),
    );
    expect(values).toContain("");
    expect(values).toContain("Home");
    expect(values).toContain("Explorer");
    const current = await landingSelect.inputValue();
    if (current !== "Editor") {
      expect(values).not.toContain("Editor");
    }
    if (current !== "Designer") {
      expect(values).not.toContain("Designer");
    }

    // Role list from USER_FIND can be empty on this stack; toggle Admin to
    // prove the picker follows assigned-role gates (#3538).
    const adminRole = editor.getByRole("checkbox", { name: /^Admin$/i });
    await expect(adminRole).toBeVisible();
    if (!(await adminRole.isChecked())) {
      await adminRole.check();
    }
    await expect(landingSelect.locator('option[value="Workflow"]')).toHaveCount(
      1,
    );
    await expect(landingSelect.locator('option[value="Developer"]')).toHaveCount(
      1,
    );

    if ((await landingSelect.locator('option[value="Explorer"]').count()) > 0) {
      await landingSelect.selectOption("Explorer");
      await expect(landingSelect).toHaveValue("Explorer");
    }

    // Cancel without saving permanent fixture data
    const cancelBtn = editor.locator("button", { hasText: /Cancel/i }).first();
    if (await cancelBtn.count()) {
      await cancelBtn.click();
      await expect(usersSection).toBeVisible({ timeout: 15000 });
    }
  });

  test("Contributor user editor does not list Admin as a new landing (#3538)", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

    const shell = page.locator("[data-testid='perc-admin-shell']");
    await expect(shell).toBeVisible({ timeout: 30000 });
    await page.locator("[data-testid='tab-users']").click();

    const usersSection = page.locator("[data-testid='perc-users-section']");
    await expect(usersSection).toBeVisible({ timeout: 30000 });

    const contributorEdit = page.locator(
      "[data-testid='edit-user-Contributor']",
    );
    test.skip(
      (await contributorEdit.count()) === 0,
      "Contributor user not present on this stack",
    );
    await contributorEdit.click();

    const editor = page.locator("[data-testid='perc-user-editor']");
    await expect(editor).toBeVisible({ timeout: 15000 });
    const landingSelect = page.locator(
      "[data-testid='user-default-landing-select']",
    );
    await expect(landingSelect).toBeVisible();
    const values = await landingSelect.locator("option").evaluateAll((els) =>
      els.map((o) => o.value),
    );
    expect(values).toContain("Home");
    expect(values).toContain("Explorer");
    const current = await landingSelect.inputValue();
    if (current !== "Workflow") {
      expect(values).not.toContain("Workflow");
    }
    if (current !== "Developer") {
      expect(values).not.toContain("Developer");
    }
    if (current !== "Publish") {
      expect(values).not.toContain("Publish");
    }

    const cancelBtn = editor.locator("button", { hasText: /Cancel/i }).first();
    if (await cancelBtn.count()) {
      await cancelBtn.click();
    }
  });
});

test.describe("Admin Roles default homepage (#3537)", () => {
  test("Roles editor lists remaining top-nav apps, not Editor/Design", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=admin&tab=roles&_=${Date.now()}`,
      { waitUntil: "domcontentloaded" },
    );

    const shell = page.locator("[data-testid='perc-admin-shell']");
    await expect(shell).toBeVisible({ timeout: 30000 });

    const rolesTab = page.locator("[data-testid='tab-roles']");
    await expect(rolesTab).toBeVisible();
    if ((await rolesTab.getAttribute("aria-selected")) !== "true") {
      await rolesTab.click();
    }

    const rolesSection = page.locator("[data-testid='perc-roles-section']");
    await expect(rolesSection).toBeVisible({ timeout: 30000 });

    const adminEdit = page.locator("[data-testid='edit-role-Admin']");
    const anyEdit = page.locator("[data-testid^='edit-role-']").first();
    if (await adminEdit.count()) {
      await adminEdit.click();
    } else {
      await expect(anyEdit).toBeVisible({ timeout: 30000 });
      await anyEdit.click();
    }

    const editor = page.locator("[data-testid='perc-role-editor']");
    await expect(editor).toBeVisible({ timeout: 15000 });

    const homepageSelect = page.locator(
      "[data-testid='role-default-homepage-select']",
    );
    await expect(homepageSelect).toBeVisible();
    await expect(
      page.locator("[data-testid='role-default-homepage-help']"),
    ).toBeVisible();

    const values = await homepageSelect.locator("option").evaluateAll((els) =>
      els.map((o) => o.value),
    );
    expect(values).toContain("Home");
    expect(values).toContain("Explorer");
    expect(values).toContain("Architecture");
    expect(values).toContain("Developer");
    expect(values).toContain("Publish");
    expect(values).toContain("Workflow");
    const current = await homepageSelect.inputValue();
    if (current !== "Editor") {
      expect(values).not.toContain("Editor");
    }
    if (current !== "Designer") {
      expect(values).not.toContain("Designer");
    }

    const cancelBtn = page.locator("[data-testid='cancel-role-button']");
    await cancelBtn.click();
    await expect(rolesSection).toBeVisible({ timeout: 15000 });
    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  });
});

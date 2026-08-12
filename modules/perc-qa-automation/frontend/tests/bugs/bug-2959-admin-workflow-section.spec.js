/**
 * Regression: Admin → Workflow fails with TypeError e.map is not a function
 * (#2959 / residual #3202).
 *
 * GET workflowmanagement/workflows/metadata often returns a Jackson root wrapper
 * ({ Workflow: [...] } or nested { Workflow: { Workflow: [...] } }).
 * Workflow / Roles / Users / Categories must unwrap before .map so the
 * Admin shell loads without RouteErrorBoundary.
 *
 * #3088: Workflow admin lives under unified AdminShell (not sibling Workflow shell).
 *
 * Tags: @workflow-admin @administration @smoke @bug-2959 @bug-3202
 *
 * Surface filter:
 *   npm run test:surface -- --path tests/bugs/bug-2959-admin-workflow-section.spec.js
 *   npm run test:surface -- --tag bug-3202
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("Administration WorkflowSection load (#2959)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "perc-workflow-section loads without RouteErrorBoundary",
    {
      tag: ["@workflow-admin", "@administration", "@smoke", "@bug-2959"],
    },
    async ({ page }) => {
      // Legacy view=workflow redirects into unified Admin workflow tab (#3088)
      await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

      const shell = page.locator("[data-testid='perc-admin-shell']");
      await expect(shell).toBeVisible({ timeout: 30_000 });

      // Must not crash into route error boundary
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
      await expect(
        page.getByText(/Unable to load Admin/i),
      ).toHaveCount(0);

      const wfSection = page.locator("[data-testid='perc-workflow-section']");
      await expect(wfSection).toBeVisible({ timeout: 30_000 });
      await expect(
        page.locator("[data-testid='create-workflow-button']"),
      ).toBeVisible();
      await expect(page.locator("[data-testid='tab-workflow']")).toBeVisible();
      await expect(
        page.locator("[data-testid='tab-workflow']"),
      ).toHaveAttribute("aria-selected", "true");
    },
  );

  test(
    "Admin workflow tab reaches workflow section (no sibling shell)",
    {
      tag: ["@workflow-admin", "@administration", "@bug-2959"],
    },
    async ({ page }) => {
      await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);

      const adminShell = page.locator("[data-testid='perc-admin-shell']");
      await expect(adminShell).toBeVisible({ timeout: 30_000 });

      // Sibling cross-link removed (#3088); use in-shell Workflow tab
      await expect(
        page.getByTestId("admin-sibling-workflow-link"),
      ).toHaveCount(0);

      await page.locator("[data-testid='tab-workflow']").click();
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
      await expect(
        page.locator("[data-testid='perc-workflow-section']"),
      ).toBeVisible({ timeout: 30_000 });
    },
  );

  test(
    "Administration tabs render without RouteErrorBoundary or console TypeError (#3202)",
    {
      tag: ["@workflow-admin", "@administration", "@bug-3202", "@smoke"],
    },
    async ({ page }) => {
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        consoleErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);
      await expect(page.locator("[data-testid='perc-admin-shell']")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);

      const tabs = [
        ["tab-workflow", "perc-workflow-section"],
        ["tab-roles", "perc-roles-section"],
        ["tab-users", "perc-users-section"],
        ["tab-categories", "perc-categories-section"],
        ["tab-tools", "perc-tools-section"],
      ];
      for (const [tabId, sectionId] of tabs) {
        await page.locator(`[data-testid='${tabId}']`).click();
        await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
        await expect(page.getByText(/Unable to load Admin/i)).toHaveCount(0);
        await expect(page.locator(`[data-testid='${sectionId}']`)).toBeVisible({
          timeout: 30_000,
        });
      }

      const mapErrors = consoleErrors.filter((m) =>
        /map is not a function|Route load\/render failed/i.test(m),
      );
      expect(mapErrors, mapErrors.join("\n")).toEqual([]);
    },
  );
});

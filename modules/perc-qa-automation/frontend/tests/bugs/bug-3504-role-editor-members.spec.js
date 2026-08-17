/**
 * Regression: Admin → Roles editor only allowed one member (#3504).
 *
 * After adding the first user, Available Users must still list remaining
 * active users. Remove must return that user to Available. Membership is
 * local until Save — this spec Cancels so it does not persist a role.
 *
 * Tags: @workflow-admin @administration @roles @bug-3504
 *
 * Surface filter:
 *   npm run test:surface -- --path tests/bugs/bug-3504-role-editor-members.spec.js
 *   npm run test:surface -- --tag bug-3504
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function adminRolesEntry() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=admin&tab=roles&_=${Date.now()}`;
}

test.describe("Admin Role editor membership (#3504)", () => {
  test(
    "can add two users and remove one back to Available",
    {
      tag: [
        "@workflow-admin",
        "@administration",
        "@roles",
        "@bug-3504",
      ],
    },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(adminRolesEntry(), { waitUntil: "domcontentloaded" });

      const shell = page.getByTestId("perc-admin-shell");
      await expect(shell).toBeVisible({ timeout: 30_000 });
      await expect(page.getByTestId("route-error")).toHaveCount(0);

      const rolesTab = page.getByTestId("tab-roles");
      await expect(rolesTab).toBeVisible();
      if ((await rolesTab.getAttribute("aria-selected")) !== "true") {
        await rolesTab.click();
      }
      await expect(page.getByTestId("perc-roles-section")).toBeVisible({
        timeout: 30_000,
      });

      await page.getByTestId("create-role-button").click();
      const editor = page.getByTestId("perc-role-editor");
      await expect(editor).toBeVisible({ timeout: 15_000 });

      const availableList = page.getByTestId("available-users-list");
      await expect(availableList).toBeVisible();

      const addButtons = availableList.locator("button[data-testid^='add-user-']");
      await expect(addButtons.first()).toBeVisible({ timeout: 15_000 });
      const initialAvailable = await addButtons.count();
      expect(initialAvailable).toBeGreaterThanOrEqual(2);

      const firstTestId = await addButtons.nth(0).getAttribute("data-testid");
      const secondTestId = await addButtons.nth(1).getAttribute("data-testid");
      expect(firstTestId).toBeTruthy();
      expect(secondTestId).toBeTruthy();
      const firstUser = String(firstTestId).replace(/^add-user-/, "");
      const secondUser = String(secondTestId).replace(/^add-user-/, "");

      await page.getByTestId(`add-user-${firstUser}`).click();
      await expect(page.getByTestId(`assigned-user-row-${firstUser}`)).toBeVisible();
      await expect(page.getByTestId(`add-user-${firstUser}`)).toHaveCount(0);
      await expect(page.getByTestId(`add-user-${secondUser}`)).toBeVisible();
      await expect(addButtons).toHaveCount(initialAvailable - 1);
      await expect(page.getByTestId("available-users-heading")).toContainText(
        `(${initialAvailable - 1})`,
      );

      await page.getByTestId(`add-user-${secondUser}`).click();
      await expect(page.getByTestId(`assigned-user-row-${secondUser}`)).toBeVisible();
      await expect(addButtons).toHaveCount(initialAvailable - 2);

      await page.getByTestId(`remove-user-${firstUser}`).click();
      await expect(page.getByTestId(`add-user-${firstUser}`)).toBeVisible();
      await expect(page.getByTestId(`assigned-user-row-${firstUser}`)).toHaveCount(0);
      await expect(page.getByTestId(`assigned-user-row-${secondUser}`)).toBeVisible();
      await expect(addButtons).toHaveCount(initialAvailable - 1);

      await page.getByTestId("cancel-role-button").click();
      await expect(page.getByTestId("perc-roles-section")).toBeVisible();

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpectedConsole = consoleErrors.filter((text) => {
        const t = String(text);
        return (
          !/favicon/i.test(t) &&
          !/Download the React DevTools/i.test(t)
        );
      });
      expect(
        unexpectedConsole,
        `console error: ${unexpectedConsole.join(" | ")}`,
      ).toEqual([]);
    },
  );
});

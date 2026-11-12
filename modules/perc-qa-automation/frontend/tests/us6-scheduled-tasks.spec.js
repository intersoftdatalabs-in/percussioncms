const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Admin Shell - Scheduled Tasks & System Tools (US7, US8)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("navigate to admin shell and verify shell and task list render", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);

    // Verify AdminShell container is present
    const shell = page.locator("[data-testid='perc-admin-shell']");
    await expect(shell).toBeVisible();

    // Verify Tasks section is visible by default
    const tasksSection = page.locator("[data-testid='perc-tasks-section']");
    await expect(tasksSection).toBeVisible();

    // Verify tabs are present
    await expect(page.locator("[data-testid='tab-tasks']")).toBeVisible();
    await expect(page.locator("[data-testid='tab-logs']")).toBeVisible();
    await expect(
      page.locator("[data-testid='tab-notifications']"),
    ).toBeVisible();
    await expect(page.locator("[data-testid='tab-tools']")).toBeVisible();
  });

  test("switch tabs to logs, notifications, and system tools", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);

    // Switch to Logs tab
    const logsTab = page.locator("[data-testid='tab-logs']");
    await logsTab.click();
    await expect(
      page.locator("[data-testid='perc-task-logs-section']"),
    ).toBeVisible();

    // Switch to Notifications tab
    const notifTab = page.locator("[data-testid='tab-notifications']");
    await notifTab.click();
    await expect(
      page.locator("[data-testid='perc-task-notifications']"),
    ).toBeVisible();

    // Switch to System Tools tab
    const toolsTab = page.locator("[data-testid='tab-tools']");
    await toolsTab.click();
    await expect(
      page.locator("[data-testid='perc-tools-section']"),
    ).toBeVisible();
    // Tools tab defaults to Security Audit; switch to the Consistency Checker.
    await page.locator("[data-testid='tool-tab-consistency']").click();
    await expect(
      page.locator("[data-testid='perc-consistency-checker']"),
    ).toBeVisible();
  });

  test("open create task editor dialog", async ({ page }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);

    const createBtn = page.locator("[data-testid='create-task-btn']");
    await expect(createBtn).toBeVisible();
    await createBtn.click();

    // Verify editor modal is displayed
    const editor = page.locator("[data-testid='task-dialog']");
    await expect(editor).toBeVisible();
    await expect(page.locator("[data-testid='task-name-input']")).toBeVisible();
  });

  test("expand a log row to show per-log detail (#6160)", async ({ page }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);

    // Switch to Logs tab
    const logsTab = page.locator("[data-testid='tab-logs']");
    await logsTab.click();
    await expect(
      page.locator("[data-testid='perc-task-logs-section']"),
    ).toBeVisible();

    const rows = page.locator("[data-testid^='log-row-']");
    const count = await rows.count();
    if (count === 0) {
      // Empty store: the section renders its empty state, no expanders.
      await expect(
        page.getByText("No execution logs available."),
      ).toBeVisible();
      return;
    }

    // Expand the first row: the TaskDetailLog-equivalent detail region
    // (task name, start/end, elapsed HH:mm:ss, full message) appears.
    const firstRowId = await rows.first().getAttribute("data-testid");
    const logId = String(firstRowId).replace("log-row-", "");
    await page.locator(`[data-testid='log-expand-${logId}']`).click();
    await expect(
      page.locator(`[data-testid='log-detail-${logId}']`),
    ).toBeVisible();
    await expect(
      page.locator(`[data-testid='log-detail-elapsed-${logId}']`),
    ).toHaveText(/(\d+:\d{2}:\d{2}|N\/A)/);
  });
});

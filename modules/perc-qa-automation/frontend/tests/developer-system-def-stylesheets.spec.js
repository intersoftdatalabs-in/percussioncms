/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer System Def stylesheet associations (#4452 / CD-16 / parent #1690).
 *
 * REST: GET/PUT round-trip, add+remove handler, invalid href 400.
 * SPA: Admin sees stylesheet chrome and can persist a href change then restore.
 *
 * Surface:
 *   npm run test:surface -- --path tests/developer-system-def-stylesheets.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
}

function unwrapStylesheets(body) {
  if (!body || typeof body !== "object") {
    return { handlers: [] };
  }
  const nested = body.SystemDefStylesheets || body.systemDefStylesheets || body;
  const raw = nested.handlers;
  let handlers = [];
  if (Array.isArray(raw)) {
    handlers = raw;
  } else if (raw && Array.isArray(raw.SystemDefCommandHandlerStylesheet)) {
    handlers = raw.SystemDefCommandHandlerStylesheet;
  } else if (raw && raw.SystemDefCommandHandlerStylesheet) {
    handlers = [raw.SystemDefCommandHandlerStylesheet];
  }
  return { ...nested, handlers };
}

function developerSystemDefUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "system-def",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

test.describe("System def stylesheets (#4452 / CD-16)", () => {
  test("REST: GET round-trip, add/remove handler, invalid href 400", async ({
    request,
  }) => {
    test.setTimeout(60_000);
    const headers = jsonHeaders();
    const url = `${BASE_URL}/Rhythmyx/services/systemdef/stylesheets`;
    const handlerName = `qa4452${Date.now().toString(36)}`;

    const loaded = await request.get(url, { headers });
    expect(loaded.status(), `GET ${url}`).toBe(200);
    const original = unwrapStylesheets(await loaded.json());
    expect(original.handlers.length, "H2 system def has at least preview").toBeGreaterThan(0);
    const originalHandlers = original.handlers.map((h) => ({
      commandHandler: h.commandHandler || h.CommandHandler,
      href: h.href || h.Href,
    }));

    const roundTrip = await request.put(url, {
      headers,
      data: { SystemDefStylesheets: { handlers: originalHandlers } },
    });
    expect(roundTrip.status(), "PUT original handlers must be 200").toBe(200);
    const afterRound = unwrapStylesheets(await roundTrip.json());
    const afterNames = afterRound.handlers.map(
      (h) => (h.commandHandler || h.CommandHandler || "").toLowerCase(),
    );
    for (const h of originalHandlers) {
      expect(afterNames).toContain((h.commandHandler || "").toLowerCase());
    }

    const added = await request.put(url, {
      headers,
      data: {
        SystemDefStylesheets: {
          handlers: [
            ...originalHandlers,
            {
              commandHandler: handlerName,
              href: "file:../sys_resources/stylesheets/singleFieldEdit.xsl",
            },
          ],
        },
      },
    });
    expect(added.status(), `PUT add ${handlerName}`).toBe(200);
    const addedBody = unwrapStylesheets(await added.json());
    expect(
      addedBody.handlers
        .map((h) => (h.commandHandler || h.CommandHandler || "").toLowerCase())
        .includes(handlerName.toLowerCase()),
    ).toBe(true);

    const restored = await request.put(url, {
      headers,
      data: { SystemDefStylesheets: { handlers: originalHandlers } },
    });
    expect(restored.status(), "PUT restore original handlers").toBe(200);
    const restoredBody = unwrapStylesheets(await restored.json());
    expect(
      restoredBody.handlers
        .map((h) => (h.commandHandler || h.CommandHandler || "").toLowerCase())
        .includes(handlerName.toLowerCase()),
    ).toBe(false);

    const bad = await request.put(url, {
      headers,
      data: {
        SystemDefStylesheets: {
          handlers: [
            {
              commandHandler: "preview",
              href: "https://evil.example/x.xsl",
            },
          ],
        },
      },
    });
    expect(bad.status(), "invalid href must be 400").toBe(400);

    const empty = await request.put(url, {
      headers,
      data: { SystemDefStylesheets: { handlers: [] } },
    });
    expect(empty.status(), "empty handlers must be 400").toBe(400);
  });

  test("Admin can view and save a system-def stylesheet href", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await page.goto(developerSystemDefUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    const panel = page.locator('[data-testid="developer-sys-panel"]');
    const listError = page.locator('[data-testid="developer-sys-error"]');
    await expect(panel.or(listError).first()).toBeVisible({ timeout: 30_000 });
    if (await listError.isVisible()) {
      throw new Error(
        `Developer system def catalog error: ${(await listError.innerText()).trim()}`,
      );
    }

    const section = page.locator('[data-testid="developer-sys-ss"]');
    await expect(section).toBeVisible();
    const hrefInput = page.locator('[data-testid="developer-sys-ss-href-0"]');
    await expect(hrefInput).toBeVisible();
    const original = await hrefInput.inputValue();
    expect(original).toMatch(/^file:\.\.\/(sys_resources|rx_resources)\//);

    const nextHref =
      original === "file:../sys_resources/stylesheets/activeEdit.xsl"
        ? "file:../sys_resources/stylesheets/contentEdit.xsl"
        : "file:../sys_resources/stylesheets/activeEdit.xsl";
    await hrefInput.fill(nextHref);
    const save = page.locator('[data-testid="developer-sys-ss-save"]');
    await expect(save).toBeEnabled();
    await save.click();
    const notice = page.locator('[data-testid="developer-sys-notice"]');
    const writeErr = page.locator('[data-testid="developer-sys-write-error"]');
    await expect(notice.or(writeErr).first()).toBeVisible({ timeout: 30_000 });
    if (await writeErr.isVisible()) {
      throw new Error(`stylesheet save error: ${(await writeErr.innerText()).trim()}`);
    }
    await expect(hrefInput).toHaveValue(nextHref);

    await hrefInput.fill(original);
    await expect(save).toBeEnabled();
    await save.click();
    await expect(notice.or(writeErr).first()).toBeVisible({ timeout: 30_000 });
    if (await writeErr.isVisible()) {
      throw new Error(`stylesheet restore error: ${(await writeErr.innerText()).trim()}`);
    }
    await expect(hrefInput).toHaveValue(original);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});

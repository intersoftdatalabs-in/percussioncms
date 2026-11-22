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
 * REST child field-set creation provisions the child backend table (#6509 / parent #1690).
 *
 * A throwaway content type is created, locked, then a field is added with a NEW `fieldSet`.
 * Before the fix the design save failed (no child backend table: `no such column SYSID` because
 * the field fell back to the parent table). Now POST .../fields returns 200, GET shows the field
 * under the new set, and a second field reuses the provisioned table. The type is deleted at the
 * end (lock still held). No Folder-style packaged fixture required.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *     TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-field-child-create.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { proofScreenshot } = require("./helpers/proof-shot");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
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

/**
 * Same-origin JSON fetch so OWASP CSRF + session cookies apply.
 * Returns { status, json } (json null when the body is not JSON).
 */
async function inPageJson(page, method, path, body) {
  return page.evaluate(
    async ({ method, path, body }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = { Accept: "application/json" };
      if (token) {
        headers[headerName] = token;
      }
      const init = { method, credentials: "same-origin", headers };
      if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(body);
      }
      const res = await fetch(path, init);
      const text = await res.text();
      let json = null;
      try {
        json = text ? JSON.parse(text) : null;
      } catch (e) {
        json = null;
      }
      return { status: res.status, json, text: text.slice(0, 500) };
    },
    { method, path, body },
  );
}

test.describe("Developer content type child field-set create (#6509)", () => {
  test("POST fields with a new fieldSet provisions the child table; save succeeds", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });

    const typeName = `percqakid${Date.now() % 100000}`;
    const base = `/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`;

    const created = await inPageJson(page, "POST", "/Rhythmyx/services/contenttypes/", {
      ContentTypeDetail: { name: typeName, label: "QA Kid Table" },
    });
    expect(created.status, `create type: ${created.text}`).toBe(200);

    let cleanupNote = "created, lock unknown";
    try {
      const locked = await inPageJson(page, "POST", `${base}/lock`);
      expect(locked.status, `lock: ${locked.text}`).toBe(200);

      const first = await inPageJson(page, "POST", `${base}/fields`, {
        ContentTypeField: { name: "rx_kid_note", label: "Kid Note", fieldSet: "kid" },
      });
      expect(
        first.status,
        `first child field (new set) should save, got ${first.status}: ${first.text}`,
      ).toBe(200);

      const detail = await inPageJson(page, "GET", base);
      expect(detail.status, `GET detail: ${detail.text}`).toBe(200);
      const detailBody =
        (detail.json && (detail.json.ContentTypeDetail || detail.json.contentTypeDetail)) ||
        detail.json ||
        {};
      const fields = detailBody.fields || [];
      // GET detail lists each field exactly once: parent rows carry no
      // fieldSet, complex-child rows carry the set name (#6600).
      const kidRows = fields.filter((f) => f.name === "rx_kid_note");
      expect(kidRows, "new child field appears exactly once in GET detail").toHaveLength(1);
      const kidField = kidRows[0];
      expect(kidField, "new child field visible in GET detail").toBeTruthy();
      expect(kidField.fieldSet, "field reports the new child set").toBe("kid");

      const second = await inPageJson(page, "POST", `${base}/fields`, {
        ContentTypeField: { name: "rx_kid_note2", label: "Kid Note 2", fieldSet: "kid" },
      });
      expect(
        second.status,
        `second child field (existing set) should save, got ${second.status}: ${second.text}`,
      ).toBe(200);

      await proofScreenshot(page, "issue-6509-child-fieldset-create.png");
      cleanupNote = "fields saved, deleting type";
    } finally {
      // Delete requires the held lock; best-effort unlock when delete fails.
      const deleted = await inPageJson(page, "DELETE", base);
      if (deleted.status !== 200 && deleted.status !== 204) {
        await inPageJson(page, "POST", `${base}/unlock`);
      }
      expect(
        deleted.status,
        `cleanup DELETE (${cleanupNote}) got ${deleted.status}: ${deleted.text}`,
      ).toBeLessThan(300);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});

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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression: Explorer left panel PathItem HTTP 500 IllegalAnnotationExceptions (GH-3196).
 *
 * JAXB rejected {@code @XmlTransient} on the transient {@code relatedObject}
 * field of {@code PSPathItem}. Path list/find then returned 500 and the tree
 * could not render roots.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const { isHumanReadableErrorText } = require("../helpers/pathmanagement-url");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;

test.describe("GH-3196 PathItem left panel (no IllegalAnnotationExceptions)", () => {
  test("REST: folder/ returns PathItem children, not Errors / 500", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = adminBasicAuthHeaders();
    const root = await request.get(`${PATH_FOLDER}/`, { headers });
    const text = await root.text();
    expect(
      root.status(),
      `GET ${PATH_FOLDER}/ must be 200 (not PathItem 500). Body: ${text.slice(0, 500)}`,
    ).toBe(200);
    expect(text).not.toContain("IllegalAnnotationExceptions");
    const body = JSON.parse(text);
    const items = Array.isArray(body?.PathItem)
      ? body.PathItem
      : Array.isArray(body)
        ? body
        : [];
    expect(items.length, "root PathItem list must not be empty").toBeGreaterThan(
      0,
    );
    expect(items[0].Errors, "PathItem items must not be PSErrors envelopes").toBeUndefined();
    const names = items.map((it) => it?.name).filter(Boolean);
    expect(names, `roots: ${names.join(", ")}`).toContain("Sites");
  });

  test("UI: left tree shows roots or a human-readable error (not blank)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // Ignore unrelated 404/400 resource noise (favicon, optional chrome).
      if (/Failed to load resource/i.test(text)) {
        return;
      }
      pageErrors.push(text);
    });

    await loginAsAdmin(page);
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });

    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const tree = page.locator('[data-testid="explorer-tree"]');
    await expect(tree).toBeVisible({ timeout: 15_000 });

    const treeErr = page.locator('[data-testid="explorer-tree-error"]');
    if ((await treeErr.count()) > 0 && (await treeErr.first().isVisible())) {
      const errText = await treeErr.first().innerText();
      expect(isHumanReadableErrorText(errText), errText).toBe(true);
      expect(errText).not.toContain("[object Object]");
      throw new Error(`Explorer tree failed to load: ${errText}`);
    }

    const nodes = tree.locator('[data-testid^="tree-node-"]');
    await expect(nodes.first()).toBeVisible({ timeout: 20_000 });
    expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });
});

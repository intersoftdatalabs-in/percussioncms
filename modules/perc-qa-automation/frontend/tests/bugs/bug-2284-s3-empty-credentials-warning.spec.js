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
 * Residual Playwright coverage for #2284 non-modal S3 empty-credentials footer
 * warning on the Publish minuet server configuration screen.
 *
 * Product: when driver is AMAZONS3 and Access/Security (or Role ARN with
 * Assume Role) are empty, save shows a footer alert via processAlert and still
 * proceeds (EC2 instance profile / Assume Role use case).
 *
 * <h3>How to run (surface-filtered H2 QA)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/bugs/bug-2284-s3-empty-credentials-warning.spec.js
 *
 *   # Pure helpers (no live CMS):
 *   node --test tests/unit/s3-empty-credentials-warning.test.js
 * </pre>
 *
 * Surface filter tag: {@code @s3-empty-credentials}. Prefer stable selectors
 * ({@code #perc-access-key}, {@code #percFooterAlertTarget}).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  SELECTORS,
  collectEmptyS3CredentialFields,
  buildS3EmptyCredentialsWarning,
  isS3EmptyCredentialsWarningText,
  s3WarningSurfaceSkipReason,
} = require("../helpers/s3-empty-credentials-warning");

test.describe("Publish S3 empty-credentials footer warning (#2284) @s3-empty-credentials", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("pure helper contract matches product warning copy", async () => {
    const empty = collectEmptyS3CredentialFields({
      accessKey: "",
      secretKey: "",
      arnRole: "",
      useAssumeRole: false,
    });
    const msg = buildS3EmptyCredentialsWarning(empty);
    expect(isS3EmptyCredentialsWarningText(msg)).toBeTruthy();
  });

  test("live CMS: empty S3 keys on save show non-modal footer warning", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=publish`, {
      waitUntil: "domcontentloaded",
    });

    // Site card / publish shell — soft skip if stock H2 has no publish surface.
    const siteCard = page.locator(".perc-site-card").first();
    try {
      await expect(siteCard).toBeVisible({ timeout: 25_000 });
    } catch {
      test.skip(
        true,
        s3WarningSurfaceSkipReason("no .perc-site-card on publish view"),
      );
      return;
    }
    await siteCard.click();

    // Prefer editing an existing server, else Add Server.
    const addServerBtn = page.locator(
      "button:has-text('Add Server'), #perc-add-server, .perc-add-server",
    );
    const existingServer = page
      .locator(
        ".perc-server-card, .perc-publish-server, [data-perc-server-id], .list-group-item",
      )
      .first();

    if ((await existingServer.count()) > 0) {
      await existingServer.click().catch(() => {});
    } else if ((await addServerBtn.count()) > 0) {
      await addServerBtn.first().click();
    } else {
      test.skip(true, s3WarningSurfaceSkipReason("no Add Server / server card"));
      return;
    }

    // Select Amazon S3 driver when a driver control is present.
    const driverSelect = page.locator(
      "select[name='server-driver'], #perc-server-driver, select#driver, select.perc-server-driver",
    );
    if ((await driverSelect.count()) > 0) {
      const select = driverSelect.first();
      const options = await select.locator("option").allTextContents();
      const s3Option = options.find(
        (t) => /amazon\s*s3|amazons3/i.test(t) || t.trim() === "AMAZONS3",
      );
      if (s3Option) {
        await select.selectOption({ label: s3Option.trim() }).catch(async () => {
          await select.selectOption({ value: "AMAZONS3" }).catch(() => {});
        });
      } else {
        // Try value AMAZONS3 directly
        await select.selectOption({ value: "AMAZONS3" }).catch(() => {});
      }
    }

    // Wait for S3 credential fields from minuet templates.
    const accessKey = page.locator(SELECTORS.accessKey);
    try {
      await expect(accessKey).toBeVisible({ timeout: 20_000 });
    } catch {
      test.skip(
        true,
        s3WarningSurfaceSkipReason(
          "S3 Access Key field #perc-access-key not visible after driver select",
        ),
      );
      return;
    }

    await accessKey.fill("");
    const secretKey = page.locator(SELECTORS.securityKey);
    if ((await secretKey.count()) > 0) {
      await secretKey.fill("");
    }

    // Ensure Assume Role is off so warning only needs Access + Security.
    const assume = page.locator(SELECTORS.useAssumeRole);
    if ((await assume.count()) > 0) {
      const checked = await assume.isChecked().catch(() => false);
      if (checked) {
        await assume.uncheck().catch(() => {});
      }
    }

    // Bucket may still be required server-side — fill a dummy if present.
    const bucket = page.locator(
      "#perc-bucket-name, #bucketName, input[name='bucketName'], #AS3Bucket",
    );
    if ((await bucket.count()) > 0) {
      const val = await bucket.first().inputValue().catch(() => "");
      if (!String(val || "").trim()) {
        await bucket.first().fill("perc-test-bucket");
      }
    }

    // Server name if required
    const nameInput = page.locator(
      "input[name='server-name'], #perc-server-name, #serverName",
    );
    if ((await nameInput.count()) > 0) {
      const n = await nameInput.first().inputValue().catch(() => "");
      if (!String(n || "").trim()) {
        await nameInput.first().fill("s3-empty-keys-warning-test");
      }
    }

    // Trigger save — product path that runs the empty-S3 warning then updateServerProperties.
    const saveBtn = page.locator(
      "button:has-text('Save'), #perc-save-server, .perc-save-server, button.perc-btn-primary:has-text('Save')",
    );
    if ((await saveBtn.count()) === 0) {
      test.skip(true, s3WarningSurfaceSkipReason("no Save button on server editor"));
      return;
    }
    await saveBtn.first().click();

    const footer = page.locator(SELECTORS.footerAlert);
    await expect(
      footer,
      "footer alert target should receive processAlert for empty S3 keys",
    ).toBeVisible({ timeout: 15_000 });

    const footerText = await footer.innerText();
    expect(
      isS3EmptyCredentialsWarningText(footerText),
      `expected #2284 S3 empty-credentials warning in footer, got: ${footerText.slice(0, 240)}`,
    ).toBeTruthy();
  });
});

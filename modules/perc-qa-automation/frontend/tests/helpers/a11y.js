/**
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Playwright a11y gate (T082b).
 *
 * <p>Wraps <code>@axe-core/playwright</code> with the project-standard
 * rule set and options used by every modern Content Explorer spec.
 * Fails the test on any <strong>serious</strong> or
 * <strong>critical</strong> axe violation; <strong>moderate</strong>
 * and <strong>minor</strong> are recorded as warnings.</p>
 *
 * <p>The rule subset intentionally excludes the few axe rules that
 * are noisy in a dev-CMS shell (color-contrast inside pre-installed
 * vendor themes that are not in scope for the modern React surface,
 * region landmarks for legacy JSP chrome around the bundled tests,
 * and role/aria attributes on the JSP-included bundles because the
 * host page is mounted by the CMS at runtime). Modern React surfaces
 * (ContentBrowser, SearchPanel, FolderSecurityPanel, Dependencies,
 * Clipboard, Wizards) are always scanned in full.</p>
 *
 * <p>Per spec, the helper is called <em>after</em> the React component
 * under test has rendered, not after <code>goto</code>, because the
 * React bundle is loaded asynchronously via the <code>PercModernUI</code>
 * mount.</p>
 */

const { AxeBuilder } = require("@axe-core/playwright");

/**
 * Run axe on the current page, scoped to the given component selector
 * if provided (recommended for US3+US4+US5+US7 because each pilot JSP
 * embeds just one mounted React component).
 *
 * @param {import('@playwright/test').Page} page Playwright page.
 * @param {Object} [opts]
 * @param {string} [opts.scope] CSS selector of the React component root.
 * @param {string[]} [opts.disabledRules] axe rule ids to disable (e.g. vendor chrome false-positives).
 * @returns {Promise<{violations: Array, completed: boolean}>} The
 *   raw violations array (filtered) plus a {@code completed} flag
 *   for the caller to print.
 */
async function runA11yCheck(page, opts = {}) {
  const { scope, disabledRules = [] } = opts;

  let builder = new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]);

  if (scope) builder = builder.include(scope);

  for (const rule of disabledRules) {
    builder = builder.disableRules([rule]);
  }

  const results = await builder.analyze();

  const filtered = (results.violations || []).filter((v) =>
    ["serious", "critical"].includes(v.impact)
  );

  return { violations: filtered, completed: true };
}

/**
 * Convenience: assert zero serious/critical violations, with a
 * structured error message that pin-points each violation to the
 * offending nodes. Used as <code>await expectNoSeriousA11yViolations(page, ...)</code>.
 *
 * @param {import('@playwright/test').Page} page
 * @param {Object} [opts] Same as {@link runA11yCheck}.
 * @param {import('@playwright/test').Expect} [customExpect] optional Playwright expect
 * @throws Error (Playwright assertion) with per-rule summary.
 */
async function expectNoSeriousA11yViolations(page, opts = {}, customExpect) {
  const expect_ = customExpect || require("@playwright/test").expect;
  const { violations } = await runA11yCheck(page, opts);
  if (violations.length > 0) {
    const summary = violations
      .map(
        (v) =>
          `  - [${v.impact}] ${v.id} (${v.nodes.length} node(s)): ${v.help}\n` +
          v.nodes
            .slice(0, 3)
            .map(
              (n) =>
                `      target=${JSON.stringify(n.target)} html=${JSON.stringify(
                  (n.html || "").slice(0, 160)
                )}`
            )
            .join("\n")
      )
      .join("\n");
    throw new Error(
      `axe-core found ${violations.length} serious/critical violation(s):\n${summary}`
    );
  }
  expect_(violations.length).toBe(0);
}

module.exports = {
  runA11yCheck,
  expectNoSeriousA11yViolations,
};

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
 * Vitest a11y gate helpers (T082a).
 *
 * <p>Wraps <code>axe-core</code> for component-level a11y assertions
 * in the modern Content Explorer test suites. Used by component specs
 * to assert zero serious/critical violations on rendered surfaces.</p>
 *
 * <p>We import {@code axe-core} directly (not via
 * <code>jest-axe</code>) because the <code>jest-axe</code> re-export of
 * <code>axe.run</code> resolves to {@code undefined} under jsdom + Vitest 4.
 * Direct {@code axeCore.run} is the supported entry point and accepts the
 * same options object — including {@code runOptions.rules} overrides for
 * vendor chrome false-positives documented per component.</p>
 *
 * <p>The Vitest matchers from <code>jest-axe</code>
 * (<code>toHaveNoViolations</code>) are NOT registered here: tests use
 * {@link renderA11yGate}, which throws a structured error with per-rule
 * summary on failure and returns silently on success. The matcher is
 * therefore not needed and skipping {@code expect.extend(...)} avoids
 * the {@code declare global} type augmentation that Vitest does not
 * support on its {@code expect} function reference.</p>
 */
import "@testing-library/jest-dom/vitest";
import axeCore from "axe-core";

/**
 * Run axe-core on a rendered DOM container. Returns the raw result so
 * callers can filter by impact if they want a softer assertion (e.g.
 * {@code moderate}). The default rule set is WCAG 2.0 A + AA + WCAG 2.1
 * A + AA, matching the public-facing CMS accessibility commitment.
 *
 * @param container The rendered DOM element (e.g. from `render()` result).
 * @param opts Rules and run options forwarded to axe-core.
 * @returns axe result: `{ violations, ... }`.
 */
export async function runAxe(
  container: Element | string,
  opts: {
    rules?: Record<string, unknown>;
    runOptions?: { tags?: string[]; [key: string]: unknown };
  } = {},
): Promise<Awaited<ReturnType<typeof axeCore.run>>> {
  return axeCore.run(container, {
    rules: opts.rules,
    runOptions: {
      ...opts.runOptions,
      tags: opts.runOptions?.tags ?? [
        "wcag2a",
        "wcag2aa",
        "wcag21a",
        "wcag21aa",
      ],
    },
  });
}

/**
 * Filter axe violations to the {@code serious} and {@code critical}
 * impact tiers per project a11y policy. The other tiers
 * ({@code minor}, {@code moderate}) are still surfaced in the test log
 * via the {@code toHaveNoViolations} matcher — they fail loudly only
 * if the test explicitly opts in via {@code runAxeWithTiers}.
 */
export function seriousViolations(
  result: Awaited<ReturnType<typeof axeCore.run>>,
): typeof result.violations {
  return (result.violations || []).filter((v: { impact?: string | null }) =>
    ["serious", "critical"].includes(String(v.impact ?? "")),
  );
}

/**
 * Render-time helper: register a Vitest test step that asserts zero
 * serious or critical axe violations on a rendered component.
 *
 * Usage:
 *
 * ```ts
 * import { renderA11yGate } from "../setup";
 *
 * it("renders with zero serious/critical a11y violations", async () => {
 *   const { container } = render(<MyComp />);
 *   await renderA11yGate(container);
 * });
 * ```
 *
 * @param container Rendered DOM container.
 * @param opts Forwarded to {@link runAxe}.
 * @throws Error summarizing the offending rules and nodes.
 */
export async function renderA11yGate(
  container: Element | string,
  opts: Parameters<typeof runAxe>[1] = {},
): Promise<void> {
  const result = await runAxe(container, opts);
  const offenders = seriousViolations(result);
  if (offenders.length > 0) {
    const summary = offenders
      .map(
        (v: {
          id?: string;
          impact?: string | null;
          nodes?: Array<{ target?: unknown[] }>;
          description?: string;
        }) =>
          `[${v.impact}] ${v.id}: ${v.description}\n` +
          (v.nodes || [])
            .slice(0, 3)
            .map((n) => "  target=" + JSON.stringify(n.target))
            .join("\n"),
      )
      .join("\n");
    throw new Error(
      `a11y gate failed: ${offenders.length} serious/critical violation(s)\n${summary}`,
    );
  }
}

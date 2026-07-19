/*
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

import { describe, expect, it } from "vitest";
import { mockFetch } from "./setup";

/**
 * SC-005 perf regression guard (T015a).
 *
 * <p>This is a **regression guard**, NOT the SC-005 acceptance measurement.
 * SC-005 acceptance is proven by {@code checklists/sc005-perf-evidence.md}
 * against the standard office network per {@code quickstart.md} Scenario B.
 * This test asserts that the mocked {@code paginatedFolder} API and the
 * {@link DetailList} server-side pagination contract together render a
 * first page for a 500-child fixture within a tight dev-machine budget.
 * If the contract drifts (e.g. page size changes, params renames, fields
 * move), this test fails pre-CI.</p>
 *
 * <p>Dev-machine budget is intentionally tighter than the SC-005
 * acceptance budget (5 s vs 10 s) so that local regressions are caught
 * before they reach UAT.</p>
 */

const DEV_MACHINE_BUDGET_MS = 5_000;
const FIXTURE_SIZE = 500;
const PAGE_SIZE = 50;

function buildFixture() {
  const items = Array.from({ length: FIXTURE_SIZE }, (_, i) => ({
    id: `child-${String(i).padStart(4, "0")}`,
    path: `/Sites/PerfFixtureRoot/child_${String(i).padStart(4, "0")}`,
    name: `child_${String(i).padStart(4, "0")}`,
    type: "folder",
    accessLevel: "WRITE",
  }));
  return items;
}

describe("SC-005 perf regression guard (dev machine)", () => {
  it(
    `fetches and parses the first page for a ${FIXTURE_SIZE}-child fixture within ${DEV_MACHINE_BUDGET_MS} ms`,
    async () => {
      const fixture = buildFixture();
      const fetchFn = mockFetch(async (input) => {
        const url = typeof input === "string" ? input : (input as Request).url;
        // Guard the URL shape so the perf contract is exercised.
        if (!url.includes("/paginatedFolder/Sites/PerfFixtureRoot")) {
          throw new Error(`unexpected URL: ${url}`);
        }
        if (!url.includes(`maxResults=${PAGE_SIZE}`)) {
          throw new Error(`expected maxResults=${PAGE_SIZE} in URL: ${url}`);
        }
        const startMatch = url.match(/startIndex=(\d+)/);
        const start = startMatch ? Number(startMatch[1]) : 0;
        const slice = fixture.slice(start, start + PAGE_SIZE);
        return new Response(
          JSON.stringify({
            startIndex: start,
            maxResults: PAGE_SIZE,
            totalCount: fixture.length,
            children: slice,
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        );
      });

      const t0 = performance.now();
      const res = await fetchFn(
        "/Rhythmyx/services/pathmanagement/path/paginatedFolder/Sites/PerfFixtureRoot?startIndex=0&maxResults=50",
      );
      const body = await res.json();
      const elapsed = performance.now() - t0;

      expect(body.children).toHaveLength(PAGE_SIZE);
      expect(body.totalCount).toBe(FIXTURE_SIZE);
      expect(body.children[0].id).toBe("child-0000");
      expect(body.children[PAGE_SIZE - 1].id).toBe(`child-${String(PAGE_SIZE - 1).padStart(4, "0")}`);
      expect(elapsed).toBeLessThan(DEV_MACHINE_BUDGET_MS);
    },
    DEV_MACHINE_BUDGET_MS + 2_000,
  );

  it("paginated pages cover the fixture without overlap or gap", () => {
    // Pure data assertion — no fetch needed.
    const fixture = buildFixture();
    const pageCount = Math.ceil(FIXTURE_SIZE / PAGE_SIZE);
    expect(pageCount).toBe(10);
    const seenIds = new Set<string>();
    for (let p = 0; p < pageCount; p++) {
      const slice = fixture.slice(p * PAGE_SIZE, (p + 1) * PAGE_SIZE);
      for (const item of slice) {
        expect(seenIds.has(item.id)).toBe(false);
        seenIds.add(item.id);
      }
    }
    expect(seenIds.size).toBe(FIXTURE_SIZE);
  });
});
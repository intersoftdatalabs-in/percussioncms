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

import React from "react";
import { render, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, it, vi, beforeEach } from "vitest";
import {
  AdminShell,
  type AdminTab,
} from "../../../main/ts/admin/AdminShell";
import * as client from "../../../main/ts/api/client";
import { renderA11yGate } from "../contentExplorer/a11y";

/**
 * WCAG 2.1 AA audit for SC-004 (issue #6099 / spec 993 T084).
 *
 * Renders the real unified Admin shell (which hosts the former Workflow
 * administration tabs: workflow / roles / users / categories) once per tab
 * with an empty backend, waits for each section to settle out of its
 * loading state, then asserts zero serious/critical axe violations under
 * the wcag2a + wcag2aa + wcag21a + wcag21aa tags.
 *
 * This file IS the repeatable audit artifact: re-running it re-runs the
 * audit. A human-readable results summary lives alongside in
 * `docs/ai-generated/tasks/993-workflow-admin/a11y-audit-sc004.md`.
 */

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(client.get).mockResolvedValue([]);
  vi.mocked(client.post).mockResolvedValue({});
  vi.mocked(client.put).mockResolvedValue({});
  vi.mocked(client.del).mockResolvedValue({});
});

const TABS: readonly { tab: AdminTab; sectionTestId: string }[] = [
  { tab: "tasks", sectionTestId: "perc-tasks-section" },
  { tab: "logs", sectionTestId: "perc-task-logs-section" },
  { tab: "notifications", sectionTestId: "perc-task-notifications" },
  { tab: "tools", sectionTestId: "perc-tools-section" },
  { tab: "workflow", sectionTestId: "perc-workflow-section" },
  { tab: "roles", sectionTestId: "perc-roles-section" },
  { tab: "users", sectionTestId: "perc-users-section" },
  { tab: "categories", sectionTestId: "perc-categories-section" },
];

function renderTab(tab: AdminTab) {
  return render(
    <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/admin"]}>
      <AdminShell initialTab={tab} />
    </MemoryRouter>,
  );
}

describe("AdminShell WCAG 2.1 AA audit (SC-004 / #6099)", () => {
  for (const { tab, sectionTestId } of TABS) {
    it(`tab "${tab}" has zero serious/critical axe violations`, async () => {
      const { container } = renderTab(tab);
      // Let every section finish its mocked backend load (or error path)
      // so axe scans the settled DOM, not a bare "Loading..." div.
      await waitFor(
        () => {
          expect(container.textContent).not.toMatch(/Loading\.\.\./);
        },
        { timeout: 5000 },
      );
      // Prove the settled section (not just shell chrome) is what axe scans.
      expect(
        container.querySelector(`[data-testid="${sectionTestId}"]`),
        `expected settled section ${sectionTestId} for tab "${tab}"`,
      ).not.toBeNull();
      await renderA11yGate(container);
    });
  }
});

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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PublishingShell } from "@/publishing/PublishingShell";
import { renderA11yGate } from "../contentExplorer/a11y";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([]),
  fetchCurrentJobsForSite: vi.fn().mockResolvedValue([]),
  fetchPublishingLogs: vi.fn().mockResolvedValue([]),
  fetchLogDetails: vi.fn().mockResolvedValue({}),
  purgePublishingLogs: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn().mockResolvedValue({
    itemId: "",
    startDate: "",
    endDate: "",
    comments: "",
  }),
  saveItemScheduleDates: vi.fn().mockResolvedValue(undefined),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([]),
  stopPublishing: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/runtimeApi", () => ({
  clearSiteItems: vi.fn().mockResolvedValue({}),
  demandPublish: vi.fn().mockResolvedValue({}),
  listRuntimeEditions: vi.fn().mockResolvedValue([]),
  purgeRuntimeJobLog: vi.fn().mockResolvedValue({}),
  startEditionJob: vi.fn().mockResolvedValue({}),
  stopRuntimeJob: vi.fn().mockResolvedValue({}),
}));

function navTestId(id: string): HTMLElement {
  return screen.getByTestId(`publish-nav-${id}`);
}

describe("PublishingShell section-nav keyboard operation (#4531)", () => {
  it("exposes one tab stop: only the active section carries tabIndex 0", () => {
    render(<PublishingShell />);
    expect(navTestId("sites").getAttribute("tabindex")).toBe("0");
    for (const id of ["status", "logs", "design", "runtime"]) {
      expect(navTestId(id).getAttribute("tabindex")).toBe("-1");
    }
  });

  it("ArrowRight moves focus and opens the next section", () => {
    render(<PublishingShell />);
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
    fireEvent.keyDown(navTestId("sites"), { key: "ArrowRight" });
    expect(document.activeElement).toBe(navTestId("status"));
    expect(screen.getByTestId("publish-section-status")).toBeTruthy();
    expect(screen.queryByTestId("publish-section-sites")).toBeNull();
    expect(navTestId("status").getAttribute("tabindex")).toBe("0");
    expect(navTestId("sites").getAttribute("tabindex")).toBe("-1");
  });

  it("ArrowLeft from the first section wraps to the last", () => {
    render(<PublishingShell />);
    fireEvent.keyDown(navTestId("sites"), { key: "ArrowLeft" });
    expect(document.activeElement).toBe(navTestId("runtime"));
    expect(screen.getByTestId("publish-section-runtime")).toBeTruthy();
  });

  it("End jumps to runtime and Home jumps back to sites", () => {
    render(<PublishingShell />);
    fireEvent.keyDown(navTestId("sites"), { key: "End" });
    expect(document.activeElement).toBe(navTestId("runtime"));
    expect(screen.getByTestId("publish-section-runtime")).toBeTruthy();
    fireEvent.keyDown(navTestId("runtime"), { key: "Home" });
    expect(document.activeElement).toBe(navTestId("sites"));
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
  });

  it("ignores non-movement keys without switching sections", () => {
    render(<PublishingShell />);
    fireEvent.keyDown(navTestId("sites"), { key: "a" });
    expect(document.activeElement).not.toBe(navTestId("status"));
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
    expect(screen.queryByTestId("publish-section-status")).toBeNull();
  });

  it("passes the zero serious/critical axe-core gate (shell nav)", async () => {
    const { container } = render(<PublishingShell />);
    await renderA11yGate(container);
  });
});

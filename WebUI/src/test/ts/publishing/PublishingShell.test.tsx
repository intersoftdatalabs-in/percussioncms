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

import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  defaultLandingSection,
  PublishingShell,
} from "@/publishing/PublishingShell";

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

describe("PublishingShell", () => {
  it("defaults landing to sites (ops first, not Design)", () => {
    expect(defaultLandingSection()).toBe("sites");
    render(<PublishingShell />);
    expect(screen.getByTestId("publishing-shell")).toBeTruthy();
    expect(screen.getByTestId("publish-section-sites")).toBeTruthy();
    expect(screen.queryByTestId("publish-section-design")).toBeNull();
  });

  it("opens status section from prop", () => {
    render(<PublishingShell section="status" />);
    expect(screen.getByTestId("publish-section-status")).toBeTruthy();
  });

  it("can hide Design when showDesign is false", () => {
    render(<PublishingShell showDesign={false} />);
    expect(screen.queryByRole("button", { name: /Design/i })).toBeNull();
  });
});

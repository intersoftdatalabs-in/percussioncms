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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SitesSection } from "@/publishing/sections/SitesSection";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([
    { name: "Corporate Investments", id: "1", siteId: "1" },
    { name: "Enterprise Investments", id: "2", siteId: "2" },
  ]),
}));

describe("SitesSection filter", () => {
  it("filters the sites list by name", async () => {
    render(<SitesSection />);
    await waitFor(() => {
      expect(screen.getByText("Corporate Investments")).toBeTruthy();
    });
    expect(screen.getByText("Enterprise Investments")).toBeTruthy();

    fireEvent.change(screen.getByTestId("publish-sites-filter"), {
      target: { value: "corp" },
    });
    expect(screen.getByText("Corporate Investments")).toBeTruthy();
    expect(screen.queryByText("Enterprise Investments")).toBeNull();

    fireEvent.change(screen.getByTestId("publish-sites-filter"), {
      target: { value: "zzzz-no-match" },
    });
    expect(screen.getByTestId("publish-empty-sites-filter")).toBeTruthy();
  });
});

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
import {
  canStopEdition,
  parseContentIds,
  RuntimeSection,
} from "@/publishing/sections/RuntimeSection";

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([{ name: "SiteA", siteId: "1" }]),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  listServers: vi.fn().mockResolvedValue([
    { serverId: "7", serverName: "LocalFS" },
  ]),
}));

vi.mock("@/api/publishing/runtimeApi", () => ({
  listRuntimeEditions: vi.fn().mockResolvedValue([
    { editionId: "10", name: "Full", runningJobId: 0, pubServerId: "7" },
    { editionId: "11", name: "Demand", runningJobId: 99, jobStatus: "Running" },
  ]),
  startEditionJob: vi.fn().mockResolvedValue({ jobId: 88, status: "started" }),
  stopRuntimeJob: vi.fn().mockResolvedValue({ jobId: 99, status: "cancelled" }),
  demandPublish: vi.fn(),
  clearSiteItems: vi.fn(),
  purgeRuntimeJobLog: vi.fn(),
}));

const runtimeApi = await import("@/api/publishing/runtimeApi");

describe("runtime edition helpers", () => {
  it("canStopEdition when job running", () => {
    expect(canStopEdition({ runningJobId: 5 })).toBe(true);
    expect(canStopEdition({ runningJobId: 0 })).toBe(false);
    expect(canStopEdition({})).toBe(false);
  });

  it("parseContentIds splits mixed separators", () => {
    expect(parseContentIds("1, 2;3  4")).toEqual(["1", "2", "3", "4"]);
  });
});

describe("RuntimeSection", () => {
  it("mounts runtime section", () => {
    render(<RuntimeSection />);
    expect(screen.getByTestId("publish-section-runtime")).toBeTruthy();
  });

  it("starts selected edition and shows status", async () => {
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("runtime-start-10")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("runtime-start-10"));
    await waitFor(() => {
      expect(runtimeApi.startEditionJob).toHaveBeenCalledWith("10");
    });
    await waitFor(() => {
      expect(screen.getByTestId("runtime-job-status").textContent).toMatch(
        /started/i,
      );
    });
  });

  it("stops a running edition job and shows status", async () => {
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("runtime-stop-11")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("runtime-stop-11"));
    await waitFor(() => {
      expect(runtimeApi.stopRuntimeJob).toHaveBeenCalledWith(99);
    });
    await waitFor(() => {
      expect(screen.getByTestId("runtime-job-status").textContent).toMatch(
        /cancelled/i,
      );
    });
  });
});

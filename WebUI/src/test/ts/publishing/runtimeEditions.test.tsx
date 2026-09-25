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
import { afterEach, describe, expect, it, vi } from "vitest";

function fallback(key: string): string {
  const at = key.lastIndexOf("@");
  return at >= 0 ? key.slice(at + 1) : key;
}
import { MSG } from "@/i18n/message";
import {
  canStopEdition,
  openableRunningJobId,
  parseContentIds,
  runtimeMessage,
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

  it("openableRunningJobId is null for idle editions", () => {
    expect(openableRunningJobId({ runningJobId: 99 })).toBe("99");
    expect(openableRunningJobId({ runningJobId: 0 })).toBeNull();
    expect(openableRunningJobId({})).toBeNull();
  });

  it("parseContentIds splits mixed separators", () => {
    expect(parseContentIds("1, 2;3  4")).toEqual(["1", "2", "3", "4"]);
  });

  it("runtimeMessage substitutes {0} from the catalog key", () => {
    expect(runtimeMessage(MSG.PUBLISH.SECTIONS.RUNTIME.JOB_RUNNING, "5")).toBe(
      "Job 5",
    );
    expect(runtimeMessage(MSG.PUBLISH.SECTIONS.RUNTIME.CONFIRM_PURGE, "9")).toBe(
      "Purge log for job 9?",
    );
  });
});

describe("RuntimeSection", () => {
  const demandKey = MSG.PUBLISH.SECTIONS.RUNTIME.DEMAND_HEADING;

  afterEach(() => {
    delete window.I18N;
  });

  it("renders demand heading from the catalog, not a hardcoded node", () => {
    window.I18N = {
      message: (key: string) =>
        key === demandKey ? "CATALOG_DEMAND_PUBLISH" : fallback(key),
    };
    render(<RuntimeSection />);
    expect(screen.getByTestId("runtime-demand-heading").textContent).toBe(
      "CATALOG_DEMAND_PUBLISH",
    );
    expect(screen.getByRole("button", { name: "Queue demand" })).toBeTruthy();
  });

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

  it("opens a running job and hides the control on idle editions", async () => {
    const onOpenRunningJob = vi.fn();
    render(<RuntimeSection onOpenRunningJob={onOpenRunningJob} />);
    await waitFor(() => {
      expect(screen.getByTestId("runtime-open-job-11")).toBeTruthy();
    });
    expect(screen.queryByTestId("runtime-open-job-10")).toBeNull();
    fireEvent.click(screen.getByTestId("runtime-open-job-11"));
    expect(onOpenRunningJob).toHaveBeenCalledWith("99");
  });

  it("queues demand publish with the selected edition and parsed content ids", async () => {
    vi.mocked(runtimeApi.demandPublish).mockResolvedValue({
      editionId: "10",
      requestId: 44,
      status: "queued",
    });
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-runtime").textContent).toMatch(
        /Selected edition: 10/,
      );
    });
    fireEvent.change(screen.getByTestId("runtime-demand-ids"), {
      target: { value: "101, 102" },
    });
    fireEvent.click(screen.getByTestId("runtime-demand-submit"));
    await waitFor(() => {
      expect(runtimeApi.demandPublish).toHaveBeenCalledWith("10", {
        contentIds: ["101", "102"],
      });
    });
    await waitFor(() => {
      expect(screen.getByTestId("runtime-job-status").textContent).toMatch(
        /queued/i,
      );
    });
    expect(
      screen.getByTestId("publish-section-runtime").textContent,
    ).toMatch(/request 44/);
  });

  it("does not call demand publish when the content id list is empty", async () => {
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-runtime").textContent).toMatch(
        /Selected edition: 10/,
      );
    });
    fireEvent.click(screen.getByTestId("runtime-demand-submit"));
    expect(runtimeApi.demandPublish).not.toHaveBeenCalled();
    expect(screen.getByRole("alert").textContent).toMatch(
      /at least one content id/i,
    );
    expect(screen.queryByTestId("runtime-job-status")).toBeNull();
  });

  it("keeps HTTP demand errors on the Runtime section", async () => {
    vi.mocked(runtimeApi.demandPublish).mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "folderId required for contentId 101" },
    });
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-runtime").textContent).toMatch(
        /Selected edition: 10/,
      );
    });
    fireEvent.change(screen.getByTestId("runtime-demand-ids"), {
      target: { value: "101" },
    });
    fireEvent.click(screen.getByTestId("runtime-demand-submit"));
    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toMatch(
        /folderId required for contentId 101/,
      );
    });
    const section = screen.getByTestId("publish-section-runtime");
    expect(section.contains(screen.getByRole("alert"))).toBe(true);
    expect(screen.queryByTestId("runtime-job-status")).toBeNull();
  });

  it("stops a running edition job and shows status", async () => {
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("runtime-stop-11")).toBeTruthy();
    });
    expect(screen.queryByTestId("runtime-stop-10")).toBeNull();
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

  it("shows stop failure text and does not keep a last result", async () => {
    vi.mocked(runtimeApi.stopRuntimeJob).mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "edition job 99 is not running" },
    });
    render(<RuntimeSection />);
    await waitFor(() => {
      expect(screen.getByTestId("runtime-stop-11")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("runtime-stop-11"));
    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toMatch(
        /edition job 99 is not running/,
      );
    });
    expect(screen.queryByTestId("runtime-job-status")).toBeNull();
  });
});

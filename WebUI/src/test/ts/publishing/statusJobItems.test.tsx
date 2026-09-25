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
import { StatusJobDetailPanel } from "@/publishing/components/StatusJobDetailPanel";
import {
  statusJobItemRows,
  statusJobItemsFailure,
} from "@/publishing/statusJobItems";

describe("statusJobItemRows (#4886)", () => {
  it("reads id and title from the log-details payload", () => {
    const rows = statusJobItemRows({
      SitePublishItem: [
        { contentid: 42, title: "Home" },
        { contentid: 7, fileName: "about.html" },
        { contentid: 0, name: "draft" },
      ],
    });
    expect(rows[0]).toEqual({ contentId: "42", title: "Home", openId: 42 });
    expect(rows[1]).toEqual({
      contentId: "7",
      title: "about.html",
      openId: 7,
    });
    expect(rows[2]?.openId).toBeNull();
    expect(rows[2]?.title).toBe("draft");
  });

  it("treats a missing list as empty, not an error", () => {
    expect(statusJobItemRows(null)).toEqual([]);
    expect(statusJobItemRows({ SitePublishItem: [] })).toEqual([]);
  });

  it("maps 403 and 404 on the item list", () => {
    expect(statusJobItemsFailure({ status: 403, statusText: "", body: null })).toBe(
      "forbidden",
    );
    expect(statusJobItemsFailure({ status: 404, statusText: "", body: null })).toBe(
      "not_found",
    );
    expect(statusJobItemsFailure(new Error("down"))).toBe("failed");
  });
});

describe("StatusJobDetailPanel items (#4886)", () => {
  const job = {
    jobId: 4886,
    siteName: "FastForward",
    editionName: "Full",
    status: "Running",
  };

  it("shows an empty state and keeps the job panel", async () => {
    render(
      <StatusJobDetailPanel
        job={job}
        onClose={() => undefined}
        loadItems={vi.fn().mockResolvedValue({ SitePublishItem: [] })}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-items-empty").textContent).toBe(
        "No content items for this job",
      );
    });
    expect(screen.getByTestId("publish-status-job-detail")).toBeTruthy();
    expect(screen.queryByTestId("publish-status-items-error")).toBeNull();
  });

  it("keeps 404 in the detail panel", async () => {
    render(
      <StatusJobDetailPanel
        job={job}
        onClose={() => undefined}
        loadItems={vi.fn().mockRejectedValue({ status: 404, statusText: "", body: null })}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-items-error").textContent).toBe(
        "Job items were not found",
      );
    });
    expect(screen.getByTestId("publish-status-detail-job-id").textContent).toBe(
      "4886",
    );
    expect(screen.queryByTestId("publish-status-items-empty")).toBeNull();
  });

  it("opens the editor for a content id and omits Open when there is no id", async () => {
    const openItem = vi.fn().mockResolvedValue({ ok: true, reason: "opened" });
    render(
      <StatusJobDetailPanel
        job={job}
        onClose={() => undefined}
        loadItems={vi.fn().mockResolvedValue({
          SitePublishItem: [
            { contentid: 42, title: "Home" },
            { fileName: "orphan.html" },
          ],
        })}
        openItem={openItem}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-item-name-42").textContent).toBe(
        "Home",
      );
    });
    expect(screen.queryByTestId("publish-status-open-item-orphan")).toBeNull();
    expect(screen.getByTestId("publish-status-item-name-row-1").textContent).toBe(
      "orphan.html",
    );
    vi.stubGlobal("open", vi.fn(() => null));
    fireEvent.click(screen.getByTestId("publish-status-open-item-42"));
    await waitFor(() => {
      expect(openItem).toHaveBeenCalledWith(42, null);
    });
  });

  it("keeps an editor 403 in the panel", async () => {
    const openItem = vi.fn().mockResolvedValue({ ok: false, reason: "forbidden" });
    render(
      <StatusJobDetailPanel
        job={job}
        onClose={() => undefined}
        loadItems={vi.fn().mockResolvedValue({
          SitePublishItem: [{ contentid: 9, name: "Page" }],
        })}
        openItem={openItem}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-open-item-9")).toBeTruthy();
    });
    vi.stubGlobal("open", vi.fn(() => null));
    fireEvent.click(screen.getByTestId("publish-status-open-item-9"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-item-open-error").textContent).toMatch(
        /not allowed/i,
      );
    });
    expect(screen.getByTestId("publish-status-job-detail")).toBeTruthy();
  });
});

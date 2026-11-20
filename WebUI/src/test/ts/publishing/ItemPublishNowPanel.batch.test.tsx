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
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ItemPublishNowPanel } from "@/publishing/components/ItemPublishNowPanel";
import { publishSelectedItem } from "@/contentExplorer/itemPublish";

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    publishSelectedItem: vi.fn(),
  };
});

const publishNow = vi.mocked(publishSelectedItem);

describe("ItemPublishNowPanel batch retry", () => {
  beforeEach(() => {
    publishNow.mockReset();
    publishNow.mockResolvedValue(true);
  });

  async function reviewBatch(ids: string): Promise<void> {
    render(<ItemPublishNowPanel />);
    fireEvent.change(screen.getByTestId("item-publish-now-id"), {
      target: { value: ids },
    });
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-confirm")).toBeTruthy();
    });
  }

  it("reviews several ids with one confirm listing each id", async () => {
    await reviewBatch("42, 43");
    expect(screen.getByTestId("item-publish-now-batch-item-42")).toBeTruthy();
    expect(screen.getByTestId("item-publish-now-batch-item-43")).toBeTruthy();
    expect(screen.getByTestId("item-publish-now-confirm").textContent).toContain(
      "2",
    );
    expect(publishNow).not.toHaveBeenCalled();
  });

  it("publishes every id and reports the full run", async () => {
    const onPublished = vi.fn();
    render(<ItemPublishNowPanel onPublished={onPublished} />);
    fireEvent.change(screen.getByTestId("item-publish-now-id"), {
      target: { value: "42 43" },
    });
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
    });
    expect(publishNow).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("item-publish-now-success").textContent).toContain(
      "all 2 items",
    );
    expect(
      screen.getByTestId("item-publish-now-batch-report-item-42").textContent,
    ).toContain("publish started");
    expect(screen.queryByTestId("item-publish-now-retry")).toBeNull();
    expect(onPublished).toHaveBeenCalled();
  });

  it("names a per-item failure and offers retry for the failed id", async () => {
    publishNow
      .mockResolvedValueOnce(true)
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: null });
    await reviewBatch("42,43");
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-publish-now-success")).toBeNull();
    expect(screen.getByTestId("item-publish-now-error").textContent).toContain(
      "Published 1 of 2",
    );
    expect(screen.getByTestId("item-publish-now-error").textContent).toContain(
      "43 (Publish Forbidden (HTTP 403 Forbidden))",
    );
    expect(
      screen.getByTestId("item-publish-now-batch-report-item-43").textContent,
    ).toContain("43");
    expect(screen.getByTestId("item-publish-now-retry")).toBeTruthy();
  });

  it("retry republishes only the failed ids and clears the report", async () => {
    publishNow
      .mockResolvedValueOnce(true)
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: null });
    await reviewBatch("42,43");
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-retry")).toBeTruthy();
    });
    expect(publishNow).toHaveBeenCalledTimes(2);
    publishNow.mockReset();
    publishNow.mockResolvedValue(true);
    fireEvent.click(screen.getByTestId("item-publish-now-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
    });
    expect(publishNow).toHaveBeenCalledTimes(1);
    expect(publishNow.mock.calls[0]?.[0]).toMatchObject({ id: "43" });
    expect(screen.getByTestId("item-publish-now-success").textContent).toContain(
      "Publish now started for 1 item",
    );
    expect(screen.queryByTestId("item-publish-now-retry")).toBeNull();
    expect(
      screen.getByTestId("item-publish-now-batch-report-item-43").textContent,
    ).toContain("publish started");
  });

  it("retry on a single failed publish resubmits the same item", async () => {
    publishNow.mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ItemPublishNowPanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-retry")).toBeTruthy();
    });
    publishNow.mockReset();
    publishNow.mockResolvedValue(true);
    fireEvent.click(screen.getByTestId("item-publish-now-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
    });
    expect(publishNow).toHaveBeenCalledTimes(1);
    expect(publishNow.mock.calls[0]?.[0]).toMatchObject({ id: "42" });
  });

  it("keeps a multi-id lookup out of the shared workspace item", async () => {
    const onItemIdChange = vi.fn();
    render(<ItemPublishNowPanel onItemIdChange={onItemIdChange} />);
    fireEvent.change(screen.getByTestId("item-publish-now-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-confirm")).toBeTruthy();
    });
    expect(onItemIdChange).not.toHaveBeenCalled();
  });
});

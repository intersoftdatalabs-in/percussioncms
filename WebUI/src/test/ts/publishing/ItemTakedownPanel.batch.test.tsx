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
import { get } from "@/api/client";
import { takedownSelectedItem } from "@/contentExplorer/itemPublish";
import { ItemTakedownPanel } from "@/publishing/components/ItemTakedownPanel";

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    takedownSelectedItem: vi.fn(),
  };
});

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
  };
});

const getMock = vi.mocked(get);
const takeDown = vi.mocked(takedownSelectedItem);

function linkedBody(path: string): unknown {
  return { ArrayList: [{ id: "7", pagePath: path }] };
}

describe("ItemTakedownPanel batch", () => {
  beforeEach(() => {
    getMock.mockReset();
    takeDown.mockReset();
    getMock.mockImplementation(async (url: string) =>
      linkedBody(`/Sites/Demo/${encodeURIComponent(url).length}`),
    );
    takeDown.mockResolvedValue(true);
  });

  async function reviewBatch(ids: string): Promise<void> {
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: ids },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
    });
  }

  it("reviews several ids with one confirm listing linked pages per item", async () => {
    getMock
      .mockResolvedValueOnce(linkedBody("/Sites/Demo/Home"))
      .mockResolvedValueOnce({ ArrayList: [] });
    await reviewBatch("42, 43");
    expect(getMock).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("item-takedown-batch-item-42").textContent).toContain(
      "/Sites/Demo/Home",
    );
    expect(
      screen.getByTestId("item-takedown-batch-linked-empty-43"),
    ).toBeTruthy();
  });

  it("takes down every id and reports the full run", async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    await reviewBatch("42 43");
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
    });
    expect(takeDown).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("item-takedown-success").textContent).toContain(
      "Take down started for all 2 items",
    );
    expect(
      screen.getByTestId("item-takedown-batch-report-item-42").textContent,
    ).toContain("taken down");
  });

  it("names a per-item failure and offers retry for the failed id", async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown
      .mockResolvedValueOnce(true)
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: null });
    await reviewBatch("42,43");
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
    expect(screen.getByTestId("item-takedown-error").textContent).toContain(
      "Taken down 1 of 2",
    );
    expect(screen.getByTestId("item-takedown-error").textContent).toContain(
      "43 (Publish Forbidden (HTTP 403 Forbidden))",
    );
    expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
  });

  it("retry takes down only the failed ids and clears the button", async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown
      .mockResolvedValueOnce(true)
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: null });
    await reviewBatch("42,43");
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
    });
    expect(takeDown).toHaveBeenCalledTimes(2);
    takeDown.mockReset();
    takeDown.mockResolvedValue(true);
    fireEvent.click(screen.getByTestId("item-takedown-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
    });
    expect(takeDown).toHaveBeenCalledTimes(1);
    expect(takeDown.mock.calls[0]?.[0]).toMatchObject({ id: "43" });
    expect(
      screen.getByTestId("item-takedown-batch-report-item-43").textContent,
    ).toContain("taken down");
    expect(screen.queryByTestId("item-takedown-retry")).toBeNull();
  });

  it("retry on a single failed takedown resubmits the same item", async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown.mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
    });
    takeDown.mockReset();
    takeDown.mockResolvedValue(true);
    fireEvent.click(screen.getByTestId("item-takedown-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
    });
    expect(takeDown).toHaveBeenCalledTimes(1);
  });

  it("keeps a multi-id lookup out of the shared workspace item", async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    const onItemIdChange = vi.fn();
    render(<ItemTakedownPanel onItemIdChange={onItemIdChange} />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
    });
    expect(onItemIdChange).not.toHaveBeenCalled();
  });
});

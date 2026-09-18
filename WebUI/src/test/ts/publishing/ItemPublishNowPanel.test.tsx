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

describe("ItemPublishNowPanel", () => {
  beforeEach(() => {
    publishNow.mockReset();
    publishNow.mockResolvedValue(true);
  });

  it("needs an item id before review", async () => {
    render(<ItemPublishNowPanel />);
    expect(screen.getByTestId("item-publish-now")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-error")).toBeTruthy();
    });
    expect(publishNow).not.toHaveBeenCalled();
    expect(screen.queryByTestId("item-publish-now-submit")).toBeNull();
  });

  it("publishes a page after confirm", async () => {
    const onPublished = vi.fn();
    render(<ItemPublishNowPanel itemId="42" onPublished={onPublished} />);
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(publishNow).toHaveBeenCalled();
    });
    const item = publishNow.mock.calls[0]?.[0];
    expect(item?.id).toBe("42");
    expect(item?.path).toMatch(/\/Sites\//);
    expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
    expect(onPublished).toHaveBeenCalled();
  });

  it("publishes a resource when Asset is selected", async () => {
    render(<ItemPublishNowPanel itemId="99" />);
    fireEvent.click(screen.getByTestId("item-publish-now-kind-resource"));
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => expect(publishNow).toHaveBeenCalled());
    expect(publishNow.mock.calls[0]?.[0]?.path).toMatch(/\/Assets\//);
  });

  it("surfaces HTTP 403 as forbidden, not success", async () => {
    publishNow.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "User demo is editing this page." },
    });
    render(<ItemPublishNowPanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-error").textContent).toMatch(
        /editing this page|Forbidden|Publish Forbidden/i,
      );
    });
    expect(screen.queryByTestId("item-publish-now-success")).toBeNull();
  });

  it("surfaces HTTP 400 as an error, not success", async () => {
    publishNow.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Invalid item" },
    });
    render(<ItemPublishNowPanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-error").textContent).toMatch(
        /Invalid item|Bad Request|400/i,
      );
    });
    expect(screen.queryByTestId("item-publish-now-success")).toBeNull();
  });

  it("surfaces HTTP 404 as not found, not success", async () => {
    publishNow.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Unknown item" },
    });
    render(<ItemPublishNowPanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-publish-now-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publish-now-error").textContent).toMatch(
        /Unknown item|not found|404/i,
      );
    });
    expect(screen.queryByTestId("item-publish-now-success")).toBeNull();
  });
});

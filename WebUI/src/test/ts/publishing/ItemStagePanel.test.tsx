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
import { ItemStagePanel } from "@/publishing/components/ItemStagePanel";
import {
  removeFromStagingSelectedItem,
  stageSelectedItem,
} from "@/contentExplorer/itemPublish";

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    removeFromStagingSelectedItem: vi.fn(),
    stageSelectedItem: vi.fn(),
  };
});

const stage = vi.mocked(stageSelectedItem);
const unstage = vi.mocked(removeFromStagingSelectedItem);

describe("ItemStagePanel", () => {
  beforeEach(() => {
    stage.mockReset();
    unstage.mockReset();
    stage.mockResolvedValue(true);
    unstage.mockResolvedValue(true);
  });

  it("needs an item id before review", async () => {
    render(<ItemStagePanel />);
    expect(screen.getByTestId("item-stage")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-error")).toBeTruthy();
    });
    expect(stage).not.toHaveBeenCalled();
    expect(unstage).not.toHaveBeenCalled();
    expect(screen.queryByTestId("item-stage-submit")).toBeNull();
  });

  it("stages a page after confirm", async () => {
    render(<ItemStagePanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => {
      expect(stage).toHaveBeenCalled();
    });
    const item = stage.mock.calls[0]?.[0];
    expect(item?.id).toBe("42");
    expect(item?.path).toMatch(/\/Sites\//);
    expect(screen.getByTestId("item-stage-success")).toBeTruthy();
  });

  it("removes from staging when Unstage is selected", async () => {
    render(<ItemStagePanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-stage-action-unstage"));
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => {
      expect(unstage).toHaveBeenCalled();
    });
    expect(stage).not.toHaveBeenCalled();
    const item = unstage.mock.calls[0]?.[0];
    expect(item?.id).toBe("42");
    expect(item?.path).toMatch(/\/Sites\//);
    expect(screen.getByTestId("item-stage-success")).toBeTruthy();
  });

  it("stages a resource when Asset is selected", async () => {
    render(<ItemStagePanel itemId="99" />);
    fireEvent.click(screen.getByTestId("item-stage-kind-resource"));
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => expect(stage).toHaveBeenCalled());
    expect(stage.mock.calls[0]?.[0]?.path).toMatch(/\/Assets\//);
  });

  it("removes a resource from staging when Asset + Unstage are selected", async () => {
    render(<ItemStagePanel itemId="99" />);
    fireEvent.click(screen.getByTestId("item-stage-kind-resource"));
    fireEvent.click(screen.getByTestId("item-stage-action-unstage"));
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => expect(unstage).toHaveBeenCalled());
    expect(unstage.mock.calls[0]?.[0]?.path).toMatch(/\/Assets\//);
    expect(stage).not.toHaveBeenCalled();
  });

  it("surfaces HTTP 403 as forbidden, not success", async () => {
    stage.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "User demo is editing this page." },
    });
    render(<ItemStagePanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-error").textContent).toMatch(
        /editing this page|Forbidden|Publish Forbidden/i,
      );
    });
    expect(screen.queryByTestId("item-stage-success")).toBeNull();
  });

  it("surfaces HTTP 400 as an error, not success", async () => {
    stage.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Invalid item" },
    });
    render(<ItemStagePanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-error").textContent).toMatch(
        /Invalid item|Bad Request|400/i,
      );
    });
    expect(screen.queryByTestId("item-stage-success")).toBeNull();
  });

  it("surfaces HTTP 404 as not found, not success", async () => {
    stage.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Unknown item" },
    });
    render(<ItemStagePanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-error").textContent).toMatch(
        /Unknown item|not found|404/i,
      );
    });
    expect(screen.queryByTestId("item-stage-success")).toBeNull();
  });

  it("surfaces HTTP 200 FORBIDDEN as an error, not success", async () => {
    stage.mockRejectedValue(new Error("FORBIDDEN"));
    render(<ItemStagePanel itemId="42" />);
    fireEvent.click(screen.getByTestId("item-stage-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-submit")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-stage-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-stage-error").textContent).toMatch(
        /FORBIDDEN|Publish Forbidden/i,
      );
    });
    expect(screen.queryByTestId("item-stage-success")).toBeNull();
  });
});

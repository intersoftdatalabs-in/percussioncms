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
import { ItemTakedownPanel } from "@/publishing/components/ItemTakedownPanel";
import {
  loadLinkedPagesForTakedown,
  takedownSelectedItem,
} from "@/contentExplorer/itemPublish";

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    loadLinkedPagesForTakedown: vi.fn(),
    takedownSelectedItem: vi.fn(),
  };
});

const loadLinked = vi.mocked(loadLinkedPagesForTakedown);
const takeDown = vi.mocked(takedownSelectedItem);

describe("ItemTakedownPanel", () => {
  beforeEach(() => {
    loadLinked.mockReset();
    takeDown.mockReset();
    loadLinked.mockResolvedValue([]);
    takeDown.mockResolvedValue(true);
  });

  it("needs an item id before review", async () => {
    render(<ItemTakedownPanel />);
    expect(screen.getByTestId("item-takedown")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error")).toBeTruthy();
    });
    expect(loadLinked).not.toHaveBeenCalled();
    expect(screen.queryByTestId("item-takedown-submit")).toBeNull();
  });

  it("reviews linked pages for a deep-linked item id", async () => {
    loadLinked.mockResolvedValue([
      { id: "7", pagePath: "/Sites/Demo/Home" },
      { id: "8", pagePath: "/Sites/Demo/About" },
    ]);
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => {
      expect(loadLinked).toHaveBeenCalledWith("42");
    });
    expect(screen.getByTestId("item-takedown-linked").textContent).toContain(
      "/Sites/Demo/Home",
    );
    expect(screen.getByTestId("item-takedown-linked").textContent).toContain(
      "/Sites/Demo/About",
    );
  });

  it("takes down a page after review with GET contract (no linked)", async () => {
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(takeDown).toHaveBeenCalled();
    });
    const item = takeDown.mock.calls[0]?.[0];
    expect(item?.id).toBe("42");
    expect(item?.path).toMatch(/\/Sites\//);
    expect(takeDown.mock.calls[0]?.[1]).toEqual([]);
    expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
  });

  it("PUTs linked pages when the confirm list is non-empty", async () => {
    const linked = [
      { id: "7", pagePath: "/Sites/Demo/Home", relationshipId: "rel-1" },
    ];
    loadLinked.mockResolvedValue(linked);
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(takeDown).toHaveBeenCalledWith(
        expect.objectContaining({ id: "42" }),
        linked,
      );
    });
    expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
  });

  it("takes down a resource when Asset is selected", async () => {
    render(<ItemTakedownPanel itemId="99" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-kind-resource"));
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => expect(takeDown).toHaveBeenCalled());
    expect(takeDown.mock.calls[0]?.[0]?.path).toMatch(/\/Assets\//);
  });

  it("surfaces HTTP 200 FORBIDDEN as an error, not success", async () => {
    takeDown.mockRejectedValue(new Error("FORBIDDEN"));
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error").textContent).toMatch(
        /FORBIDDEN|Publish Forbidden/i,
      );
    });
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
  });

  it("surfaces HTTP 200 BADCONFIG as an error, not success", async () => {
    takeDown.mockRejectedValue(
      new Error(
        "Could not connect to publishing server, please check publishing server configuration.",
      ),
    );
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error").textContent).toMatch(
        /Could not connect to publishing server/i,
      );
    });
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
  });

  it("surfaces HTTP 200 INVALID as an error, not success", async () => {
    takeDown.mockRejectedValue(new Error("INVALID"));
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error").textContent).toMatch(
        /INVALID/i,
      );
    });
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
  });

  it("surfaces HTTP 403 as forbidden, not success", async () => {
    takeDown.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "User demo is editing this page." },
    });
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(() => expect(loadLinked).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("item-takedown-error").textContent).toMatch(
        /editing this page|Forbidden|Publish Forbidden/i,
      );
    });
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
  });
});

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
import { ItemPublishingActionsMenu } from "@/publishing/components/ItemPublishingActionsMenu";
import { fetchItemPublishingActions } from "@/api/publishing/itemPublishingActionsApi";

vi.mock("@/api/publishing/itemPublishingActionsApi", () => ({
  fetchItemPublishingActions: vi.fn(),
}));

const fetchActions = vi.mocked(fetchItemPublishingActions);

describe("ItemPublishingActionsMenu", () => {
  beforeEach(() => {
    fetchActions.mockReset();
  });

  it("needs an item id before load", async () => {
    render(<ItemPublishingActionsMenu />);
    expect(screen.getByTestId("item-publishing-actions")).toBeTruthy();
    fireEvent.click(screen.getByTestId("item-publishing-actions-load"));
    await waitFor(() => {
      expect(screen.getByTestId("item-publishing-actions-error")).toBeTruthy();
    });
    expect(fetchActions).not.toHaveBeenCalled();
  });

  it("renders server rows with unavailable actions disabled", async () => {
    fetchActions.mockResolvedValue([
      { name: "Publish", enabled: true },
      { name: "Schedule...", enabled: false },
      { name: "Remove from Site", enabled: true },
      { name: "Stage", enabled: false },
      { name: "Remove from Staging", enabled: true },
    ]);
    const onItemIdChange = vi.fn();
    render(
      <ItemPublishingActionsMenu itemId="42" onItemIdChange={onItemIdChange} />,
    );
    await waitFor(() => {
      expect(fetchActions).toHaveBeenCalledWith("42");
    });
    await waitFor(() => {
      expect(screen.getByTestId("item-publishing-actions-menu")).toBeTruthy();
    });
    expect(screen.getByTestId("item-action-publish-now")).toBeTruthy();
    expect(
      (screen.getByTestId("item-action-publish-now") as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    expect(
      (screen.getByTestId("item-action-schedule") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("item-action-takedown") as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    expect(
      (screen.getByTestId("item-action-stage") as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("item-action-unstage") as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    expect(screen.queryByTestId("item-publishing-actions-error")).toBeNull();
  });

  it("skips unknown server action names", async () => {
    fetchActions.mockResolvedValue([
      { name: "Publish", enabled: true },
      { name: "FutureAction", enabled: true },
    ]);
    render(<ItemPublishingActionsMenu itemId="7" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-publishing-actions-menu")).toBeTruthy();
    });
    expect(screen.getByTestId("item-action-publish-now")).toBeTruthy();
    expect(screen.queryByText("FutureAction")).toBeNull();
  });

  it("shows empty state when the server returns no actions", async () => {
    fetchActions.mockResolvedValue([]);
    render(<ItemPublishingActionsMenu itemId="7" />);
    await waitFor(() => {
      expect(
        screen.getByTestId("item-publishing-actions-empty"),
      ).toBeTruthy();
    });
  });

  it("surfaces 403 as forbidden, not empty success", async () => {
    fetchActions.mockRejectedValue({ status: 403, message: "denied" });
    render(<ItemPublishingActionsMenu itemId="42" />);
    await waitFor(() => {
      expect(
        screen.getByTestId("item-publishing-actions-error"),
      ).toBeTruthy();
    });
    expect(screen.getByTestId("item-publishing-actions-error").textContent).toMatch(
      /Forbidden/i,
    );
    expect(screen.queryByTestId("item-publishing-actions-menu")).toBeNull();
  });

  it("surfaces 404 as not found, not empty success", async () => {
    fetchActions.mockRejectedValue({ status: 404, message: "no such item" });
    render(<ItemPublishingActionsMenu itemId="4242" />);
    await waitFor(() => {
      expect(
        screen.getByTestId("item-publishing-actions-error"),
      ).toBeTruthy();
    });
    expect(screen.getByTestId("item-publishing-actions-error").textContent).toMatch(
      /not found/i,
    );
    expect(screen.queryByTestId("item-publishing-actions-menu")).toBeNull();
  });

  it("enabled rows navigate to the shipped panel for the item", async () => {
    fetchActions.mockResolvedValue([{ name: "Publish", enabled: true }]);
    const onItemIdChange = vi.fn();
    render(
      <div>
        <ItemPublishingActionsMenu
          itemId="42"
          onItemIdChange={onItemIdChange}
        />
        <section data-testid="item-publish-now">panel</section>
      </div>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-action-publish-now")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("item-action-publish-now"));
    expect(onItemIdChange).toHaveBeenCalledWith("42");
  });
});

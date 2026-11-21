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
import { CheckoutOwnerPanel } from "../../../main/ts/contentExplorer/CheckoutOwnerPanel";
import { renderA11yGate } from "./a11y";

describe("CheckoutOwnerPanel (#4910)", () => {
  it("shows the checkout user", async () => {
    render(
      <CheckoutOwnerPanel
        itemId="42"
        load={vi.fn().mockResolvedValue({
          checkOutUser: "editor",
          currentUser: "Admin",
          itemName: "Home",
        })}
      />,
    );
    expect(await screen.findByTestId("explorer-checkout-owner-user")).toHaveTextContent(
      "editor",
    );
  });

  it("shows none when not checked out", async () => {
    render(
      <CheckoutOwnerPanel
        itemId="42"
        load={vi.fn().mockResolvedValue({
          checkOutUser: "",
          currentUser: "Admin",
          itemName: "Home",
        })}
      />,
    );
    expect(await screen.findByTestId("explorer-checkout-owner-none")).toBeTruthy();
  });

  it("keeps HTTP 403 on the panel", async () => {
    render(
      <CheckoutOwnerPanel
        itemId="42"
        load={vi.fn().mockRejectedValue({ status: 403, statusText: "Forbidden" })}
      />,
    );
    const err = await screen.findByTestId("explorer-checkout-owner-error");
    expect(err.textContent).toMatch(/not allowed/i);
  });

  it("does not render for a folder (null id)", () => {
    const { container } = render(<CheckoutOwnerPanel itemId={null} load={vi.fn()} />);
    expect(container.querySelector('[data-testid="explorer-checkout-owner"]')).toBeNull();
  });

  it("leaves the checkout user in place until reloadToken changes (#5199)", async () => {
    let released = false;
    const load = vi.fn(async () => ({
      checkOutUser: released ? "" : "editor",
      currentUser: "Admin",
      itemName: "Home",
    }));
    const { rerender } = render(
      <CheckoutOwnerPanel itemId="42" load={load} reloadToken={0} />,
    );
    expect(await screen.findByTestId("explorer-checkout-owner-user")).toHaveTextContent(
      "editor",
    );
    const callsWhileHeld = load.mock.calls.length;
    rerender(<CheckoutOwnerPanel itemId="42" load={load} reloadToken={0} />);
    expect(load.mock.calls.length).toBe(callsWhileHeld);
    expect(screen.getByTestId("explorer-checkout-owner-user")).toHaveTextContent("editor");
    released = true;
    rerender(<CheckoutOwnerPanel itemId="42" load={load} reloadToken={1} />);
    expect(await screen.findByTestId("explorer-checkout-owner-none")).toBeTruthy();
    expect(load.mock.calls.length).toBeGreaterThan(callsWhileHeld);
  });

  it("does not throw when the lookup rejects", async () => {
    render(
      <CheckoutOwnerPanel
        itemId="42"
        load={vi.fn().mockRejectedValue(new Error("boom"))}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-checkout-owner-error")).toBeTruthy();
    });
  });

  it("offers Retry on a failed lookup; Retry re-runs the lookup (#6871)", async () => {
    const load = vi
      .fn()
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden" })
      .mockResolvedValueOnce({
        checkOutUser: "editor",
        currentUser: "Admin",
        itemName: "Home",
      });
    const { container } = render(<CheckoutOwnerPanel itemId="42" load={load} />);
    const retry = await screen.findByTestId("explorer-checkout-owner-retry");
    expect(retry).toHaveTextContent(/retry/i);
    expect(load).toHaveBeenCalledTimes(1);
    fireEvent.click(retry);
    expect(await screen.findByTestId("explorer-checkout-owner-user")).toHaveTextContent(
      "editor",
    );
    expect(load).toHaveBeenCalledTimes(2);
    expect(load).toHaveBeenLastCalledWith("42");
    await renderA11yGate(container);
  });

  it("passes the a11y gate on the error state with Retry (#6871)", async () => {
    const { container } = render(
      <CheckoutOwnerPanel
        itemId="42"
        load={vi.fn().mockRejectedValue({ status: 403, statusText: "Forbidden" })}
      />,
    );
    await screen.findByTestId("explorer-checkout-owner-retry");
    await renderA11yGate(container);
  });

  it("keeps the error and returns focus to Retry when the retry fails (#6871)", async () => {
    const load = vi.fn().mockRejectedValue(new Error("still down"));
    render(<CheckoutOwnerPanel itemId="42" load={load} />);
    const retry = await screen.findByTestId("explorer-checkout-owner-retry");
    // Native button: keyboard-focusable and activated by Enter/Space.
    expect(retry.tagName).toBe("BUTTON");
    retry.focus();
    expect(retry).toHaveFocus();
    fireEvent.click(retry);
    await waitFor(() => {
      expect(load.mock.calls.length).toBeGreaterThanOrEqual(2);
    });
    const retryAgain = await screen.findByTestId("explorer-checkout-owner-retry");
    expect(
      screen.getByTestId("explorer-checkout-owner-error"),
    ).toBeTruthy();
    await waitFor(() => {
      expect(retryAgain).toHaveFocus();
    });
  });
});

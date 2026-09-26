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

import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CheckoutOwnerPanel } from "../../../main/ts/contentExplorer/CheckoutOwnerPanel";

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
});

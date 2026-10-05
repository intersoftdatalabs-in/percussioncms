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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DeliveryTypesPanel } from "@/publishing/design/DeliveryTypesPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { DELIVERY_TYPE_IN_USE } from "@/publishing/deliveryTypeDelete";

const deleteDeliveryType = vi.fn();
const listDeliveryTypes = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listDeliveryTypes: (...args: unknown[]) => listDeliveryTypes(...args),
  createDeliveryType: vi.fn(),
  updateDeliveryType: vi.fn(),
  deleteDeliveryType: (...args: unknown[]) => deleteDeliveryType(...args),
}));

const unused = {
  deliveryTypeId: "8",
  name: "nightonly",
  beanName: "sys_fileDeliveryHandler",
  description: "disposable",
};
const keep = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "built in",
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DeliveryTypesPanel />
    </DirtyFormProvider>,
  );
}

describe("DeliveryTypesPanel delete", () => {
  beforeEach(() => {
    deleteDeliveryType.mockReset();
    listDeliveryTypes.mockReset();
    listDeliveryTypes.mockResolvedValue([unused, keep]);
    vi.restoreAllMocks();
  });

  it("removes one delivery type only after delete succeeds", async () => {
    let resolveDelete: () => void = () => undefined;
    deleteDeliveryType.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveDelete = resolve;
        }),
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("delivery-type-delete").length).toBe(2));
    const listCallsBefore = listDeliveryTypes.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("delivery-type-delete")[0]);
    await waitFor(() => expect(deleteDeliveryType).toHaveBeenCalledWith("8"));
    expect(deleteDeliveryType).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "nightonly" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(listDeliveryTypes.mock.calls.length).toBe(listCallsBefore);
    listDeliveryTypes.mockResolvedValueOnce([keep]);
    resolveDelete();
    await waitFor(() => expect(screen.queryByRole("button", { name: "nightonly" })).toBeNull());
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("cancel does not call the server", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("delivery-type-delete").length).toBe(2));
    fireEvent.click(screen.getAllByTestId("delivery-type-delete")[0]);
    expect(deleteDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "nightonly" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it.each([
    [400, "Bad Request", "deliveryTypeId is required"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", DELIVERY_TYPE_IN_USE],
  ])("HTTP %s does not claim the delivery type was deleted", async (status, statusText, message) => {
    deleteDeliveryType.mockRejectedValue({ status, statusText, body: { message } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("delivery-type-delete").length).toBe(2));
    const listCallsBefore = listDeliveryTypes.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("delivery-type-delete")[0]);
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByRole("button", { name: "nightonly" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(listDeliveryTypes.mock.calls.length).toBe(listCallsBefore);
    expect(deleteDeliveryType).toHaveBeenCalledTimes(1);
  });

  it("drops a numeric wire id after delete succeeds", async () => {
    const numericUnused = { ...unused, deliveryTypeId: 10001 };
    const numericKeep = { ...keep, deliveryTypeId: 1 };
    listDeliveryTypes.mockResolvedValue([numericUnused, numericKeep]);
    deleteDeliveryType.mockResolvedValue(undefined);
    listDeliveryTypes.mockResolvedValueOnce([numericUnused, numericKeep]);
    listDeliveryTypes.mockResolvedValueOnce([numericKeep]);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("delivery-type-delete").length).toBe(2));
    fireEvent.click(screen.getAllByTestId("delivery-type-delete")[0]);
    await waitFor(() => expect(screen.queryByRole("button", { name: "nightonly" })).toBeNull());
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
    expect(deleteDeliveryType).toHaveBeenCalledWith(10001);
  });

  it("drops the delivery type when the refresh after success fails", async () => {
    let failRefresh = false;
    deleteDeliveryType.mockImplementation(async () => {
      failRefresh = true;
    });
    listDeliveryTypes.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [unused, keep];
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("delivery-type-delete").length).toBe(2));
    fireEvent.click(screen.getAllByTestId("delivery-type-delete")[0]);
    await waitFor(() => expect(screen.queryByRole("button", { name: "nightonly" })).toBeNull());
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });
});

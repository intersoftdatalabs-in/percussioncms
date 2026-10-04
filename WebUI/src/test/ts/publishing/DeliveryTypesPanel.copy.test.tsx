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
import { DELIVERY_TYPE_NAME_MAX_LENGTH } from "@/publishing/deliveryTypeCopy";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const createDeliveryType = vi.fn();
const listDeliveryTypes = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createDeliveryType: (...args: unknown[]) => createDeliveryType(...args),
  updateDeliveryType: vi.fn(),
  deleteDeliveryType: vi.fn(),
  listDeliveryTypes: (...args: unknown[]) => listDeliveryTypes(...args),
}));

const source = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "Publish content to the filesystem",
  unpublishingRequiresAssembly: true,
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DeliveryTypesPanel />
    </DirtyFormProvider>,
  );
}

async function openCopy(): Promise<void> {
  listDeliveryTypes.mockResolvedValue([source]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("delivery-type-copy")).toBeTruthy());
  fireEvent.click(screen.getByTestId("delivery-type-copy"));
  await waitFor(() => expect(screen.getByTestId("delivery-type-copy-form")).toBeTruthy());
}

describe("DeliveryTypesPanel copy", () => {
  beforeEach(() => {
    createDeliveryType.mockReset();
    listDeliveryTypes.mockReset();
  });

  it("creates a copy and lists the new name only after success", async () => {
    let resolveCreate: (value: unknown) => void = () => undefined;
    createDeliveryType.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    await openCopy();
    expect(screen.getByLabelText(/New name/i)).toHaveValue("filesystem copy");
    expect(screen.getByTestId("delivery-type-copy-bean")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-copy-description")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    const listCallsBefore = listDeliveryTypes.mock.calls.length;
    fireEvent.click(screen.getByTestId("delivery-type-copy-submit"));
    await waitFor(() => expect(createDeliveryType).toHaveBeenCalledTimes(1));
    expect(screen.getByTestId("delivery-type-copy-form")).toBeTruthy();
    expect(screen.queryByTestId("delivery-types-panel")).toBeNull();
    expect(listDeliveryTypes.mock.calls.length).toBe(listCallsBefore);
    expect(createDeliveryType).toHaveBeenCalledWith({
      name: "filesystem copy",
      beanName: "sys_fileDeliveryHandler",
      description: "Publish content to the filesystem",
      unpublishingRequiresAssembly: true,
    });
    listDeliveryTypes.mockResolvedValueOnce([
      source,
      {
        deliveryTypeId: "9",
        name: "filesystem copy",
        beanName: "sys_fileDeliveryHandler",
      },
    ]);
    resolveCreate({
      deliveryTypeId: "9",
      name: "filesystem copy",
      beanName: "sys_fileDeliveryHandler",
    });
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-copy-form")).toBeNull(),
    );
    expect(screen.getByTestId("delivery-types-panel")).toBeTruthy();
    expect(screen.getByRole("button", { name: "filesystem copy" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
  });

  it("rejects a blank name before any request", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("delivery-type-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(createDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-type-copy-form")).toBeTruthy();
  });

  it("rejects an overlong name before any request", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("delivery-type-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Delivery type name must be 50 characters or fewer",
    );
    expect(createDeliveryType).not.toHaveBeenCalled();
  });

  it("cancel does not call the server", async () => {
    await openCopy();
    fireEvent.click(screen.getByTestId("delivery-type-copy-cancel"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-copy-form")).toBeNull(),
    );
    expect(createDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "filesystem copy" })).toBeNull();
  });

  it("cancel after an edit does not POST when the operator confirms discard", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "other" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-copy-cancel"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-copy-form")).toBeNull(),
    );
    expect(createDeliveryType).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: "other" })).toBeNull();
  });

  it.each([
    [400, "Bad Request", "Delivery type name must be 50 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Delivery type name already exists"],
  ])(
    "HTTP %s does not claim the delivery type was copied",
    async (status, statusText, message) => {
      createDeliveryType.mockRejectedValue({ status, statusText, body: { message } });
      await openCopy();
      const listCallsBefore = listDeliveryTypes.mock.calls.length;
      fireEvent.click(screen.getByTestId("delivery-type-copy-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("delivery-type-copy-form")).toBeTruthy();
      expect(listDeliveryTypes.mock.calls.length).toBe(listCallsBefore);
      expect(screen.queryByRole("button", { name: "filesystem copy" })).toBeNull();
    },
  );

  it("lists the created type when the refresh after success fails", async () => {
    let failRefresh = false;
    createDeliveryType.mockResolvedValue({
      deliveryTypeId: "9",
      name: "filesystem copy",
      beanName: "sys_fileDeliveryHandler",
    });
    listDeliveryTypes.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-copy")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-copy"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-copy-form")).toBeTruthy());
    failRefresh = true;
    fireEvent.click(screen.getByTestId("delivery-type-copy-submit"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-copy-form")).toBeNull(),
    );
    expect(screen.getByRole("button", { name: "filesystem copy" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
  });
});

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
import { DeliveryTypesPanel } from "@/publishing/design/DeliveryTypesPanel";

const createDeliveryType = vi.fn();
const updateDeliveryType = vi.fn();
const listDeliveryTypes = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createDeliveryType: (...args: unknown[]) => createDeliveryType(...args),
  updateDeliveryType: (...args: unknown[]) => updateDeliveryType(...args),
  deleteDeliveryType: vi.fn(),
  listDeliveryTypes: (...args: unknown[]) => listDeliveryTypes(...args),
}));

describe("DeliveryTypesPanel save", () => {
  it("creates a new delivery type then returns to the list", async () => {
    listDeliveryTypes.mockResolvedValue([]);
    createDeliveryType.mockResolvedValue({
      deliveryTypeId: "12",
      name: "NightDt",
      beanName: "sys_fileDeliveryType",
    });
    render(<DeliveryTypesPanel />);
    await waitFor(() => expect(listDeliveryTypes).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("design-add-delivery-type"));
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "NightDt" },
    });
    fireEvent.change(screen.getByLabelText("* Bean name"), {
      target: { value: "sys_fileDeliveryType" },
    });
    fireEvent.click(screen.getByTestId("delivery-type-save"));
    await waitFor(() => expect(createDeliveryType).toHaveBeenCalled());
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-editor")).not.toBeInTheDocument(),
    );
  });

  it("shows 409 conflict on the editor", async () => {
    listDeliveryTypes.mockResolvedValue([]);
    createDeliveryType.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Delivery type name already exists" },
    });
    render(<DeliveryTypesPanel />);
    await waitFor(() => expect(listDeliveryTypes).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("design-add-delivery-type"));
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "Dup" },
    });
    fireEvent.change(screen.getByLabelText("* Bean name"), {
      target: { value: "sys_fileDeliveryType" },
    });
    fireEvent.click(screen.getByTestId("delivery-type-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Delivery type name already exists",
    );
  });
});

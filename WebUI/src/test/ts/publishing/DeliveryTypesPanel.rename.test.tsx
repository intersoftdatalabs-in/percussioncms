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
import { DELIVERY_TYPE_NAME_MAX_LENGTH } from "@/publishing/deliveryTypeRename";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const updateDeliveryType = vi.fn();
const listDeliveryTypes = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createDeliveryType: vi.fn(),
  updateDeliveryType: (...args: unknown[]) => updateDeliveryType(...args),
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

async function openRename(): Promise<void> {
  listDeliveryTypes.mockResolvedValue([source]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("delivery-type-rename")).toBeTruthy());
  fireEvent.click(screen.getByTestId("delivery-type-rename"));
  await waitFor(() => expect(screen.getByTestId("delivery-type-rename-form")).toBeTruthy());
}

describe("DeliveryTypesPanel rename", () => {
  beforeEach(() => {
    updateDeliveryType.mockReset();
    listDeliveryTypes.mockReset();
  });

  it("shows the new name only after the rename reload, with the same bean name", async () => {
    let releaseUpdate: (value: unknown) => void = () => undefined;
    updateDeliveryType.mockImplementation(
      () =>
        new Promise((resolve) => {
          releaseUpdate = resolve;
        }),
    );
    let releaseList: ((rows: unknown) => void) | undefined;
    let listed = false;
    listDeliveryTypes.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!listed) {
            resolve([source]);
            return;
          }
          releaseList = resolve;
        }),
    );

    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-rename")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    fireEvent.click(screen.getByTestId("delivery-type-rename"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-rename-form")).toBeTruthy());
    expect(screen.getByLabelText("* Name")).toHaveValue("filesystem");
    expect(screen.getByTestId("delivery-type-rename-bean")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-rename-description")).toHaveTextContent(
      "Publish content to the filesystem",
    );

    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "Renamed" },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("delivery-type-rename-submit"));
    await waitFor(() => expect(updateDeliveryType).toHaveBeenCalledTimes(1));
    expect(updateDeliveryType).toHaveBeenCalledWith("1", { name: "Renamed" });
    expect(screen.getByTestId("delivery-type-rename-form")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Renamed" })).toBeNull();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(1);

    releaseUpdate({ ...source, name: "Renamed" });
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-rename-form")).toBeNull(),
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Renamed" })).toBeNull();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, name: "Renamed" }]);
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Renamed" })).toBeTruthy(),
    );
    expect(screen.queryByRole("button", { name: "filesystem" })).toBeNull();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it("rejects a blank or overlong name without calling the server", async () => {
    await openRename();
    const loads = listDeliveryTypes.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: " " } });
    fireEvent.click(screen.getByTestId("delivery-type-rename-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("delivery-type-rename-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Delivery type name must be 50 characters or fewer",
    );
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-rename-cancel"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-rename-form")).toBeNull(),
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it("cancel keeps the old name and does not update", async () => {
    await openRename();
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Nope" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-rename-cancel"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-rename-form")).toBeNull(),
    );
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Nope" })).toBeNull();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it.each([
    [400, "Bad Request", "Delivery type name must be 50 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Delivery type name already exists"],
  ])(
    "keeps the previous name when HTTP %s rejects the rename",
    async (status, statusText, message) => {
      updateDeliveryType.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openRename();
      const loads = listDeliveryTypes.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Taken" } });
      fireEvent.click(screen.getByTestId("delivery-type-rename-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("delivery-type-rename-form")).toBeTruthy();
      expect(screen.queryByRole("button", { name: "Taken" })).toBeNull();
      expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("delivery-type-rename-cancel"));
      await waitFor(() =>
        expect(screen.queryByTestId("delivery-type-rename-form")).toBeNull(),
      );
      expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
      expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
        "sys_fileDeliveryHandler",
      );
    },
  );

  it("keeps the bean name when the reload after success fails", async () => {
    updateDeliveryType.mockResolvedValue({ ...source, name: "Renamed" });
    let failRefresh = false;
    listDeliveryTypes.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-rename")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-rename"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-rename-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Renamed" } });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("delivery-type-rename-submit"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-rename-form")).toBeNull(),
    );
    expect(screen.getByRole("button", { name: "Renamed" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "filesystem" })).toBeNull();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });
});

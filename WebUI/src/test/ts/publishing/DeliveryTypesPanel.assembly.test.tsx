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
  unpublishingRequiresAssembly: false,
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <DeliveryTypesPanel />
    </DirtyFormProvider>,
  );
}

async function openAssemblyForm(
  row: typeof source = source,
): Promise<void> {
  listDeliveryTypes.mockResolvedValue([row]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("delivery-type-set-assembly")).toBeTruthy());
  fireEvent.click(screen.getByTestId("delivery-type-set-assembly"));
  await waitFor(() => expect(screen.getByTestId("delivery-type-assembly-form")).toBeTruthy());
}

describe("DeliveryTypesPanel unpublish assembly flag", () => {
  beforeEach(() => {
    updateDeliveryType.mockReset();
    listDeliveryTypes.mockReset();
  });

  it("shows the new flag only after reload and keeps name, description, and bean", async () => {
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
    await waitFor(() => expect(screen.getByTestId("delivery-type-set-assembly")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("No");
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    fireEvent.click(screen.getByTestId("delivery-type-set-assembly"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-assembly-form")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-assembly-form-name")).toHaveTextContent(
      "filesystem",
    );
    expect(screen.getByTestId("delivery-type-assembly-form-bean")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-assembly-form-description")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(screen.getByTestId("delivery-type-assembly-flag")).not.toBeChecked();

    fireEvent.click(screen.getByTestId("delivery-type-assembly-flag"));
    listed = true;
    fireEvent.click(screen.getByTestId("delivery-type-assembly-submit"));
    await waitFor(() => expect(updateDeliveryType).toHaveBeenCalledTimes(1));
    expect(updateDeliveryType).toHaveBeenCalledWith("1", {
      unpublishingRequiresAssembly: true,
    });
    expect(screen.getByTestId("delivery-type-assembly-form")).toBeTruthy();
    expect(screen.queryByTestId("delivery-type-assembly-1")).toBeNull();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(1);

    releaseUpdate({ ...source, unpublishingRequiresAssembly: true });
    await waitFor(() => expect(screen.queryByTestId("delivery-type-assembly-form")).toBeNull());
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("No");
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, unpublishingRequiresAssembly: true }]);
    await waitFor(() =>
      expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("Yes"),
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });

  it("treats a missing flag as off and can save explicit false", async () => {
    const missing = {
      deliveryTypeId: "1",
      name: "filesystem",
      beanName: "sys_fileDeliveryHandler",
      description: "Publish content to the filesystem",
    };
    updateDeliveryType.mockResolvedValue({
      ...missing,
      unpublishingRequiresAssembly: false,
    });
    listDeliveryTypes.mockResolvedValue([missing]);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("No"));
    fireEvent.click(screen.getByTestId("delivery-type-set-assembly"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-assembly-flag")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-assembly-flag")).not.toBeChecked();
    fireEvent.click(screen.getByTestId("delivery-type-assembly-submit"));
    await waitFor(() => expect(updateDeliveryType).toHaveBeenCalledTimes(1));
    expect(updateDeliveryType).toHaveBeenCalledWith("1", {
      unpublishingRequiresAssembly: false,
    });
  });

  it("cancel keeps the old flag and does not update", async () => {
    await openAssemblyForm();
    fireEvent.click(screen.getByTestId("delivery-type-assembly-flag"));
    expect(screen.getByTestId("delivery-type-assembly-flag")).toBeChecked();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-assembly-cancel"));
    await waitFor(() => expect(screen.queryByTestId("delivery-type-assembly-form")).toBeNull());
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("No");
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });

  it.each([
    [400, "Bad Request", "Bad Request"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Delivery type name already exists"],
  ])(
    "keeps the previous flag when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateDeliveryType.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openAssemblyForm({ ...source, unpublishingRequiresAssembly: true });
      const loads = listDeliveryTypes.mock.calls.length;
      expect(screen.getByTestId("delivery-type-assembly-flag")).toBeChecked();
      fireEvent.click(screen.getByTestId("delivery-type-assembly-flag"));
      fireEvent.click(screen.getByTestId("delivery-type-assembly-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("delivery-type-assembly-form")).toBeTruthy();
      expect(screen.queryByTestId("delivery-type-assembly-1")).toBeNull();
      expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("delivery-type-assembly-cancel"));
      await waitFor(() => expect(screen.queryByTestId("delivery-type-assembly-form")).toBeNull());
      expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("Yes");
      expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
      expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
        "sys_fileDeliveryHandler",
      );
      expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
        "Publish content to the filesystem",
      );
    },
  );

  it("keeps the name, description, and bean when the reload after success fails", async () => {
    updateDeliveryType.mockResolvedValue({
      ...source,
      unpublishingRequiresAssembly: true,
    });
    let failRefresh = false;
    listDeliveryTypes.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-set-assembly")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-set-assembly"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-assembly-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-assembly-flag"));
    failRefresh = true;
    fireEvent.click(screen.getByTestId("delivery-type-assembly-submit"));
    await waitFor(() => expect(screen.queryByTestId("delivery-type-assembly-form")).toBeNull());
    expect(screen.getByTestId("delivery-type-assembly-1")).toHaveTextContent("Yes");
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });
});

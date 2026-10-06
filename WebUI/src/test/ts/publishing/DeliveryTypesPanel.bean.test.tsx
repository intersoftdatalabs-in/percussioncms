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
import { DELIVERY_TYPE_BEAN_NAME_MAX_LENGTH } from "@/publishing/deliveryTypeBean";
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

async function openBeanForm(): Promise<void> {
  listDeliveryTypes.mockResolvedValue([source]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("delivery-type-set-bean")).toBeTruthy());
  fireEvent.click(screen.getByTestId("delivery-type-set-bean"));
  await waitFor(() => expect(screen.getByTestId("delivery-type-bean-form")).toBeTruthy());
}

describe("DeliveryTypesPanel bean name", () => {
  beforeEach(() => {
    updateDeliveryType.mockReset();
    listDeliveryTypes.mockReset();
  });

  it("shows the new bean name only after reload and keeps the name and description", async () => {
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
    await waitFor(() => expect(screen.getByTestId("delivery-type-set-bean")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    fireEvent.click(screen.getByTestId("delivery-type-set-bean"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-bean-form")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-bean-form-name")).toHaveTextContent("filesystem");
    expect(screen.getByTestId("delivery-type-bean-form-description")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(screen.getByLabelText("* Bean name")).toHaveValue("sys_fileDeliveryHandler");

    fireEvent.change(screen.getByLabelText("* Bean name"), {
      target: { value: "  sys_ftpDeliveryHandler  " },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("delivery-type-bean-submit"));
    await waitFor(() => expect(updateDeliveryType).toHaveBeenCalledTimes(1));
    expect(updateDeliveryType).toHaveBeenCalledWith("1", {
      beanName: "sys_ftpDeliveryHandler",
    });
    expect(screen.getByTestId("delivery-type-bean-form")).toBeTruthy();
    expect(screen.queryByTestId("delivery-type-bean-1")).toBeNull();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(1);

    releaseUpdate({ ...source, beanName: "sys_ftpDeliveryHandler" });
    await waitFor(() => expect(screen.queryByTestId("delivery-type-bean-form")).toBeNull());
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, beanName: "sys_ftpDeliveryHandler" }]);
    await waitFor(() =>
      expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
        "sys_ftpDeliveryHandler",
      ),
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });

  it("rejects a blank bean name without calling the server", async () => {
    await openBeanForm();
    const loads = listDeliveryTypes.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Bean name"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("delivery-type-bean-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Bean name is required");
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-bean-cancel"));
    await waitFor(() => expect(screen.queryByTestId("delivery-type-bean-form")).toBeNull());
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });

  it("rejects an overlong bean name without calling the server", async () => {
    await openBeanForm();
    const loads = listDeliveryTypes.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Bean name"), {
      target: { value: "b".repeat(DELIVERY_TYPE_BEAN_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("delivery-type-bean-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Delivery type bean name must be 255 characters or fewer",
    );
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
  });

  it("cancel keeps the old bean name and does not update", async () => {
    await openBeanForm();
    fireEvent.change(screen.getByLabelText("* Bean name"), {
      target: { value: "sys_willNotSave" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-bean-cancel"));
    await waitFor(() => expect(screen.queryByTestId("delivery-type-bean-form")).toBeNull());
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });

  it.each([
    [400, "Bad Request", "Bean name is required"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Delivery type name already exists"],
  ])(
    "keeps the previous bean name when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateDeliveryType.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openBeanForm();
      const loads = listDeliveryTypes.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Bean name"), {
        target: { value: "sys_willNotStick" },
      });
      fireEvent.click(screen.getByTestId("delivery-type-bean-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("delivery-type-bean-form")).toBeTruthy();
      expect(screen.queryByTestId("delivery-type-bean-1")).toBeNull();
      expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("delivery-type-bean-cancel"));
      await waitFor(() => expect(screen.queryByTestId("delivery-type-bean-form")).toBeNull());
      expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
        "sys_fileDeliveryHandler",
      );
      expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
      expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
        "Publish content to the filesystem",
      );
    },
  );

  it("keeps the name and description when the reload after success fails", async () => {
    updateDeliveryType.mockResolvedValue({
      ...source,
      beanName: "sys_ftpDeliveryHandler",
    });
    let failRefresh = false;
    listDeliveryTypes.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-set-bean")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-set-bean"));
    await waitFor(() => expect(screen.getByTestId("delivery-type-bean-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("* Bean name"), {
      target: { value: "sys_ftpDeliveryHandler" },
    });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("delivery-type-bean-submit"));
    await waitFor(() => expect(screen.queryByTestId("delivery-type-bean-form")).toBeNull());
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_ftpDeliveryHandler",
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
  });
});

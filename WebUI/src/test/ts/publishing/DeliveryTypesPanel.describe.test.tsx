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
import { DELIVERY_TYPE_DESCRIPTION_MAX_LENGTH } from "@/publishing/deliveryTypeDescription";
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

async function openDescribe(): Promise<void> {
  listDeliveryTypes.mockResolvedValue([source]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("delivery-type-describe")).toBeTruthy());
  fireEvent.click(screen.getByTestId("delivery-type-describe"));
  await waitFor(() =>
    expect(screen.getByTestId("delivery-type-describe-form")).toBeTruthy(),
  );
}

describe("DeliveryTypesPanel description", () => {
  beforeEach(() => {
    updateDeliveryType.mockReset();
    listDeliveryTypes.mockReset();
  });

  it("shows the new description only after reload and keeps the name and bean", async () => {
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
    await waitFor(() => expect(screen.getByTestId("delivery-type-describe")).toBeTruthy());
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    fireEvent.click(screen.getByTestId("delivery-type-describe"));
    await waitFor(() =>
      expect(screen.getByTestId("delivery-type-describe-form")).toBeTruthy(),
    );
    expect(screen.getByTestId("delivery-type-describe-name")).toHaveTextContent("filesystem");
    expect(screen.getByTestId("delivery-type-describe-bean")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(screen.getByLabelText("Description")).toHaveValue(
      "Publish content to the filesystem",
    );

    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "  Night notes  " },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("delivery-type-describe-submit"));
    await waitFor(() => expect(updateDeliveryType).toHaveBeenCalledTimes(1));
    expect(updateDeliveryType).toHaveBeenCalledWith("1", { description: "Night notes" });
    expect(screen.getByTestId("delivery-type-describe-form")).toBeTruthy();
    expect(screen.queryByTestId("delivery-type-description-1")).toBeNull();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(1);

    releaseUpdate({ ...source, description: "Night notes" });
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-describe-form")).toBeNull(),
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, description: "Night notes" }]);
    await waitFor(() =>
      expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
        "Night notes",
      ),
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it("clears a blank description only after reload", async () => {
    updateDeliveryType.mockResolvedValue({ ...source, description: "" });
    listDeliveryTypes.mockResolvedValue([source]);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-describe")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-describe"));
    await waitFor(() =>
      expect(screen.getByTestId("delivery-type-describe-form")).toBeTruthy(),
    );
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "   " } });
    listDeliveryTypes.mockResolvedValueOnce([{ ...source, description: "" }]);
    fireEvent.click(screen.getByTestId("delivery-type-describe-submit"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-describe-form")).toBeNull(),
    );
    expect(updateDeliveryType).toHaveBeenCalledWith("1", { description: "" });
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent("");
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it("rejects an overlong description without calling the server", async () => {
    await openDescribe();
    const loads = listDeliveryTypes.mock.calls.length;
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "d".repeat(DELIVERY_TYPE_DESCRIPTION_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("delivery-type-describe-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Delivery type description must be 255 characters or fewer",
    );
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-describe-cancel"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-describe-form")).toBeNull(),
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it("cancel keeps the old description and does not update", async () => {
    await openDescribe();
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "Will not save" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("delivery-type-describe-cancel"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-describe-form")).toBeNull(),
    );
    expect(updateDeliveryType).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
      "Publish content to the filesystem",
    );
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });

  it.each([
    [400, "Bad Request", "Delivery type description must be 255 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Delivery type name already exists"],
  ])(
    "keeps the previous description when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateDeliveryType.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openDescribe();
      const loads = listDeliveryTypes.mock.calls.length;
      fireEvent.change(screen.getByLabelText("Description"), {
        target: { value: "Will not stick" },
      });
      fireEvent.click(screen.getByTestId("delivery-type-describe-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("delivery-type-describe-form")).toBeTruthy();
      expect(screen.queryByTestId("delivery-type-description-1")).toBeNull();
      expect(listDeliveryTypes).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("delivery-type-describe-cancel"));
      await waitFor(() =>
        expect(screen.queryByTestId("delivery-type-describe-form")).toBeNull(),
      );
      expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent(
        "Publish content to the filesystem",
      );
      expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
      expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
        "sys_fileDeliveryHandler",
      );
    },
  );

  it("keeps the name and bean when the reload after success fails", async () => {
    updateDeliveryType.mockResolvedValue({ ...source, description: "Night notes" });
    let failRefresh = false;
    listDeliveryTypes.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source];
    });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("delivery-type-describe")).toBeTruthy());
    fireEvent.click(screen.getByTestId("delivery-type-describe"));
    await waitFor(() =>
      expect(screen.getByTestId("delivery-type-describe-form")).toBeTruthy(),
    );
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "Night notes" },
    });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("delivery-type-describe-submit"));
    await waitFor(() =>
      expect(screen.queryByTestId("delivery-type-describe-form")).toBeNull(),
    );
    expect(screen.getByTestId("delivery-type-description-1")).toHaveTextContent("Night notes");
    expect(screen.getByRole("button", { name: "filesystem" })).toBeTruthy();
    expect(screen.getByTestId("delivery-type-bean-1")).toHaveTextContent(
      "sys_fileDeliveryHandler",
    );
  });
});

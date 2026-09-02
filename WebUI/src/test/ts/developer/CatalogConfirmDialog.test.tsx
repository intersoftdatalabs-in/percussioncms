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

import { fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { describe, expect, it, vi } from "vitest";
import { CatalogConfirmDialog } from "../../../main/ts/developer/CatalogConfirmDialog";
import { DEV_MSG } from "../../../main/ts/developer/messages";

describe("CatalogConfirmDialog (#4122)", () => {
  it("renders an in-app modal with the catalog message", () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    render(
      <CatalogConfirmDialog
        open
        busy={false}
        message={DEV_MSG.SR_DELETE_CONFIRM}
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    );
    const dialog = screen.getByTestId("developer-catalog-confirm-dialog");
    expect(dialog.getAttribute("role")).toBe("dialog");
    expect(dialog.getAttribute("aria-modal")).toBe("true");
    expect(screen.getByTestId("developer-catalog-confirm-body").textContent).toBe(
      DEV_MSG.SR_DELETE_CONFIRM,
    );
    expect(screen.getByTestId("developer-catalog-confirm-submit").textContent).toBe(
      DEV_MSG.CATALOG_CONFIRM_SUBMIT,
    );
  });

  it("does not render when closed", () => {
    render(
      <CatalogConfirmDialog
        open={false}
        busy={false}
        message={DEV_MSG.SR_DELETE_CONFIRM}
        onCancel={() => undefined}
        onConfirm={() => undefined}
      />,
    );
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
  });

  it("cancel and confirm call the matching handlers", () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    render(
      <CatalogConfirmDialog
        open
        busy={false}
        message={DEV_MSG.SR_DELETE_CONFIRM}
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    );
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("Escape cancels when not busy", () => {
    const onCancel = vi.fn();
    render(
      <CatalogConfirmDialog
        open
        busy={false}
        message={DEV_MSG.SR_DELETE_CONFIRM}
        onCancel={onCancel}
        onConfirm={() => undefined}
      />,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});

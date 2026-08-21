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

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { CreateSectionDialog } from "../../../main/ts/architecture/CreateSectionDialog";
import * as homeApi from "../../../main/ts/api/home/homeApi";

describe("CreateSectionDialog (#3661 / #3350 / #3155)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
    vi.spyOn(homeApi, "fetchTemplatesForSectionCreate").mockResolvedValue([
      { id: "tpl-1", name: "Base" },
    ]);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders a modal dialog and Escape closes when not busy", async () => {
    const onCancel = vi.fn();
    render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={onCancel}
        onSubmit={() => undefined}
      />,
    );
    const panel = await screen.findByRole("dialog");
    expect(panel.getAttribute("aria-modal")).toBe("true");
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-create-template-select"),
      ).toBeTruthy();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("does not close on Escape while busy", async () => {
    const onCancel = vi.fn();
    render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy
        onCancel={onCancel}
        onSubmit={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeTruthy();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).not.toHaveBeenCalled();
  });

  it("restores focus to the opener after Escape (#3350)", async () => {
    const onCancel = vi.fn();
    const opener = document.createElement("button");
    opener.type = "button";
    opener.setAttribute("data-testid", "create-opener");
    document.body.appendChild(opener);
    opener.focus();

    const { rerender } = render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={onCancel}
        onSubmit={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeTruthy();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
    rerender(
      <CreateSectionDialog
        open={false}
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={onCancel}
        onSubmit={() => undefined}
      />,
    );
    expect(document.activeElement).toBe(opener);
    opener.remove();
  });

  it("submits landing page name, title, and template (#3661)", async () => {
    const onSubmit = vi.fn();
    render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-create-template-select"),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-create-title-input"), {
      target: { value: "Products" },
    });
    fireEvent.change(screen.getByTestId("architecture-create-url-input"), {
      target: { value: "products" },
    });
    fireEvent.change(
      screen.getByTestId("architecture-create-page-name-input"),
      { target: { value: "products.html" } },
    );
    fireEvent.change(screen.getByTestId("architecture-create-template-select"), {
      target: { value: "tpl-1" },
    });
    fireEvent.click(screen.getByTestId("architecture-create-submit"));
    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      title: "Products",
      urlName: "products",
      pageName: "products.html",
      templateId: "tpl-1",
    });
  });

  it("autofills landing page name from title until edited (#3661)", async () => {
    render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={() => undefined}
        onSubmit={() => undefined}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-create-page-name-input"),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-create-title-input"), {
      target: { value: "About Us" },
    });
    expect(
      (screen.getByTestId(
        "architecture-create-page-name-input",
      ) as HTMLInputElement).value,
    ).toBe("about-us.html");
    expect(
      (screen.getByTestId(
        "architecture-create-url-input",
      ) as HTMLInputElement).value,
    ).toBe("about-us");
  });

  it("does not submit when landing page name is empty (#3661)", async () => {
    const onSubmit = vi.fn();
    render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-create-template-select"),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-create-title-input"), {
      target: { value: "Products" },
    });
    fireEvent.change(screen.getByTestId("architecture-create-url-input"), {
      target: { value: "products" },
    });
    fireEvent.change(
      screen.getByTestId("architecture-create-page-name-input"),
      { target: { value: "" } },
    );
    const form = screen.getByRole("dialog").querySelector("form");
    expect(form).toBeTruthy();
    fireEvent.submit(form as HTMLFormElement);
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-create-error")).toBeTruthy();
  });

  it("cancel does not submit (#3661)", async () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();
    render(
      <CreateSectionDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        onCancel={onCancel}
        onSubmit={onSubmit}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("architecture-create-cancel")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("architecture-create-title-input"), {
      target: { value: "Products" },
    });
    fireEvent.click(screen.getByTestId("architecture-create-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});

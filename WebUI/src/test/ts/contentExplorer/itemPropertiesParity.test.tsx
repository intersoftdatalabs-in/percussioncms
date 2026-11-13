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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 008 US2 (T014): item-properties dialog parity — groups render with server
 * values (FR-004), permitted edits persist and show on reopen (FR-005),
 * denied edits message instead of omitting (FR-009).
 */
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ItemPropertiesPanel } from "../../../main/ts/contentExplorer/ItemPropertiesPanel";
import { renderA11yGate } from "./a11y";

const LOADED = {
  itemPath: "/Sites/Demo/Home",
  name: "Home",
  displayTitle: "Home page",
};

function renderPanel(overrides: Record<string, unknown> = {}) {
  let savedTitle: string | null = null;
  const load = vi.fn(async () => ({
    ...LOADED,
    displayTitle: savedTitle ?? LOADED.displayTitle,
  }));
  const save = vi.fn(async (body: {
    itemPath: string;
    name: string;
    displayTitle?: string | null;
  }) => {
    // Product flow reloads after save; the panel commits the reloaded
    // (server-truth) value, so the mock reload reflects the save.
    savedTitle = body.displayTitle ?? savedTitle;
    return { ...LOADED, displayTitle: savedTitle ?? LOADED.displayTitle };
  });
  const { container } = render(
    <ItemPropertiesPanel
      itemPath="/Sites/Demo/Home"
      itemName="Home"
      canEdit
      load={load}
      save={save}
      {...overrides}
    />,
  );
  return { container, load, save };
}

describe("item-properties groups (FR-004)", () => {
  it("renders General and Display groups with server values", async () => {
    renderPanel();
    expect(await screen.findByTestId("item-properties-general-group")).toBeTruthy();
    expect(await screen.findByTestId("item-properties-display-group")).toBeTruthy();
    expect(await screen.findByTestId("item-properties-path-value")).toHaveTextContent(
      "/Sites/Demo/Home",
    );
    expect(screen.getByTestId("item-properties-name")).toHaveValue("Home");
    expect(screen.getByTestId("item-properties-shown-display-title-value")).toHaveTextContent(
      "Home page",
    );
  });
});

describe("edit-persist round trip (FR-005)", () => {
  it("saves the edited title and shows the committed value", async () => {
    const { save } = renderPanel();
    const input = await screen.findByTestId("item-properties-display-title");
    fireEvent.change(input, { target: { value: "Home RENAMED" } });
    fireEvent.click(screen.getByTestId("item-properties-save"));
    await waitFor(() => expect(save).toHaveBeenCalledTimes(1));
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({ displayTitle: "Home RENAMED" }),
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("item-properties-shown-display-title-value"),
      ).toHaveTextContent("Home RENAMED"),
    );
  });
});

describe("denied edits (FR-009)", () => {
  it("renders read-only with disabled inputs when editing is not permitted", async () => {
    renderPanel({ canEdit: false });
    expect(await screen.findByTestId("item-properties-readonly")).toBeTruthy();
    expect(screen.getByTestId("item-properties-name")).toBeDisabled();
    expect(screen.getByTestId("item-properties-display-title")).toBeDisabled();
  });

  it("surfaces a clear message when the server rejects the save", async () => {
    const save = vi.fn(async () => {
      throw { status: 403, statusText: "Forbidden", body: "denied" };
    });
    renderPanel({ save });
    const input = await screen.findByTestId("item-properties-display-title");
    fireEvent.change(input, { target: { value: "Nope" } });
    fireEvent.click(screen.getByTestId("item-properties-save"));
    await waitFor(() =>
      expect(screen.getByTestId("item-properties-status")).toBeTruthy(),
    );
    // Previous committed value is kept, not silently replaced.
    expect(
      screen.getByTestId("item-properties-shown-display-title-value"),
    ).toHaveTextContent("Home page");
  });
});

describe("a11y gate (residual #6179)", () => {
  it("editable panel surface has no serious a11y violations", async () => {
    const { container } = renderPanel();
    expect(await screen.findByTestId("item-properties-general-group")).toBeTruthy();
    expect(await screen.findByTestId("item-properties-display-group")).toBeTruthy();
    await renderA11yGate(container);
  });

  it("read-only panel surface has no serious a11y violations", async () => {
    const { container } = renderPanel({ canEdit: false });
    expect(await screen.findByTestId("item-properties-readonly")).toBeTruthy();
    await renderA11yGate(container);
  });
});

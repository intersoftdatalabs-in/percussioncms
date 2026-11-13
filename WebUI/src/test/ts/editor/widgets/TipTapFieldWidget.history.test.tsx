/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TipTapFieldWidget } from "../../../../main/ts/editor/widgets/TipTapFieldWidget";

describe("TipTapFieldWidget external sync history (#6262)", () => {
  afterEach(() => {
    cleanup();
  });

  it("external value sync is silent and leaves toolbar undo a no-op", async () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <TipTapFieldWidget
        name="text"
        value="<p>One</p>"
        readOnly={false}
        onChange={onChange}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.textContent).toBe("One");

    // Programmatic sync (revert-all / compare-restore / autosave-restore /
    // save-reload): the widget shows the new value without echoing onChange.
    rerender(
      <TipTapFieldWidget
        name="text"
        value="<p>Two</p>"
        readOnly={false}
        onChange={onChange}
      />,
    );
    expect(editable.textContent).toBe("Two");
    expect(onChange).not.toHaveBeenCalled();

    // The sync pushed no undo step and cleared the stack: toolbar Undo is
    // disabled and clicking it cannot resurrect the pre-sync document.
    // (Vanilla DOM assertion — no jest-dom dependency, like the
    // contentExplorer ContextMenu suite.)
    const undo = (await screen.findByTestId(
      "tiptap-undo-text",
    )) as HTMLButtonElement;
    expect(undo.disabled).toBe(true);
    fireEvent.click(undo);
    expect(editable.textContent).toBe("Two");
  });
});

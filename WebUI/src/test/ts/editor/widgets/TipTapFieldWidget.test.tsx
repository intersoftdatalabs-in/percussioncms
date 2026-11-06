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

import { Editor } from "@tiptap/core";
import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";
import { TipTapFieldWidget } from "../../../../main/ts/editor/widgets/TipTapFieldWidget";

describe("TipTapFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders stored HTML in a ProseMirror surface", async () => {
    const onChange = vi.fn();
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={onChange}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("data-editor-kind")).toBe("html");
    expect(editable.getAttribute("data-editor-engine")).toBe("tiptap");
    expect(editable.getAttribute("contenteditable")).toBe("true");
    expect(editable.textContent).toContain("Hi");
  });

  it("reports HTML edits through onChange (headless command path)", () => {
    const seen: string[] = [];
    const editor = new Editor({
      extensions: createTipTapEditorExtensions(),
      content: "<p>Hi</p>",
      onUpdate: ({ editor: updated }) => {
        seen.push(updated.getHTML());
      },
    });
    try {
      // setContent only notifies when emitUpdate is true (core default false).
      editor.commands.setContent("<p>Bye</p>", true);
      expect(seen).toEqual(["<p>Bye</p>"]);
    } finally {
      editor.destroy();
    }
  });

  it("adopts externally saved values", async () => {
    const { rerender } = render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={vi.fn()}
      />,
    );
    await screen.findByText("Hi");
    rerender(
      <TipTapFieldWidget
        name="text"
        value="<p>Bye</p>"
        readOnly={false}
        onChange={vi.fn()}
      />,
    );
    await screen.findByText("Bye");
  });

  it("is non-editable in view mode", async () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("contenteditable")).toBe("false");
    expect(editable.getAttribute("readonly")).toBe("");
  });

  it("marks the surface invalid when the HTML is refused", async () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        invalid
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("aria-invalid")).toBe("true");
  });

  it("cancels surface clicks so the row label cannot forward them", async () => {
    // EditorHost wraps rows in an implicit <label>. A div is not labelable,
    // so a real browser forwards surface clicks to the first labelable
    // control (the Clear button) — clicking the editor used to wipe the
    // field (Playwright proof in editor-host-html-tiptap.spec.js). The
    // widget cancels the click default to stop the forwarding; ProseMirror
    // selection/focus runs on mousedown and is unaffected.
    const onChange = vi.fn();
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={onChange}
      />,
    );
    const host = await screen.findByTestId("editor-tiptap-text");
    const click = new MouseEvent("click", { bubbles: true, cancelable: true });
    host.dispatchEvent(click);
    expect(click.defaultPrevented).toBe(true);
  });

  it("renders the parity toolbar when editable", async () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={vi.fn()}
      />,
    );
    await screen.findByTestId("editor-tiptap-toolbar-text");
    expect(screen.getByTestId("tiptap-bullist-text")).toBeTruthy();
    expect(screen.getByTestId("tiptap-link-text")).toBeTruthy();
    expect(screen.getByTestId("tiptap-code-block-text")).toBeTruthy();
    expect(screen.getByTestId("tiptap-table-insert-text")).toBeTruthy();
  });

  it("hides the parity toolbar in view mode", async () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    await screen.findByTestId("editor-field-text");
    expect(screen.queryByTestId("editor-tiptap-toolbar-text")).toBeNull();
  });
});

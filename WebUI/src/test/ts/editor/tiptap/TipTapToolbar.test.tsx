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
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it } from "vitest";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";
import { TipTapToolbar } from "../../../../main/ts/editor/tiptap/TipTapToolbar";

function makeEditor(html = "<p>Hello</p>"): Editor {
  return new Editor({
    extensions: createTipTapEditorExtensions(),
    content: html,
  });
}

describe("TipTapToolbar", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders the parity toolbar groups", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      expect(screen.getByTestId("editor-tiptap-toolbar-text")).toBeTruthy();
      for (const action of [
        "undo",
        "redo",
        "bold",
        "italic",
        "underline",
        "bullist",
        "numlist",
        "outdent",
        "indent",
        "link",
        "unlink",
        "code-inline",
        "code-block",
        "table-insert",
        "table-add-row",
        "table-remove-row",
        "table-add-col",
        "table-remove-col",
        "table-delete",
      ]) {
        expect(screen.getByTestId(`tiptap-${action}-text`)).toBeTruthy();
      }
      expect(screen.getByTestId("tiptap-style-text")).toBeTruthy();
    } finally {
      editor.destroy();
    }
  });

  it("toggles bold on the selection", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      editor.commands.selectAll();
      fireEvent.click(screen.getByTestId("tiptap-bold-text"));
      expect(editor.getHTML()).toContain("<strong>Hello</strong>");
    } finally {
      editor.destroy();
    }
  });

  it("toggles a bullet list", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-bullist-text"));
      const html = editor.getHTML();
      expect(html).toContain("<ul>");
      expect(html).toContain("Hello");
    } finally {
      editor.destroy();
    }
  });

  it("toggles a code block", () => {
    const editor = makeEditor("<p>var x = 1;</p>");
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-code-block-text"));
      expect(editor.getHTML()).toContain("<pre><code>var x = 1;</code></pre>");
    } finally {
      editor.destroy();
    }
  });

  it("applies a heading style through the style select", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      fireEvent.change(screen.getByTestId("tiptap-style-text"), {
        target: { value: "h1" },
      });
      expect(editor.getHTML()).toContain("<h1>Hello</h1>");
    } finally {
      editor.destroy();
    }
  });

  it("centers the paragraph through the align control", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-align-center-text"));
      expect(editor.getHTML()).toContain("text-align: center");
    } finally {
      editor.destroy();
    }
  });

  it("applies a link address and refuses javascript: addresses", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      editor.commands.selectAll();
      fireEvent.click(screen.getByTestId("tiptap-link-text"));
      const input = screen.getByTestId("tiptap-link-url-text") as HTMLInputElement;
      fireEvent.change(input, { target: { value: "javascript:alert(1)" } });
      fireEvent.click(screen.getByTestId("tiptap-link-apply-text"));
      expect(screen.getByText("That link address cannot be used.")).toBeTruthy();
      expect(editor.getHTML()).not.toContain("<a");
      fireEvent.change(input, { target: { value: "https://example.com/x" } });
      fireEvent.click(screen.getByTestId("tiptap-link-apply-text"));
      expect(editor.getHTML()).toContain('<a href="https://example.com/x">Hello</a>');
    } finally {
      editor.destroy();
    }
  });

  it("inserts a table and removes a row", () => {
    const editor = makeEditor();
    try {
      const { rerender } = render(<TipTapToolbar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-table-insert-text"));
      // Production re-renders the toolbar on every editor transaction
      // (TipTapFieldWidget bumps state); refresh the command states here.
      rerender(<TipTapToolbar editor={editor} name="text" />);
      // TipTap 3 serializes column bookkeeping (colgroup/width styles);
      // the round-trip normalizer already accepts that representation.
      expect(editor.getHTML()).toContain("<table");
      const rowsBefore = (editor.getHTML().match(/<tr>/g) ?? []).length;
      expect(rowsBefore).toBe(3);
      fireEvent.click(screen.getByTestId("tiptap-table-remove-row-text"));
      const rowsAfter = (editor.getHTML().match(/<tr>/g) ?? []).length;
      expect(rowsAfter).toBe(rowsBefore - 1);
    } finally {
      editor.destroy();
    }
  });

  it("disables table row controls outside a table", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      expect(
        (screen.getByTestId("tiptap-table-remove-row-text") as HTMLButtonElement).disabled,
      ).toBe(true);
      expect(
        (screen.getByTestId("tiptap-table-delete-text") as HTMLButtonElement).disabled,
      ).toBe(true);
    } finally {
      editor.destroy();
    }
  });

  it("exposes full accessible names for glyph buttons (label-in-name)", () => {
    const editor = makeEditor();
    try {
      render(<TipTapToolbar editor={editor} name="text" />);
      // Accessible name comes from content (not aria-label), so the
      // visible glyph can never mismatch the name (axe label-in-name).
      expect(screen.getByRole("button", { name: "Bold" })).toBeTruthy();
      expect(screen.getByRole("button", { name: "Numbered list" })).toBeTruthy();
      expect(screen.getByRole("button", { name: "Insert table" })).toBeTruthy();
    } finally {
      editor.destroy();
    }
  });

  it("renders disabled controls before the editor mounts", () => {
    render(<TipTapToolbar editor={null} name="text" />);
    expect(
      (screen.getByTestId("tiptap-bold-text") as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(
      (screen.getByTestId("tiptap-bullist-text") as HTMLButtonElement).disabled,
    ).toBe(true);
  });
});

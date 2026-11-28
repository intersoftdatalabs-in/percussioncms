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

import { describe, expect, it } from "vitest";
import { Editor } from "@tiptap/core";
import {
  normalizeHtmlForRoundTrip,
  roundTripHtmlFragment,
} from "../../../../main/ts/editor/tiptap/tiptapRoundTrip";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";
import { TINYMCE_CORPUS } from "./tinymceCorpus";

describe("TipTap HTML round-trip (#5650 corpus, #5651 parity schema)", () => {
  for (const entry of TINYMCE_CORPUS) {
    it(`preserves TinyMCE content: ${entry.name}`, () => {
      expect(normalizeHtmlForRoundTrip(roundTripHtmlFragment(entry.tinymce))).toBe(
        normalizeHtmlForRoundTrip(entry.tinymce),
      );
    });
  }

  it("serializes an empty field as an empty paragraph", () => {
    expect(roundTripHtmlFragment("")).toBe("<p></p>");
  });

  it("normalizes known TinyMCE/TipTap representation pairs", () => {
    expect(normalizeHtmlForRoundTrip("<p><b>B</b> <i>I</i></p>")).toBe(
      normalizeHtmlForRoundTrip("<p><strong>B</strong> <em>I</em></p>"),
    );
    expect(normalizeHtmlForRoundTrip("<pre><code>x</code></pre>")).toBe(
      normalizeHtmlForRoundTrip("<pre>x</pre>"),
    );
    expect(normalizeHtmlForRoundTrip("<p><br></p>")).toBe(
      normalizeHtmlForRoundTrip("<p></p>"),
    );
  });
});

describe("TipTap table structure editing round-trip (#7393)", () => {
  function makeTableEditor(): Editor {
    const editor = new Editor({
      extensions: createTipTapEditorExtensions(),
      content: "<p>Intro</p>",
    });
    editor.commands.insertTable({ rows: 3, cols: 3, withHeaderRow: true });
    return editor;
  }

  function rowCount(html: string): number {
    return html.match(/<tr>/g)?.length ?? 0;
  }

  function headerCellCount(html: string): number {
    return html.match(/<th>/g)?.length ?? 0;
  }

  function bodyCellCount(html: string): number {
    return html.match(/<td>/g)?.length ?? 0;
  }

  /** Zero-drift: the edited document survives parse + serialize unchanged. */
  function expectZeroDrift(html: string): void {
    expect(normalizeHtmlForRoundTrip(roundTripHtmlFragment(html))).toBe(
      normalizeHtmlForRoundTrip(html),
    );
  }

  it("inserts a 3x3 header table with zero drift", () => {
    const editor = makeTableEditor();
    try {
      const html = editor.getHTML();
      expect(html).toContain("<table");
      expect(rowCount(html)).toBe(3);
      expect(headerCellCount(html)).toBe(3);
      expectZeroDrift(html);
    } finally {
      editor.destroy();
    }
  });

  it("preserves header and body cell content with zero drift", () => {
    const editor = makeTableEditor();
    try {
      // Selection lands in the first header cell after insert; Tab walks
      // the row like the toolbar keyboard contract (#6588).
      editor.commands.insertContent("Name");
      editor.commands.goToNextCell();
      editor.commands.insertContent("Value");
      editor.commands.goToNextCell();
      editor.commands.insertContent("Note");
      const html = editor.getHTML();
      expect(headerCellCount(html)).toBe(3);
      expect(html).toContain("Name");
      expect(html).toContain("Value");
      expect(html).toContain("Note");
      expectZeroDrift(html);
    } finally {
      editor.destroy();
    }
  });

  it("adds a row and a column with zero drift", () => {
    const editor = makeTableEditor();
    try {
      editor.commands.addRowAfter();
      expect(rowCount(editor.getHTML())).toBe(4);
      editor.commands.addColumnAfter();
      const html = editor.getHTML();
      expect(rowCount(html)).toBe(4);
      expect(headerCellCount(html)).toBe(4);
      expect(bodyCellCount(html)).toBe(12);
      expectZeroDrift(html);
    } finally {
      editor.destroy();
    }
  });

  /** Clamp the selection to the first cell (over-pressing is a no-op). */
  function selectFirstCell(editor: Editor): void {
    for (let i = 0; i < 20; i += 1) {
      editor.commands.goToPreviousCell();
    }
  }

  it("removes a row and a column with zero drift", () => {
    const editor = makeTableEditor();
    try {
      editor.commands.addRowAfter();
      editor.commands.addColumnAfter();
      expect(rowCount(editor.getHTML())).toBe(4);
      expect(headerCellCount(editor.getHTML())).toBe(4);
      // Delete the trailing column first: from the first cell, three
      // next-cell steps land in the added header cell.
      selectFirstCell(editor);
      for (let i = 0; i < 3; i += 1) {
        editor.commands.goToNextCell();
      }
      editor.commands.deleteColumn();
      expect(headerCellCount(editor.getHTML())).toBe(3);
      // Then the trailing body row: eleven next-cell steps from the first
      // cell land in the last body cell of the 4x3 grid.
      selectFirstCell(editor);
      for (let i = 0; i < 11; i += 1) {
        editor.commands.goToNextCell();
      }
      editor.commands.deleteRow();
      const html = editor.getHTML();
      expect(rowCount(html)).toBe(3);
      expect(headerCellCount(html)).toBe(3);
      expect(bodyCellCount(html)).toBe(6);
      expectZeroDrift(html);
    } finally {
      editor.destroy();
    }
  });

  it("keeps header row and content through a full edit cycle", () => {
    const editor = makeTableEditor();
    try {
      editor.commands.insertContent("Alpha");
      editor.commands.addRowAfter();
      editor.commands.addColumnAfter();
      expect(headerCellCount(editor.getHTML())).toBe(4);
      // Both deletes target the trailing column/row, so the header row
      // and the Alpha cell (first column) survive.
      selectFirstCell(editor);
      for (let i = 0; i < 3; i += 1) {
        editor.commands.goToNextCell();
      }
      editor.commands.deleteColumn();
      selectFirstCell(editor);
      for (let i = 0; i < 11; i += 1) {
        editor.commands.goToNextCell();
      }
      editor.commands.deleteRow();
      const html = editor.getHTML();
      expect(rowCount(html)).toBe(3);
      expect(headerCellCount(html)).toBe(3);
      expect(html).toContain("Alpha");
      expectZeroDrift(html);
      editor.commands.deleteTable();
      expect(editor.getHTML()).not.toContain("<table");
    } finally {
      editor.destroy();
    }
  });
});
